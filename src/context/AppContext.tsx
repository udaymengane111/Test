import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type {
  AppData,
  AttendanceRecord,
  AttendanceStatus,
  ClassSection,
  Student,
} from '../types';
import { loadData, resetData, saveData } from '../storage/store';

interface AppContextValue {
  data: AppData;
  setSchoolName: (name: string) => void;
  addStudent: (student: Omit<Student, 'id'>) => void;
  updateStudent: (id: string, patch: Partial<Student>) => void;
  deactivateStudent: (id: string) => void;
  addClass: (cls: Omit<ClassSection, 'id'>) => void;
  markAttendance: (
    classId: string,
    date: string,
    marks: { studentId: string; status: AttendanceStatus; remark?: string }[],
  ) => void;
  getAttendanceForClassDate: (classId: string, date: string) => AttendanceRecord[];
  getStudentAttendance: (studentId: string) => AttendanceRecord[];
  resetToSeed: () => void;
}

const AppContext = createContext<AppContextValue | null>(null);

function uid(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [data, setData] = useState<AppData>(() => loadData());

  const persist = useCallback((next: AppData) => {
    setData(next);
    saveData(next);
  }, []);

  const setSchoolName = useCallback(
    (name: string) => {
      persist({ ...data, school: { ...data.school, name } });
    },
    [data, persist],
  );

  const addStudent = useCallback(
    (student: Omit<Student, 'id'>) => {
      const newStudent: Student = { ...student, id: uid('stu') };
      persist({ ...data, students: [...data.students, newStudent] });
    },
    [data, persist],
  );

  const updateStudent = useCallback(
    (id: string, patch: Partial<Student>) => {
      persist({
        ...data,
        students: data.students.map((s) => (s.id === id ? { ...s, ...patch } : s)),
      });
    },
    [data, persist],
  );

  const deactivateStudent = useCallback(
    (id: string) => {
      persist({
        ...data,
        students: data.students.map((s) => (s.id === id ? { ...s, active: false } : s)),
      });
    },
    [data, persist],
  );

  const addClass = useCallback(
    (cls: Omit<ClassSection, 'id'>) => {
      const id = `c${cls.standard}${cls.section.toLowerCase()}-${Date.now().toString(36)}`;
      persist({ ...data, classes: [...data.classes, { ...cls, id }] });
    },
    [data, persist],
  );

  const markAttendance = useCallback(
    (
      classId: string,
      date: string,
      marks: { studentId: string; status: AttendanceStatus; remark?: string }[],
    ) => {
      const others = data.attendance.filter(
        (a) => !(a.classId === classId && a.date === date),
      );
      const records: AttendanceRecord[] = marks.map((m) => ({
        id: uid('att'),
        studentId: m.studentId,
        classId,
        date,
        status: m.status,
        remark: m.remark,
      }));
      persist({ ...data, attendance: [...others, ...records] });
    },
    [data, persist],
  );

  const getAttendanceForClassDate = useCallback(
    (classId: string, date: string) =>
      data.attendance.filter((a) => a.classId === classId && a.date === date),
    [data.attendance],
  );

  const getStudentAttendance = useCallback(
    (studentId: string) => data.attendance.filter((a) => a.studentId === studentId),
    [data.attendance],
  );

  const resetToSeed = useCallback(() => {
    persist(resetData());
  }, [persist]);

  const value = useMemo(
    () => ({
      data,
      setSchoolName,
      addStudent,
      updateStudent,
      deactivateStudent,
      addClass,
      markAttendance,
      getAttendanceForClassDate,
      getStudentAttendance,
      resetToSeed,
    }),
    [
      data,
      setSchoolName,
      addStudent,
      updateStudent,
      deactivateStudent,
      addClass,
      markAttendance,
      getAttendanceForClassDate,
      getStudentAttendance,
      resetToSeed,
    ],
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp(): AppContextValue {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used within AppProvider');
  return ctx;
}
