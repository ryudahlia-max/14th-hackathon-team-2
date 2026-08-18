import type { RoutineCompletionNotification } from '../types';

const NOTIFICATIONS_KEY = 'routine-app:notifications';

function load(): RoutineCompletionNotification[] {
  try {
    const raw = localStorage.getItem(NOTIFICATIONS_KEY);
    return raw ? (JSON.parse(raw) as RoutineCompletionNotification[]) : [];
  } catch {
    return [];
  }
}

function save(list: RoutineCompletionNotification[]) {
  localStorage.setItem(NOTIFICATIONS_KEY, JSON.stringify(list));
}

export function getNotifications(): RoutineCompletionNotification[] {
  return load();
}

export function addRoutineCompletionNotification(
  friendId: string,
  friendName: string,
  routineName: string
): RoutineCompletionNotification {
  const list = load();
  const notification: RoutineCompletionNotification = {
    id: `n-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    friendId,
    friendName,
    routineName,
    completedAt: new Date().toISOString(),
  };
  save([...list, notification]);
  return notification;
}
