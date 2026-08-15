export interface Friend {
  id: string;
  name: string;
  profileImage?: string;
}

export interface Routine {
  id: string;
  name: string;
  color: string;
  totalCount: number;
}

export interface DayProgress {
  [routineId: string]: number;
}

export interface MonthProgress {
  [dateStr: string]: DayProgress; // 'YYYY-MM-DD'
}
