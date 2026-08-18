import type { ChatRoom, Message } from '../types';
import { FRIENDS } from './mockData';

const CHATS_KEY = 'routine-app:chats';
const MESSAGES_KEY = 'routine-app:chatMessages';

const SEED_CHATS: ChatRoom[] = [
  { id: 'c-1', name: '연진', participantIds: ['2'], isGroup: false, createdAt: '2026-08-17T09:00:00' },
  { id: 'c-2', name: '쩡', participantIds: ['3'], isGroup: false, createdAt: '2026-08-15T18:00:00' },
];

const SEED_MESSAGES: Message[] = [
  { id: 'm-1', chatId: 'c-1', text: '오늘 루틴 다 했어?', sentAt: '2026-08-17T09:12:00', fromMe: false },
  { id: 'm-2', chatId: 'c-1', text: '응 방금 끝냈어!', sentAt: '2026-08-17T09:15:00', fromMe: true },
  { id: 'm-3', chatId: 'c-2', text: '내일 같이 뛸까?', sentAt: '2026-08-15T18:02:00', fromMe: false },
];

function load<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function save<T>(key: string, value: T) {
  localStorage.setItem(key, JSON.stringify(value));
}

export function getChats(): ChatRoom[] {
  return load(CHATS_KEY, SEED_CHATS);
}

export function getChat(chatId: string): ChatRoom | undefined {
  return getChats().find(c => c.id === chatId);
}

export function getMessages(): Message[] {
  return load(MESSAGES_KEY, SEED_MESSAGES);
}

export function getChatMessages(chatId: string): Message[] {
  return getMessages()
    .filter(m => m.chatId === chatId)
    .sort((a, b) => a.sentAt.localeCompare(b.sentAt));
}

export function getLastMessage(chatId: string): Message | null {
  const msgs = getChatMessages(chatId);
  return msgs.length ? msgs[msgs.length - 1] : null;
}

export function getOrCreateDirectChat(friendId: string): string {
  const chats = getChats();
  const existing = chats.find(
    c => !c.isGroup && c.participantIds.length === 1 && c.participantIds[0] === friendId
  );
  if (existing) return existing.id;

  const friend = FRIENDS.find(f => f.id === friendId);
  const chat: ChatRoom = {
    id: `c-${Date.now()}`,
    name: friend?.name ?? '알 수 없음',
    participantIds: [friendId],
    isGroup: false,
    createdAt: new Date().toISOString(),
  };
  save(CHATS_KEY, [...chats, chat]);
  return chat.id;
}

export function createGroupChat(name: string, friendIds: string[]): string {
  const chats = getChats();
  const chat: ChatRoom = {
    id: `c-${Date.now()}`,
    name,
    participantIds: friendIds,
    isGroup: true,
    createdAt: new Date().toISOString(),
  };
  save(CHATS_KEY, [...chats, chat]);
  return chat.id;
}

export function deleteChat(chatId: string) {
  save(CHATS_KEY, getChats().filter(c => c.id !== chatId));
  save(MESSAGES_KEY, getMessages().filter(m => m.chatId !== chatId));
}

export function addMessage(chatId: string, payload: { text?: string; imageUrl?: string; fromMe: boolean }): Message {
  const messages = getMessages();
  const msg: Message = {
    id: `m-${Date.now()}`,
    chatId,
    sentAt: new Date().toISOString(),
    ...payload,
  };
  save(MESSAGES_KEY, [...messages, msg]);
  return msg;
}
