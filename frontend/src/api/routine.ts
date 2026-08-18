import { api } from '../services/apiClient';

export interface RoutineResponse {
  id: string;
  title: string;
  category: string;
  daysOfWeek: string[];
  reminderTime: string;
  timezone: string;
  startDate: string;
  endDate: string | null;
  active: boolean;
}

export interface CalendarDayResponse {
  date: string;
  scheduledCount: number;
  completedCount: number;
  completionRate: number;
  completedRoutineIds: string[];
}

export interface MissedRoutineResponse {
  routineId: string;
  title: string;
  missedDate: string;
}

const ALL_DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

function payload(title: string, category: string, existing?: RoutineResponse) {
  return {
    title,
    category,
    daysOfWeek: existing?.daysOfWeek ?? ALL_DAYS,
    reminderTime: existing?.reminderTime ?? '09:00:00',
    timezone: existing?.timezone ?? (Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Seoul'),
    startDate: existing?.startDate ?? new Date().toISOString().slice(0, 10),
    endDate: existing?.endDate ?? null,
  };
}

export const getRoutines = () => api.get<RoutineResponse[]>('/api/v1/routines');
export const getFriendRoutines = (friendId: string) =>
  api.get<RoutineResponse[]>(`/api/v1/routines/friends/${friendId}`);
export const createRoutine = (title: string, category: string) =>
  api.post<RoutineResponse>('/api/v1/routines', payload(title, category));
export const updateRoutine = (routine: RoutineResponse, title: string, category: string) =>
  api.patch<RoutineResponse>(`/api/v1/routines/${routine.id}`, {
    routine: payload(title, category, routine),
    active: routine.active,
  });
export const deleteRoutine = (id: string) => api.delete<void>(`/api/v1/routines/${id}`);
export const completeRoutine = (id: string, date: string) =>
  api.post(`/api/v1/routines/${id}/completions`, { completionDate: date });
export const uncompleteRoutine = (id: string, date: string) =>
  api.delete<void>(`/api/v1/routines/${id}/completions?date=${encodeURIComponent(date)}`);
export const getCalendar = (month: string) =>
  api.get<CalendarDayResponse[]>(`/api/v1/routines/calendar?month=${encodeURIComponent(month)}`);
export const getFriendCalendar = (friendId: string, month: string) =>
  api.get<CalendarDayResponse[]>(
    `/api/v1/routines/friends/${friendId}/calendar?month=${encodeURIComponent(month)}`,
  );
export const getMissedFriendRoutines = (friendId: string) =>
  api.get<MissedRoutineResponse[]>(`/api/v1/routines/friends/${friendId}/missed`);
