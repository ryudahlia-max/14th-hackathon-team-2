import { supabase } from './supabaseClient';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(
    status: number,
    code: string,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

async function request<T>(path: string, options: RequestInit = {}, accessToken?: string): Promise<T> {
  const token = accessToken ?? (await supabase.auth.getSession()).data.session?.access_token;

  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const res = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });

  if (!res.ok) {
    const body = await res.json().catch(() => null) as { code?: string; message?: string } | null;
    throw new ApiError(
      res.status,
      body?.code ?? 'API_REQUEST_FAILED',
      body?.message ?? `요청에 실패했습니다. (${res.status})`,
    );
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  get: <T>(path: string, accessToken?: string) => request<T>(path, {}, accessToken),
  post: <T>(path: string, body?: unknown, accessToken?: string) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }, accessToken),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  upload: <T>(path: string, formData: FormData) => request<T>(path, { method: 'POST', body: formData }),
};
