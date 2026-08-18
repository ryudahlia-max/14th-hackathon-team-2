import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import type { Session } from '@supabase/supabase-js';
import { supabase } from '../services/supabaseClient';
import { ensureProfile } from '../api/profile';
import { AuthContext } from './authState';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    supabase.auth.getSession().then(async ({ data }) => {
      setSession(data.session);
      if (data.session) {
        const emailName = data.session.user.email?.split('@')[0] ?? '사용자';
        await ensureProfile(emailName).catch(error => console.error('프로필 초기화에 실패했습니다.', error));
      }
      setLoading(false);
    });

    const { data: subscription } = supabase.auth.onAuthStateChange((_event, newSession) => {
      setSession(newSession);
    });

    return () => subscription.subscription.unsubscribe();
  }, []);

  async function signUp(email: string, password: string) {
    const { data, error } = await supabase.auth.signUp({ email, password });
    if (!error && data.session) {
      try {
        await ensureProfile(email.split('@')[0] || '사용자');
      } catch (profileError) {
        console.error('프로필 초기화에 실패했습니다.', profileError);
        return { error: '프로필 초기화에 실패했습니다. 잠시 후 다시 시도해주세요.', session: data.session };
      }
    }
    return { error: error?.message ?? null, session: data.session };
  }

  async function signIn(email: string, password: string) {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password });
    if (!error) {
      try {
        await ensureProfile(email.split('@')[0] || '사용자');
      } catch (profileError) {
        console.error('프로필 초기화에 실패했습니다.', profileError);
        return { error: '프로필 초기화에 실패했습니다. 잠시 후 다시 시도해주세요.', session: data.session };
      }
    }
    return { error: error?.message ?? null, session: data.session };
  }

  async function signOut() {
    await supabase.auth.signOut();
  }

  return (
    <AuthContext.Provider value={{ session, user: session?.user ?? null, loading, signUp, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}
