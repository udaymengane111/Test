import { useState, type FormEvent } from 'react';
import { useApp } from '../context/AppContext';
import { activeStudentsInClass, formatClass, ordinalStandard, sortClasses } from '../utils/attendance';

export function Classes() {
  const { data, addClass, setSchoolName, resetToSeed } = useApp();
  const classes = sortClasses(data.classes);
  const [showForm, setShowForm] = useState(false);
  const [schoolName, setLocalSchool] = useState(data.school.name);
  const [form, setForm] = useState({
    standard: 1,
    section: 'A',
    classTeacher: '',
  });

  function onAdd(e: FormEvent) {
    e.preventDefault();
    const exists = data.classes.some(
      (c) =>
        c.standard === form.standard &&
        c.section.toUpperCase() === form.section.trim().toUpperCase(),
    );
    if (exists) {
      alert('That class & section already exists.');
      return;
    }
    addClass({
      standard: form.standard,
      section: form.section.trim().toUpperCase(),
      classTeacher: form.classTeacher.trim(),
    });
    setShowForm(false);
    setForm({ standard: 1, section: 'A', classTeacher: '' });
  }

  function saveSchool(e: FormEvent) {
    e.preventDefault();
    if (schoolName.trim()) setSchoolName(schoolName.trim());
  }

  return (
    <div className="page">
      <header className="page-header reveal">
        <div>
          <h1>Classes & school</h1>
          <p className="lede">Primary standards I–V with sections and class teachers</p>
        </div>
        <button type="button" className="btn primary" onClick={() => setShowForm(true)}>
          Add class
        </button>
      </header>

      <form className="form-panel compact reveal delay-1" onSubmit={saveSchool}>
        <h2>School profile</h2>
        <div className="form-grid">
          <label className="field grow">
            <span>School name</span>
            <input value={schoolName} onChange={(e) => setLocalSchool(e.target.value)} />
          </label>
          <div className="field readonly">
            <span>Academic year</span>
            <strong>{data.school.academicYear}</strong>
          </div>
          <div className="field readonly">
            <span>Medium</span>
            <strong>{data.school.medium}</strong>
          </div>
        </div>
        <p className="address">{data.school.address}</p>
        <div className="form-actions">
          <button type="submit" className="btn primary">
            Save school name
          </button>
          <button
            type="button"
            className="btn ghost"
            onClick={() => {
              if (confirm('Reset demo data? All attendance marks will be cleared.')) {
                resetToSeed();
                setLocalSchool('Shri Saraswati Vidyalaya');
              }
            }}
          >
            Reset demo data
          </button>
        </div>
      </form>

      {showForm && (
        <form className="form-panel reveal" onSubmit={onAdd}>
          <h2>New class</h2>
          <div className="form-grid">
            <label className="field">
              <span>Standard</span>
              <select
                value={form.standard}
                onChange={(e) => setForm({ ...form, standard: Number(e.target.value) })}
              >
                {[1, 2, 3, 4, 5].map((n) => (
                  <option key={n} value={n}>
                    Class {ordinalStandard(n)} ({n})
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Section</span>
              <input
                required
                maxLength={2}
                value={form.section}
                onChange={(e) => setForm({ ...form, section: e.target.value })}
              />
            </label>
            <label className="field grow">
              <span>Class teacher</span>
              <input
                required
                value={form.classTeacher}
                onChange={(e) => setForm({ ...form, classTeacher: e.target.value })}
              />
            </label>
          </div>
          <div className="form-actions">
            <button type="button" className="btn ghost" onClick={() => setShowForm(false)}>
              Cancel
            </button>
            <button type="submit" className="btn primary">
              Create class
            </button>
          </div>
        </form>
      )}

      <div className="class-admin-grid reveal delay-2">
        {classes.map((cls) => {
          const count = activeStudentsInClass(data.students, cls.id).length;
          return (
            <article key={cls.id} className="class-admin-card">
              <h3>{formatClass(cls)}</h3>
              <p className="roman">Std. {ordinalStandard(cls.standard)}</p>
              <dl>
                <div>
                  <dt>Class teacher</dt>
                  <dd>{cls.classTeacher}</dd>
                </div>
                <div>
                  <dt>Strength</dt>
                  <dd>{count} students</dd>
                </div>
              </dl>
            </article>
          );
        })}
      </div>
    </div>
  );
}
