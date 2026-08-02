import { useMemo, useState } from 'react';
import { format, parseISO } from 'date-fns';
import { StatusBadge } from '../components/StatusBadge';
import { useApp } from '../context/AppContext';
import {
  activeStudentsInClass,
  attendanceStats,
  formatClass,
  formatDisplayDate,
  monthWorkingDays,
  sortClasses,
  studentAttendancePct,
} from '../utils/attendance';

export function Reports() {
  const { data, getAttendanceForClassDate, getStudentAttendance } = useApp();
  const classes = sortClasses(data.classes);
  const now = new Date();
  const [classId, setClassId] = useState(classes[0]?.id || '');
  const [month, setMonth] = useState(format(now, 'yyyy-MM'));
  const [tab, setTab] = useState<'daily' | 'monthly' | 'low'>('daily');
  const [dailyDate, setDailyDate] = useState(format(now, 'yyyy-MM-dd'));

  const [yearStr, monthStr] = month.split('-');
  const year = Number(yearStr);
  const monthIndex = Number(monthStr) - 1;

  const students = useMemo(
    () => activeStudentsInClass(data.students, classId),
    [data.students, classId],
  );

  const dailyRecords = getAttendanceForClassDate(classId, dailyDate);
  const dailyStats = attendanceStats(dailyRecords);

  const workingDays = useMemo(() => monthWorkingDays(year, monthIndex), [year, monthIndex]);

  const monthlyRows = useMemo(() => {
    return students.map((s) => {
      const all = getStudentAttendance(s.id).filter((r) => {
        const d = parseISO(r.date);
        return d.getFullYear() === year && d.getMonth() === monthIndex && r.classId === classId;
      });
      const presentish = all.filter(
        (r) => r.status === 'present' || r.status === 'late' || r.status === 'halfday',
      ).length;
      const absent = all.filter((r) => r.status === 'absent').length;
      const leave = all.filter((r) => r.status === 'leave').length;
      const markedDays = all.length;
      const pct = markedDays === 0 ? 0 : Math.round((presentish / markedDays) * 100);
      return { student: s, presentish, absent, leave, markedDays, pct };
    });
  }, [students, getStudentAttendance, year, monthIndex, classId]);

  const lowAttendance = useMemo(() => {
    return data.students
      .filter((s) => s.active)
      .map((s) => {
        const recs = getStudentAttendance(s.id);
        return { student: s, pct: studentAttendancePct(recs), days: recs.length };
      })
      .filter((r) => r.days > 0 && r.pct < 75)
      .sort((a, b) => a.pct - b.pct);
  }, [data.students, getStudentAttendance]);

  const selectedClass = classes.find((c) => c.id === classId);

  const monthMarkedDays = workingDays.filter(
    (d) => getAttendanceForClassDate(classId, d).length > 0,
  ).length;

  return (
    <div className="page">
      <header className="page-header reveal">
        <div>
          <h1>Reports</h1>
          <p className="lede">Daily registers, monthly summaries, and students below 75%</p>
        </div>
      </header>

      <div className="tabs reveal delay-1" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'daily'}
          className={tab === 'daily' ? 'active' : ''}
          onClick={() => setTab('daily')}
        >
          Daily register
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'monthly'}
          className={tab === 'monthly' ? 'active' : ''}
          onClick={() => setTab('monthly')}
        >
          Monthly summary
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'low'}
          className={tab === 'low' ? 'active' : ''}
          onClick={() => setTab('low')}
        >
          Below 75%
        </button>
      </div>

      {tab !== 'low' && (
        <div className="toolbar reveal delay-1">
          <label className="field">
            <span>Class</span>
            <select value={classId} onChange={(e) => setClassId(e.target.value)}>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>
                  {formatClass(c)}
                </option>
              ))}
            </select>
          </label>
          {tab === 'daily' ? (
            <label className="field">
              <span>Date</span>
              <input
                type="date"
                value={dailyDate}
                onChange={(e) => setDailyDate(e.target.value)}
              />
            </label>
          ) : (
            <label className="field">
              <span>Month</span>
              <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} />
            </label>
          )}
        </div>
      )}

      {tab === 'daily' && (
        <section className="section reveal delay-2">
          <div className="section-head">
            <h2>
              {selectedClass ? formatClass(selectedClass) : 'Class'} — {formatDisplayDate(dailyDate)}
            </h2>
            <p>
              {dailyRecords.length === 0
                ? 'No attendance marked for this date.'
                : `${dailyStats.present} present · ${dailyStats.absent} absent · ${dailyStats.late} late · ${dailyStats.pct}% present`}
            </p>
          </div>
          {dailyRecords.length > 0 && (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Roll</th>
                    <th>Name</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {students.map((s) => {
                    const rec = dailyRecords.find((r) => r.studentId === s.id);
                    if (!rec) return null;
                    return (
                      <tr key={s.id}>
                        <td>{String(s.rollNo).padStart(2, '0')}</td>
                        <td>{s.name}</td>
                        <td>
                          <StatusBadge status={rec.status} />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {tab === 'monthly' && (
        <section className="section reveal delay-2">
          <div className="section-head">
            <h2>
              {selectedClass ? formatClass(selectedClass) : 'Class'} —{' '}
              {format(new Date(year, monthIndex, 1), 'MMMM yyyy')}
            </h2>
            <p>
              {monthMarkedDays} of {workingDays.length} working days marked (Sundays excluded)
            </p>
          </div>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Roll</th>
                  <th>Name</th>
                  <th>Present*</th>
                  <th>Absent</th>
                  <th>Leave</th>
                  <th>Days marked</th>
                  <th>%</th>
                </tr>
              </thead>
              <tbody>
                {monthlyRows.map((row) => (
                  <tr key={row.student.id}>
                    <td>{String(row.student.rollNo).padStart(2, '0')}</td>
                    <td>{row.student.name}</td>
                    <td>{row.presentish}</td>
                    <td>{row.absent}</td>
                    <td>{row.leave}</td>
                    <td>{row.markedDays}</td>
                    <td>
                      <span className={`pct${row.pct < 75 ? ' low' : ''}`}>{row.pct}%</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="footnote">* Present includes Late and Half Day.</p>
        </section>
      )}

      {tab === 'low' && (
        <section className="section reveal delay-2">
          <div className="section-head">
            <h2>Students below 75% attendance</h2>
            <p>Useful for RTE / school follow-up with parents</p>
          </div>
          {lowAttendance.length === 0 ? (
            <p className="empty">
              No students below 75% yet. Mark a few days of attendance to populate this list.
            </p>
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Class</th>
                    <th>Parent mobile</th>
                    <th>Days marked</th>
                    <th>%</th>
                  </tr>
                </thead>
                <tbody>
                  {lowAttendance.map(({ student, pct, days }) => {
                    const cls = classes.find((c) => c.id === student.classId);
                    return (
                      <tr key={student.id}>
                        <td>
                          <strong>{student.name}</strong>
                          <div className="cell-sub">Roll {student.rollNo}</div>
                        </td>
                        <td>{cls ? formatClass(cls) : '—'}</td>
                        <td>{student.parentPhone}</td>
                        <td>{days}</td>
                        <td>
                          <span className="pct low">{pct}%</span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}
    </div>
  );
}
