import { api } from '../services/apiClient';

export type ChatMessageType = 'TEXT' | 'IMAGE' | 'ROUTINE_CARD' | 'AI_IMAGE' | 'SYSTEM';

export interface ChatMessageResponse {
  id: string;
  roomId: string;
  senderId: string | null;
  type: ChatMessageType;
  content: string | null;
  mediaUrl: string | null;
  createdAt: string;
  reactions: ChatReactionResponse[];
}

export interface ChatReactionResponse {
  userId: string;
  type: string;
  createdAt: string;
}

export interface ChatReactionEvent {
  messageId: string;
  userId: string;
  type: string;
  active: boolean;
}

export interface ChatRoomResponse {
  id: string;
  type: 'DIRECT' | 'GROUP';
  groupId: string | null;
  name: string;
  memberIds: string[];
  createdAt: string;
  lastMessage: ChatMessageResponse | null;
}

export const getChatRooms = () => api.get<ChatRoomResponse[]>('/api/v1/engagement/chat-rooms');
export const createDirectRoom = (targetUserId: string) =>
  api.post<ChatRoomResponse>('/api/v1/engagement/chat-rooms', { type: 'DIRECT', targetUserId });
export const createGroupRoom = (groupId: string) =>
  api.post<ChatRoomResponse>('/api/v1/engagement/chat-rooms', { type: 'GROUP', groupId });
export const getChatMessages = (
  roomId: string,
  cursor?: { createdAt: string; id: string },
  size = 30,
) => {
  const query = new URLSearchParams({ size: String(size) });
  if (cursor) {
    query.set('cursorCreatedAt', cursor.createdAt);
    query.set('cursorId', cursor.id);
  }
  return (
  api.get<{ items: ChatMessageResponse[]; nextCursorCreatedAt: string | null; nextCursorId: string | null }>(
      `/api/v1/engagement/chat-rooms/${roomId}/messages?${query.toString()}`,
    )
  );
};
export const sendChatMessage = (roomId: string, content: string) =>
  api.post<ChatMessageResponse>(`/api/v1/engagement/chat-rooms/${roomId}/messages`, {
    clientMessageId: crypto.randomUUID(),
    type: 'TEXT',
    content,
  });
export const uploadChatMedia = async (file: File) => {
  const form = new FormData();
  form.append('file', file);
  return api.upload<{ objectKey: string; url: string }>('/api/v1/engagement/media', form);
};
export const sendImageMessage = (roomId: string, objectKey: string) =>
  api.post<ChatMessageResponse>(`/api/v1/engagement/chat-rooms/${roomId}/messages`, {
    clientMessageId: crypto.randomUUID(),
    type: 'IMAGE',
    mediaUrl: objectKey,
  });
export const leaveChatRoom = (roomId: string) =>
  api.delete<void>(`/api/v1/engagement/chat-rooms/${roomId}/membership`);
export const addMessageReaction = (messageId: string, type: string) =>
  api.post<void>(`/api/v1/engagement/messages/${messageId}/reactions`, { type });
export const removeMessageReaction = (messageId: string, type: string) =>
  api.delete<void>(`/api/v1/engagement/messages/${messageId}/reactions/${encodeURIComponent(type)}`);
