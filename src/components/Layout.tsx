import { NavLink, Outlet } from 'react-router-dom';
import { useApp } from '../context/AppContext';

const nav = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/attendance', label: 'Mark Attendance' },
  { to: '/students', label: 'Students' },
  { to: '/classes', label: 'Classes' },
  { to: '/reports', label: 'Reports' },
];

export function Layout() {
  const { data } = useApp();

  return (
    <div className="app-shell">
      <div className="atmosphere" aria-hidden="true" />
      <header className="topbar">
        <div className="brand-block">
          <div className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 40 40" width="40" height="40">
              <rect width="40" height="40" rx="10" fill="currentColor" className="mark-bg" />
              <path
                d="M8 26V14l12-6 12 6v12l-12 6-12-6z"
                stroke="#FFB84D"
                strokeWidth="2"
                fill="none"
              />
              <path d="M20 8v24M8 14l12 6 12-6" stroke="#FFB84D" strokeWidth="1.6" fill="none" />
            </svg>
          </div>
          <div className="brand-text">
            <p className="brand-name">Pathshala</p>
            <p className="brand-school">{data.school.name}</p>
          </div>
        </div>
        <div className="topbar-meta">
          <span className="chip">{data.school.academicYear}</span>
          <span className="chip muted">{data.school.medium}</span>
        </div>
      </header>

      <div className="body-grid">
        <nav className="sidenav" aria-label="Main">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <main className="main-panel">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
