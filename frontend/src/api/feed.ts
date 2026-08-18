import { api } from '../services/apiClient';
import type { Reaction } from '../types/reaction';

export interface FeedItem {
  completionId: string;
  userId: string;
  nickname: string;
  routineId: string;
  routineTitle: string;
  completionDate: string;
  completedAt: string;
  proofObjectPath: string | null;
  note: string | null;
  myReaction: string | null;
}

export interface ReceivedReaction {
  completionId: string;
  reactorId: string;
  reactorNickname: string;
  routineTitle: string;
  type: string;
  createdAt: string;
}

export const getFeed = () => api.get<{ items: FeedItem[]; nextCursor: string | null }>('/api/v1/feed?limit=50');
export const reactToCompletion = (completionId: string, reaction: Reaction) =>
  api.post(`/api/v1/feed/${completionId}/reaction`, { type: reaction.replace(/[A-Z]/g, m => `_${m}`).toUpperCase() });
export const removeCompletionReaction = (completionId: string) =>
  api.delete<void>(`/api/v1/feed/${completionId}/reaction`);
export const getReceivedReactions = () =>
  api.get<ReceivedReaction[]>('/api/v1/feed/reactions/received');
