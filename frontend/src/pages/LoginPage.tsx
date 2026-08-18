import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { isSupabaseConfigured } from '../services/supabaseClient';

type Mode = 'signIn' | 'signUp';

export default function LoginPage() {
  const navigate = useNavigate();
  const { signIn, signUp } = useAuth();
  const [mode, setMode] = useState<Mode>('signIn');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [signedUpNotice, setSignedUpNotice] = useState(false);

  async function handleSubmit() {
    if (!email.trim() || !password.trim()) return;
    setSubmitting(true);
    setError(null);
    setSignedUpNotice(false);

    const result = mode === 'signIn' ? await signIn(email.trim(), password) : await signUp(email.trim(), password);

    setSubmitting(false);
    if (result.error) {
      setError(result.error);
      return;
    }
    if (mode === 'signUp') {
      setSignedUpNotice(true);
      return;
    }
    navigate('/');
  }

  return (
    <div className="flex flex-col h-full bg-white px-7 pt-16">
      <h1 className="text-xl font-bold mb-1">{mode === 'signIn' ? '로그인' : '회원가입'}</h1>
      <p className="text-sm text-gray-500 mb-8">
        {mode === 'signIn' ? '루틴을 이어가려면 로그인하세요' : '이메일로 계정을 만들어요'}
      </p>

      {!isSupabaseConfigured && (
        <p className="text-xs text-red-500 mb-4">
          Supabase 연결 정보가 아직 설정되지 않았어요 (.env의 VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY).
        </p>
      )}

      <div className="mb-4">
        <label className="text-sm text-gray-500 mb-1.5 block">이메일</label>
        <input
          type="email"
          value={email}
          onChange={e => setEmail(e.target.value)}
          className="w-full border border-gray-300 rounded-xl px-4 py-3 text-sm outline-none focus:border-[#a2bfff]"
          autoFocus
        />
      </div>

      <div className="mb-6">
        <label className="text-sm text-gray-500 mb-1.5 block">비밀번호</label>
        <input
          type="password"
          value={password}
          onChange={e => setPassword(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSubmit()}
          className="w-full border border-gray-300 rounded-xl px-4 py-3 text-sm outline-none focus:border-[#a2bfff]"
        />
      </div>

      {error && <p className="text-xs text-red-500 mb-4">{error}</p>}
      {signedUpNotice && (
        <p className="text-xs text-[#6e6e6e] mb-4">
          가입 확인 메일을 보냈어요. 메일함을 확인한 뒤 로그인해주세요.
        </p>
      )}

      <button
        onClick={handleSubmit}
        disabled={!email.trim() || !password.trim() || submitting}
        className="w-full py-3 rounded-xl bg-[#a2bfff] text-white font-semibold text-sm disabled:opacity-40 mb-4"
      >
        {submitting ? '처리 중...' : mode === 'signIn' ? '로그인' : '회원가입'}
      </button>

      <button
        onClick={() => {
          setMode(m => (m === 'signIn' ? 'signUp' : 'signIn'));
          setError(null);
          setSignedUpNotice(false);
        }}
        className="text-sm text-[#6e6e6e] underline"
      >
        {mode === 'signIn' ? '계정이 없으신가요? 회원가입' : '이미 계정이 있으신가요? 로그인'}
      </button>
    </div>
  );
}
