import type { Friend, Routine, MonthProgress } from '../types';

export const FRIENDS: Friend[] = [
  { id: '1', name: '이가영' },
  { id: '2', name: '연진' },
  { id: '3', name: '쩡' },
  { id: '4', name: '가나다라' },
];

export const ROUTINES: Routine[] = [
  { id: 'water', name: '물 마시기', color: '#60A5FA', totalCount: 3 },
  { id: 'sunscreen', name: '선크림', color: '#FBBF24', totalCount: 2 },
  { id: 'exercise', name: '운동', color: '#34D399', totalCount: 1 },
];

export const MONTH_PROGRESS: MonthProgress = {};
