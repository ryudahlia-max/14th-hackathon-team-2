// 프로젝트에 로그인 플로우가 아직 없어 이 키에 실제로 토큰을 쓰는 곳은 없지만,
// localStorage 기반 저장(HomePage/chatStore와 동일한 'routine-app:' 네임스페이스 관례)이
// 이 프로젝트의 유일한 클라이언트 저장 방식이라 로그아웃/계정 삭제 시 그 규칙에 맞춰 제거한다.
const AUTH_TOKEN_KEY = 'routine-app:authToken';

export function clearAuthToken() {
  localStorage.removeItem(AUTH_TOKEN_KEY);
}

export function getAuthToken(): string | null {
  return localStorage.getItem(AUTH_TOKEN_KEY);
}

// 인증이 필요한 /api/v1/** 요청에 붙일 공통 헤더. 로그인 플로우가 아직 없어
// 토큰이 없을 때가 많으므로, 없으면 Authorization 헤더 자체를 생략한다.
export function authHeaders(): HeadersInit {
  const token = getAuthToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// TODO: 백엔드에 아직 POST /api/v1/auth/logout 엔드포인트가 없습니다 (SecurityConfig는
// Supabase 발급 JWT를 검증만 하는 stateless 리소스 서버). 엔드포인트 주소가 확정되면 아래를 맞춰주세요.
export async function logout(): Promise<void> {
  const res = await fetch('/api/v1/auth/logout', { method: 'POST' });
  if (!res.ok) {
    throw new Error(`로그아웃 요청이 실패했습니다. (status: ${res.status})`);
  }
}

// TODO: 백엔드 ProfileController(/api/v1/me)에는 아직 DELETE가 없습니다(GET/POST/PATCH만 존재).
// 엔드포인트가 추가되면 경로/메서드를 맞춰주세요.
export async function deleteAccount(): Promise<void> {
  const res = await fetch('/api/v1/me', { method: 'DELETE' });
  if (!res.ok) {
    throw new Error(`계정 삭제 요청이 실패했습니다. (status: ${res.status})`);
  }
}
