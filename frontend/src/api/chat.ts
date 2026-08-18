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
export const getChatMessages = (roomId: string) =>
  api.get<{ items: ChatMessageResponse[]; nextCursorCreatedAt: string | null; nextCursorId: string | null }>(
    `/api/v1/engagement/chat-rooms/${roomId}/messages?size=100`,
  );
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
