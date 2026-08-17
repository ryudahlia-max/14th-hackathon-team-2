import type { Friend, Message } from '../types';

export const FRIENDS: Friend[] = [
  { id: '1', name: '이가영' },
  { id: '2', name: '연진' },
  { id: '3', name: '쩡' },
  { id: '4', name: '가나다라' },
];

export const MESSAGES: Message[] = [
  { id: 'm-1', friendId: '1', text: '오늘 루틴 다 했어?', sentAt: '2026-08-17T09:12:00', fromMe: false },
  { id: 'm-2', friendId: '1', text: '응 방금 끝냈어!', sentAt: '2026-08-17T09:15:00', fromMe: true },
  { id: 'm-3', friendId: '2', text: '캘린더 확인해봤어~', sentAt: '2026-08-16T21:40:00', fromMe: false },
  { id: 'm-4', friendId: '3', text: '내일 같이 뛸까?', sentAt: '2026-08-15T18:02:00', fromMe: false },
];
