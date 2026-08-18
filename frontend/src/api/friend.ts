import { authHeaders } from './auth';
import type { Friend, FriendSummary, InviteToken } from '../types/friend';

async function readErrorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const body: unknown = await res.json();
    if (
      body &&
      typeof body === 'object' &&
      'message' in body &&
      typeof (body as { message: unknown }).message === 'string' &&
      (body as { message: string }).message
    ) {
      return (body as { message: string }).message;
    }
  } catch {
    // 응답 본문이 JSON이 아니면 fallback 사용
  }
  return fallback;
}

export async function createInvite(): Promise<InviteToken> {
  const res = await fetch('/api/v1/friends/invites', {
    method: 'POST',
    headers: authHeaders(),
  });
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, '초대 코드 발급에 실패했습니다.'));
  }
  return res.json();
}

export async function acceptInvite(token: string): Promise<void> {
  const res = await fetch(`/api/v1/friends/invites/${encodeURIComponent(token)}/accept`, {
    method: 'POST',
    headers: authHeaders(),
  });
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, '친구 추가에 실패했습니다.'));
  }
}

// GET /api/v1/friends는 { userId, nickname }만 내려주고 avatarUrl이 없다.
// 화면 쪽 Friend 타입(id, name, avatarUrl?, status)과 필드명이 달라서
// FriendRow 등 기존 컴포넌트를 바꾸지 않도록 여기서 매핑해서 반환한다.
function toFriend(summary: FriendSummary): Friend {
  return {
    id: summary.userId,
    name: summary.nickname,
    avatarUrl: undefined,
    status: 'active',
  };
}

export async function getFriends(): Promise<Friend[]> {
  const res = await fetch('/api/v1/friends', { headers: authHeaders() });
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, '친구 목록을 불러오지 못했습니다.'));
  }
  const summaries: FriendSummary[] = await res.json();
  return summaries.map(toFriend);
}
