import { api, ApiError } from '../services/apiClient';

export interface ProfileResponse {
  id: string;
  nickname: string;
  avatarObjectPath: string | null;
  avatarUrl: string | null;
  aiFaceConsent: boolean;
  timezone: string;
  createdAt: string;
  updatedAt: string;
}

export function getProfile() {
  return api.get<ProfileResponse>('/api/v1/me');
}

export function bootstrapProfile(nickname: string) {
  return api.post<ProfileResponse>('/api/v1/me/bootstrap', {
    nickname: nickname.slice(0, 30) || '사용자',
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Seoul',
    termsVersion: '2026-08-01',
    privacyVersion: '2026-08-01',
  });
}

export async function ensureProfile(fallbackNickname: string) {
  try {
    return await getProfile();
  } catch (error) {
    if (error instanceof ApiError && error.status === 404 && error.code === 'PROFILE_NOT_FOUND') {
      return bootstrapProfile(fallbackNickname);
    }
    throw error;
  }
}

export function updateProfile(profile: Pick<ProfileResponse, 'nickname' | 'timezone' | 'avatarObjectPath' | 'aiFaceConsent'>) {
  return api.patch<ProfileResponse>('/api/v1/me', profile);
}

export function uploadAvatar(file: File) {
  const form = new FormData();
  form.append('file', file);
  return api.upload<ProfileResponse>('/api/v1/me/avatar', form);
}
