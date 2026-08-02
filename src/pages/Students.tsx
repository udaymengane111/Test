import { useMemo, useState, type FormEvent } from 'react';
import { useApp } from '../context/AppContext';
import type { Gender, Student } from '../types';
import { formatClass, sortClasses, studentAttendancePct } from '../utils/attendance';

const emptyForm = {
  name: '',
  rollNo: 1,
  gender: 'boy' as Gender,
  classId: '',
  parentName: '',
  parentPhone: '',
  admissionNo: '',
};

export function Students() {
  const { data, addStudent, updateStudent, deactivateStudent, getStudentAttendance } = useApp();
  const classes = sortClasses(data.classes);
  const [filterClass, setFilterClass] = useState('all');
  const [query, setQuery] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Student | null>(null);
  const [form, setForm] = useState(emptyForm);

  const students = useMemo(() => {
    return data.students
      .filter((s) => s.active)
      .filter((s) => filterClass === 'all' || s.classId === filterClass)
      .filter((s) => {
        const q = query.trim().toLowerCase();
        if (!q) return true;
        return (
          s.name.toLowerCase().includes(q) ||
          s.admissionNo.toLowerCase().includes(q) ||
          s.parentName.toLowerCase().includes(q) ||
          String(s.rollNo).includes(q)
        );
      })
      .sort((a, b) => {
        const ca = classes.find((c) => c.id === a.classId);
        const cb = classes.find((c) => c.id === b.classId);
        const sa = (ca?.standard ?? 99) - (cb?.standard ?? 99);
        if (sa !== 0) return sa;
        const sec = (ca?.section ?? '').localeCompare(cb?.section ?? '');
        if (sec !== 0) return sec;
        return a.rollNo - b.rollNo;
      });
  }, [data.students, filterClass, query, classes]);

  function openAdd() {
    setEditing(null);
    setForm({
      ...emptyForm,
      classId: filterClass !== 'all' ? filterClass : classes[0]?.id || '',
      rollNo:
        (data.students.filter(
          (s) => s.classId === (filterClass !== 'all' ? filterClass : classes[0]?.id) && s.active,
        ).length || 0) + 1,
    });
    setShowForm(true);
  }

  function openEdit(s: Student) {
    setEditing(s);
    setForm({
      name: s.name,
      rollNo: s.rollNo,
      gender: s.gender,
      classId: s.classId,
      parentName: s.parentName,
      parentPhone: s.parentPhone,
      admissionNo: s.admissionNo,
    });
    setShowForm(true);
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form.name.trim() || !form.classId) return;
    if (editing) {
      updateStudent(editing.id, { ...form, name: form.name.trim() });
    } else {
      addStudent({
        ...form,
        name: form.name.trim(),
        active: true,
      });
    }
    setShowForm(false);
    setEditing(null);
  }

  function classNameOf(id: string) {
    const c = classes.find((x) => x.id === id);
    return c ? formatClass(c) : '—';
  }

  return (
    <div className="page">
      <header className="page-header reveal">
        <div>
          <h1>Students</h1>
          <p className="lede">{students.length} on the current list</p>
        </div>
        <button type="button" className="btn primary" onClick={openAdd}>
          Add student
        </button>
      </header>

      <div className="toolbar reveal delay-1">
        <label className="field grow">
          <span>Search</span>
          <input
            type="search"
            placeholder="Name, roll no, admission no…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </label>
        <label className="field">
          <span>Class</span>
          <select value={filterClass} onChange={(e) => setFilterClass(e.target.value)}>
            <option value="all">All classes</option>
            {classes.map((c) => (
              <option key={c.id} value={c.id}>
                {formatClass(c)}
              </option>
            ))}
          </select>
        </label>
      </div>

      {showForm && (
        <form className="form-panel reveal" onSubmit={onSubmit}>
          <h2>{editing ? 'Edit student' : 'New student'}</h2>
          <div className="form-grid">
            <label className="field">
              <span>Full name</span>
              <input
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </label>
            <label className="field">
              <span>Roll no.</span>
              <input
                type="number"
                min={1}
                required
                value={form.rollNo}
                onChange={(e) => setForm({ ...form, rollNo: Number(e.target.value) })}
              />
            </label>
            <label className="field">
              <span>Class</span>
              <select
                required
                value={form.classId}
                onChange={(e) => setForm({ ...form, classId: e.target.value })}
              >
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {formatClass(c)}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Gender</span>
              <select
                value={form.gender}
                onChange={(e) => setForm({ ...form, gender: e.target.value as Gender })}
              >
                <option value="boy">Boy</option>
                <option value="girl">Girl</option>
              </select>
            </label>
            <label className="field">
              <span>Admission no.</span>
              <input
                required
                value={form.admissionNo}
                onChange={(e) => setForm({ ...form, admissionNo: e.target.value })}
              />
            </label>
            <label className="field">
              <span>Parent / guardian</span>
              <input
                required
                value={form.parentName}
                onChange={(e) => setForm({ ...form, parentName: e.target.value })}
              />
            </label>
            <label className="field">
              <span>Parent mobile</span>
              <input
                required
                pattern="[0-9]{10}"
                title="10-digit mobile number"
                value={form.parentPhone}
                onChange={(e) => setForm({ ...form, parentPhone: e.target.value })}
              />
            </label>
          </div>
          <div className="form-actions">
            <button type="button" className="btn ghost" onClick={() => setShowForm(false)}>
              Cancel
            </button>
            <button type="submit" className="btn primary">
              {editing ? 'Save changes' : 'Add to roll'}
            </button>
          </div>
        </form>
      )}

      <div className="table-wrap reveal delay-2">
        <table className="data-table">
          <thead>
            <tr>
              <th>Roll</th>
              <th>Name</th>
              <th>Class</th>
              <th>Parent</th>
              <th>Mobile</th>
              <th>Attendance %</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {students.map((s) => {
              const pct = studentAttendancePct(getStudentAttendance(s.id));
              return (
                <tr key={s.id}>
                  <td>{String(s.rollNo).padStart(2, '0')}</td>
                  <td>
                    <strong>{s.name}</strong>
                    <div className="cell-sub">{s.admissionNo}</div>
                  </td>
                  <td>{classNameOf(s.classId)}</td>
                  <td>{s.parentName}</td>
                  <td>{s.parentPhone}</td>
                  <td>
                    <span className={`pct${pct < 75 ? ' low' : ''}`}>{pct}%</span>
                  </td>
                  <td className="row-actions">
                    <button type="button" className="linkish" onClick={() => openEdit(s)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className="linkish danger"
                      onClick={() => {
                        if (confirm(`Remove ${s.name} from the active roll?`)) {
                          deactivateStudent(s.id);
                        }
                      }}
                    >
                      Remove
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {students.length === 0 && <p className="empty">No students match your filters.</p>}
      </div>
    </div>
  );
}
