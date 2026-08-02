import { Link } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import {
  activeStudentsInClass,
  attendanceStats,
  defaultAttendanceDate,
  formatClass,
  formatDisplayDate,
  sortClasses,
} from '../utils/attendance';

export function Dashboard() {
  const { data, getAttendanceForClassDate } = useApp();
  const date = defaultAttendanceDate();
  const classes = sortClasses(data.classes);
  const activeStudents = data.students.filter((s) => s.active);

  const classSummaries = classes.map((cls) => {
    const students = activeStudentsInClass(data.students, cls.id);
    const records = getAttendanceForClassDate(cls.id, date);
    const marked = records.length > 0;
    const stats = attendanceStats(records);
    return { cls, students, marked, stats };
  });

  const markedToday = classSummaries.filter((c) => c.marked).length;
  const todayRecords = data.attendance.filter((a) => a.date === date);
  const todayStats = attendanceStats(todayRecords);

  return (
    <div className="page">
      <header className="page-header reveal">
        <div>
          <h1>Today&apos;s attendance</h1>
          <p className="lede">{formatDisplayDate(date)} · {data.school.academicYear}</p>
        </div>
        <Link to="/attendance" className="btn primary">
          Mark attendance
        </Link>
      </header>

      <section className="stat-row reveal delay-1" aria-label="Overview">
        <div className="stat">
          <span className="stat-value">{activeStudents.length}</span>
          <span className="stat-label">Students</span>
        </div>
        <div className="stat">
          <span className="stat-value">{classes.length}</span>
          <span className="stat-label">Classes</span>
        </div>
        <div className="stat">
          <span className="stat-value">
            {markedToday}/{classes.length}
          </span>
          <span className="stat-label">Classes marked</span>
        </div>
        <div className="stat accent">
          <span className="stat-value">{todayStats.pct}%</span>
          <span className="stat-label">Present today</span>
        </div>
      </section>

      <section className="section reveal delay-2">
        <div className="section-head">
          <h2>Class-wise status</h2>
          <p>Tap a class to take or review today&apos;s roll call.</p>
        </div>
        <div className="class-grid">
          {classSummaries.map(({ cls, students, marked, stats }) => (
            <Link
              key={cls.id}
              to={`/attendance?class=${cls.id}&date=${date}`}
              className={`class-tile${marked ? ' marked' : ''}`}
            >
              <div className="class-tile-top">
                <span className="class-name">{formatClass(cls)}</span>
                <span className={`mark-pill${marked ? ' ok' : ''}`}>
                  {marked ? 'Marked' : 'Pending'}
                </span>
              </div>
              <p className="class-teacher">{cls.classTeacher}</p>
              <div className="class-tile-foot">
                <span>{students.length} students</span>
                {marked ? (
                  <span>
                    {stats.present}P · {stats.absent}A · {stats.late}L
                  </span>
                ) : (
                  <span className="hint">Not marked yet</span>
                )}
              </div>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
