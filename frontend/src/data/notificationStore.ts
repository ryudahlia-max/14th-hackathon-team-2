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

function isSameInstance(
  n: RoutineCompletionNotification,
  friendId: string,
  routineId: string,
  dateStr: string,
  instanceIndex: number
) {
  return (
    n.friendId === friendId &&
    n.routineId === routineId &&
    n.dateStr === dateStr &&
    n.instanceIndex === instanceIndex
  );
}

export function getNotifications(): RoutineCompletionNotification[] {
  return load();
}

interface RoutineCompletionParams {
  friendId: string;
  friendName: string;
  routineId: string;
  routineName: string;
  dateStr: string;
  instanceIndex: number;
}

// 같은 (친구, 루틴, 날짜, 인스턴스)에 대한 기존 알림을 대체한다 —
// 체크/해제를 반복해도 알림이 중복으로 쌓이지 않도록.
export function setRoutineCompletionNotification(
  params: RoutineCompletionParams
): RoutineCompletionNotification {
  const list = load().filter(
    n => !isSameInstance(n, params.friendId, params.routineId, params.dateStr, params.instanceIndex)
  );
  const notification: RoutineCompletionNotification = {
    id: `n-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    ...params,
    completedAt: new Date().toISOString(),
  };
  save([...list, notification]);
  return notification;
}

export function removeRoutineCompletionNotification(
  friendId: string,
  routineId: string,
  dateStr: string,
  instanceIndex: number
) {
  const list = load().filter(n => !isSameInstance(n, friendId, routineId, dateStr, instanceIndex));
  save(list);
}
