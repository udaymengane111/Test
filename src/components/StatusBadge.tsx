import type { AttendanceStatus } from '../types';
import { STATUS_LABELS, STATUS_SHORT } from '../types';

export function StatusBadge({ status }: { status: AttendanceStatus }) {
  return (
    <span className={`status-badge status-${status}`} title={STATUS_LABELS[status]}>
      {STATUS_SHORT[status]}
    </span>
  );
}

export function StatusButton({
  status,
  selected,
  onSelect,
}: {
  status: AttendanceStatus;
  selected: boolean;
  onSelect: (s: AttendanceStatus) => void;
}) {
  return (
    <button
      type="button"
      className={`status-btn status-${status}${selected ? ' selected' : ''}`}
      onClick={() => onSelect(status)}
      aria-pressed={selected}
    >
      <span className="status-btn-code">{STATUS_SHORT[status]}</span>
      <span className="status-btn-label">{STATUS_LABELS[status]}</span>
    </button>
  );
}
