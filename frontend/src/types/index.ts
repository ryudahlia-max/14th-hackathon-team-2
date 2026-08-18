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
  [routineId: string]: number[]; // completed instance indices, e.g. [0, 2]
}

export interface MonthProgress {
  [dateStr: string]: DayProgress; // 'YYYY-MM-DD'
}

export interface ChatRoom {
  id: string;
  name: string;
  participantIds: string[]; // friend ids, excludes me
  isGroup: boolean;
  createdAt: string; // ISO timestamp
}

export interface Message {
  id: string;
  chatId: string;
  text?: string;
  imageUrl?: string;
  sentAt: string; // ISO timestamp
  fromMe: boolean;
}

export interface RoutineCompletionNotification {
  id: string;
  friendId: string;
  friendName: string;
  routineId: string;
  routineName: string;
  dateStr: string; // 'YYYY-MM-DD', the day the instance belongs to
  instanceIndex: number;
  completedAt: string; // ISO timestamp
}
