export type AttendanceStatus = 'present' | 'absent' | 'late' | 'halfday' | 'leave';

export type Gender = 'boy' | 'girl';

export interface School {
  name: string;
  academicYear: string;
  medium: string;
  address: string;
}

export interface ClassSection {
  id: string;
  standard: number; // 1–5 for primary
  section: string;
  classTeacher: string;
}

export interface Student {
  id: string;
  rollNo: number;
  name: string;
  gender: Gender;
  classId: string;
  parentName: string;
  parentPhone: string;
  admissionNo: string;
  active: boolean;
}

export interface AttendanceRecord {
  id: string;
  studentId: string;
  classId: string;
  date: string; // YYYY-MM-DD
  status: AttendanceStatus;
  remark?: string;
}

export interface AppData {
  school: School;
  classes: ClassSection[];
  students: Student[];
  attendance: AttendanceRecord[];
}

export const STATUS_LABELS: Record<AttendanceStatus, string> = {
  present: 'Present',
  absent: 'Absent',
  late: 'Late',
  halfday: 'Half Day',
  leave: 'On Leave',
};

export const STATUS_SHORT: Record<AttendanceStatus, string> = {
  present: 'P',
  absent: 'A',
  late: 'L',
  halfday: 'HD',
  leave: 'OL',
};
