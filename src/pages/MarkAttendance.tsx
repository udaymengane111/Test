import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { StatusButton } from '../components/StatusBadge';
import { useApp } from '../context/AppContext';
import type { AttendanceStatus } from '../types';
import {
  activeStudentsInClass,
  defaultAttendanceDate,
  formatClass,
  formatDisplayDate,
  isSchoolHoliday,
  sortClasses,
} from '../utils/attendance';

const ALL_STATUSES: AttendanceStatus[] = ['present', 'absent', 'late', 'halfday', 'leave'];

export function MarkAttendance() {
  const { data, markAttendance, getAttendanceForClassDate } = useApp();
  const [params, setParams] = useSearchParams();
  const classes = sortClasses(data.classes);

  const classId = params.get('class') || classes[0]?.id || '';
  const date = params.get('date') || defaultAttendanceDate();

  const students = useMemo(
    () => activeStudentsInClass(data.students, classId),
    [data.students, classId],
  );

  const existing = getAttendanceForClassDate(classId, date);
  const existingKey = existing
    .map((e) => `${e.studentId}:${e.status}`)
    .sort()
    .join('|');

  const [marks, setMarks] = useState<Record<string, AttendanceStatus>>({});
  const [savedFlash, setSavedFlash] = useState(false);

  useEffect(() => {
    const current = getAttendanceForClassDate(classId, date);
    const next: Record<string, AttendanceStatus> = {};
    for (const s of students) {
      const found = current.find((e) => e.studentId === s.id);
      next[s.id] = found?.status ?? 'present';
    }
    setMarks(next);
  }, [classId, date, students, existingKey, getAttendanceForClassDate]);

  useEffect(() => {
    setSavedFlash(false);
  }, [classId, date]);

  const selectedClass = classes.find((c) => c.id === classId);
  const holiday = isSchoolHoliday(date);
  const alreadyMarked = existing.length > 0;

  function setClass(id: string) {
    const next = new URLSearchParams(params);
    next.set('class', id);
    next.set('date', date);
    setParams(next);
  }

  function setDate(value: string) {
    const next = new URLSearchParams(params);
    next.set('class', classId);
    next.set('date', value);
    setParams(next);
  }

  function setAll(status: AttendanceStatus) {
    const next: Record<string, AttendanceStatus> = {};
    for (const s of students) next[s.id] = status;
    setMarks(next);
  }

  function setOne(studentId: string, status: AttendanceStatus) {
    setMarks((m) => ({ ...m, [studentId]: status }));
  }

  function save() {
    const payload = students.map((s) => ({
      studentId: s.id,
      status: marks[s.id] ?? 'present',
    }));
    markAttendance(classId, date, payload);
    setSavedFlash(true);
    window.setTimeout(() => setSavedFlash(false), 2200);
  }

  const counts = ALL_STATUSES.reduce(
    (acc, st) => {
      acc[st] = Object.values(marks).filter((v) => v === st).length;
      return acc;
    },
    {} as Record<AttendanceStatus, number>,
  );

  return (
    <div className="page">
      <header className="page-header reveal">
        <div>
          <h1>Mark attendance</h1>
          <p className="lede">
            {selectedClass ? formatClass(selectedClass) : 'Select a class'} ·{' '}
            {formatDisplayDate(date)}
          </p>
        </div>
        <button type="button" className="btn primary" onClick={save} disabled={!students.length || holiday}>
          {alreadyMarked ? 'Update roll' : 'Save roll'}
        </button>
      </header>

      <div className="toolbar reveal delay-1">
        <label className="field">
          <span>Class</span>
          <select value={classId} onChange={(e) => setClass(e.target.value)}>
            {classes.map((c) => (
              <option key={c.id} value={c.id}>
                {formatClass(c)} — {c.classTeacher}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Date</span>
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
        <div className="toolbar-actions">
          <button type="button" className="btn ghost" onClick={() => setAll('present')}>
            All present
          </button>
          <button type="button" className="btn ghost" onClick={() => setAll('absent')}>
            All absent
          </button>
        </div>
      </div>

      {holiday && (
        <div className="banner warn reveal" role="status">
          Sunday — schools are typically closed. You can still record attendance if needed.
        </div>
      )}

      {savedFlash && (
        <div className="banner success reveal" role="status">
          Attendance saved for {selectedClass ? formatClass(selectedClass) : 'class'}.
        </div>
      )}

      <div className="count-strip reveal delay-2" aria-live="polite">
        <span>P {counts.present}</span>
        <span>A {counts.absent}</span>
        <span>L {counts.late}</span>
        <span>HD {counts.halfday}</span>
        <span>OL {counts.leave}</span>
        <span className="muted">{students.length} on roll</span>
      </div>

      <div className="roll-list reveal delay-2">
        {students.length === 0 ? (
          <p className="empty">No active students in this class.</p>
        ) : (
          students.map((s) => (
            <div key={s.id} className="roll-row">
              <div className="roll-identity">
                <span className="roll-no">{String(s.rollNo).padStart(2, '0')}</span>
                <div>
                  <p className="roll-name">{s.name}</p>
                  <p className="roll-meta">
                    {s.gender === 'boy' ? 'Boy' : 'Girl'} · {s.admissionNo}
                  </p>
                </div>
              </div>
              <div className="status-group" role="group" aria-label={`Status for ${s.name}`}>
                {ALL_STATUSES.map((st) => (
                  <StatusButton
                    key={st}
                    status={st}
                    selected={(marks[s.id] ?? 'present') === st}
                    onSelect={(status) => setOne(s.id, status)}
                  />
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
