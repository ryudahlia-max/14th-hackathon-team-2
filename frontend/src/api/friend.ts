import { api } from '../services/apiClient';
import type { Friend, FriendSummary, Group, InviteToken } from '../types/friend';

export async function createInvite(): Promise<InviteToken> {
  return api.post<InviteToken>('/api/v1/friends/invites');
}

export async function acceptInvite(token: string): Promise<void> {
  await api.post<unknown>(`/api/v1/friends/invites/${encodeURIComponent(token)}/accept`);
}

function toFriend(summary: FriendSummary): Friend {
  return {
    id: summary.userId,
    name: summary.nickname,
    avatarUrl: summary.avatarUrl ?? undefined,
    status: 'active',
  };
}

export async function getFriends(): Promise<Friend[]> {
  const summaries = await api.get<FriendSummary[]>('/api/v1/friends');
  return summaries.map(toFriend);
}

// GET /api/v1/friends와 마찬가지로 DELETE /api/v1/friends/{friendId}는
// FriendshipController에서 실제로 확인함 (204 No Content).
export async function removeFriend(friendId: string): Promise<void> {
  await api.delete<unknown>(`/api/v1/friends/${encodeURIComponent(friendId)}`);
}

// GroupService.DEFAULT_MAX_MEMBERS = 8 (소유자 포함 총 정원).
export const GROUP_MAX_MEMBERS = 8;
// CreateGroupRequest.memberIds는 @Size(max = 7) — 소유자를 제외하고
// 함께 초대할 수 있는 최대 인원.
export const GROUP_MAX_INVITE_MEMBERS = 7;

interface GroupSummaryResponse {
  id: string;
  name: string;
  ownerId: string;
  memberCount: number;
  maxMembers: number;
}

interface GroupDetailResponse {
  id: string;
  name: string;
  ownerId: string;
  maxMembers: number;
  members: { userId: string; role: string }[];
}

export async function getGroups(): Promise<Group[]> {
  const summaries = await api.get<GroupSummaryResponse[]>('/api/v1/groups');
  return summaries.map((s) => ({
    id: s.id,
    name: s.name,
    memberCount: s.memberCount,
    maxMember: s.maxMembers,
  }));
}

// GroupController.create: POST /api/v1/groups, body { name, memberIds }.
// memberIds는 전부 나와 이미 친구(ACCEPTED)여야 하고 최대 7명까지만 허용된다.
export async function createGroup(name: string, memberIds: string[]): Promise<Group> {
  const detail = await api.post<GroupDetailResponse>('/api/v1/groups', { name, memberIds });
  return {
    id: detail.id,
    name: detail.name,
    memberCount: detail.members.length,
    maxMember: detail.maxMembers,
  };
}
