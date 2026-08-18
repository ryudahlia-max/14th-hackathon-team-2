import { createClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL;
const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

export const isSupabaseConfigured = Boolean(url && anonKey);

if (!isSupabaseConfigured) {
  console.warn(
    'VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY가 설정되지 않았습니다. ' +
      '로그인 기능은 .env에 값을 채우기 전까지 동작하지 않습니다.'
  );
}

// 값이 없을 때도 앱이 죽지 않도록 더미 값으로 클라이언트를 만든다 (실제 요청은 실패함).
export const supabase = createClient(
  url || 'https://placeholder.supabase.co',
  anonKey || 'placeholder-anon-key'
);
