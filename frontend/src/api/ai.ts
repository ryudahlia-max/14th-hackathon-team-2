import { api } from '../services/apiClient';

export interface AiJobResponse {
  id: string;
  status: 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'BLOCKED';
  attemptCount: number;
  outputUrl: string | null;
  failureCode: string | null;
}

export const requestAiImage = (targetUserId: string, occurrenceId: string) =>
  api.post<AiJobResponse>('/api/v1/engagement/ai-generations', {
    targetUserId,
    occurrenceId,
    clientRequestId: crypto.randomUUID(),
  });
export const getAiJob = (jobId: string) =>
  api.get<AiJobResponse>(`/api/v1/engagement/ai-generations/${jobId}`);
