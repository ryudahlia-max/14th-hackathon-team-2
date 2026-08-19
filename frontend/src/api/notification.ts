import { api } from '../services/apiClient';

export interface ServiceNotification {
  id: string;
  userId: string;
  type: 'CHAT_MESSAGE' | 'CHAT_REACTION' | 'AI_COMPLETED' | 'ROUTINE_REMINDER' | 'MONTHLY_RECAP' | string;
  content: string;
  readAt: string | null;
  createdAt: string;
}

export const getServiceNotifications = () =>
  api.get<ServiceNotification[]>('/api/v1/engagement/notifications?size=100');

export const markNotificationRead = (id: string) =>
  api.patch<void>(`/api/v1/engagement/notifications/${id}/read`);

export const markAllNotificationsRead = () =>
  api.patch<void>('/api/v1/engagement/notifications/read-all');
