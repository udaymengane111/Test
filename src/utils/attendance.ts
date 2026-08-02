import {
  eachDayOfInterval,
  endOfMonth,
  format,
  isSunday,
  parseISO,
  startOfMonth,
} from 'date-fns';
import type { AttendanceRecord, ClassSection, Student } from '../types';
import { classLabel } from '../data/seed';

export function todayISO(): string {
  return format(new Date(), 'yyyy-MM-dd');
}

export function formatDisplayDate(iso: string): string {
  try {
    return format(parseISO(iso), 'EEE, d MMM yyyy');
  } catch {
    return iso;
  }
}

export function formatClass(cls: ClassSection): string {
  return classLabel(cls.standard, cls.section);
}

export function sortClasses(classes: ClassSection[]): ClassSection[] {
  return [...classes].sort((a, b) => a.standard - b.standard || a.section.localeCompare(b.section));
}

export function activeStudentsInClass(students: Student[], classId: string): Student[] {
  return students
    .filter((s) => s.classId === classId && s.active)
    .sort((a, b) => a.rollNo - b.rollNo);
}

export function attendanceStats(records: AttendanceRecord[]) {
  const total = records.length;
  const present = records.filter((r) => r.status === 'present').length;
  const absent = records.filter((r) => r.status === 'absent').length;
  const late = records.filter((r) => r.status === 'late').length;
  const halfday = records.filter((r) => r.status === 'halfday').length;
  const leave = records.filter((r) => r.status === 'leave').length;
  const attended = present + late;
  const pct = total === 0 ? 0 : Math.round((attended / total) * 100);
  return { total, present, absent, late, halfday, leave, pct };
}

/** Present + Late + Half Day count toward attendance % (common school practice) */
export function studentAttendancePct(records: AttendanceRecord[]): number {
  if (records.length === 0) return 0;
  const attended = records.filter(
    (r) => r.status === 'present' || r.status === 'late' || r.status === 'halfday',
  ).length;
  return Math.round((attended / records.length) * 100);
}

export function monthWorkingDays(year: number, monthIndex: number): string[] {
  const start = startOfMonth(new Date(year, monthIndex, 1));
  const end = endOfMonth(start);
  return eachDayOfInterval({ start, end })
    .filter((d) => !isSunday(d))
    .map((d) => format(d, 'yyyy-MM-dd'));
}

export function isSchoolHoliday(iso: string): boolean {
  const d = parseISO(iso);
  return isSunday(d);
}

export function ordinalStandard(n: number): string {
  const map: Record<number, string> = {
    1: 'I',
    2: 'II',
    3: 'III',
    4: 'IV',
    5: 'V',
  };
  return map[n] ?? String(n);
}

/** Prefer last working day when today is Sunday */
export function defaultAttendanceDate(): string {
  const d = new Date();
  if (isSunday(d)) {
    d.setDate(d.getDate() - 1);
  }
  return format(d, 'yyyy-MM-dd');
}
