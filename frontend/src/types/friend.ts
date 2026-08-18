export type FriendStatus = 'active' | 'blocked' | 'pending';

export interface Friend {
  id: string;
  name: string;
  avatarUrl?: string;
  status: FriendStatus;
}

export interface Group {
  id: string;
  name: string;
  memberCount: number;
  maxMember: number;
}

// 현재 백엔드(/api/v1/friends)는 "토큰 발급 → 즉시 수락" 구조라 대기 중인
// 친구 요청 목록 개념이 없다. 이 타입은 이 기능에서는 사용하지 않고,
// 추후 백엔드에 pending 요청 모델이 추가될 경우를 대비해 남겨둔다.
export interface FriendRequest {
  id: string;
  name: string;
  avatarUrl?: string;
}

export interface InviteToken {
  token: string;
  expiresAt: string; // ISO timestamp
}

// GET /api/v1/friends 응답 원형 (FriendshipService.FriendSummary). 화면에서는
// api/friend.ts가 이 값을 Friend 타입으로 매핑해서 사용한다.
export interface FriendSummary {
  userId: string;
  nickname: string;
}
