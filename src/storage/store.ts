import type { AppData } from '../types';
import { createSeedData } from '../data/seed';

const STORAGE_KEY = 'pathshala-attendance-v1';

export function loadData(): AppData {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as AppData;
      if (parsed?.school && Array.isArray(parsed.classes) && Array.isArray(parsed.students)) {
        return parsed;
      }
    }
  } catch {
    // fall through to seed
  }
  const seed = createSeedData();
  saveData(seed);
  return seed;
}

export function saveData(data: AppData): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}

export function resetData(): AppData {
  const seed = createSeedData();
  saveData(seed);
  return seed;
}
