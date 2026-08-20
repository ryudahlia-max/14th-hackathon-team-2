import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, Sparkles } from 'lucide-react';
import { getAiJob, requestAiImage, type AiJobResponse } from '../api/ai';
import { getChatRooms } from '../api/chat';
import { getMissedFriendRoutines, type MissedRoutineResponse } from '../api/routine';
import { useAuth } from '../auth/authState';
import { ApiError } from '../services/apiClient';

const TERMINAL = new Set(['SUCCEEDED', 'FAILED', 'BLOCKED']);

const FAILURE_MESSAGES: Record<string, string> = {
  OPENAI_BILLING_REQUIRED: 'OpenAI 이미지 사용 한도가 0입니다. 결제 수단 또는 프로젝트 사용 한도를 확인해주세요.',
  OPENAI_RATE_LIMIT: 'OpenAI 요청이 몰려 잠시 제한되었습니다. 잠시 후 다시 시도해주세요.',
  OPENAI_API_KEY_MISSING: '서버에 OpenAI API 키가 설정되지 않았습니다.',
  FACE_ASSET_MISSING: '친구가 프로필 사진을 등록해야 AI 이미지를 만들 수 있어요.',
  FACE_REFERENCE_INVALID: '친구의 프로필 사진에 선명한 한 명의 실제 얼굴이 없어요. 얼굴 사진으로 바꾼 뒤 다시 시도해주세요.',
  OPENAI_REFERENCE_CHECK_REJECTED: '프로필 얼굴 사진을 확인하지 못했습니다. 잠시 후 다시 시도해주세요.',
  OPENAI_REFERENCE_CHECK_INVALID_RESPONSE: '프로필 얼굴 사진을 확인하지 못했습니다. 잠시 후 다시 시도해주세요.',
};

export default function AiImageGenPage() {
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [targetId, setTargetId] = useState('');
  const [routines, setRoutines] = useState<MissedRoutineResponse[]>([]);
  const [job, setJob] = useState<AiJobResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!chatId || !user) return;
    getChatRooms().then(rooms => {
      const room = rooms.find(item => item.id === chatId);
      const target = room?.memberIds.find(id => id !== user.id);
      if (!target) throw new Error('AI 이미지는 1:1 채팅에서만 만들 수 있습니다.');
      setTargetId(target);
      return getMissedFriendRoutines(target);
    }).then(setRoutines).catch(loadError => {
      console.error(loadError);
      setError(loadError instanceof Error ? loadError.message : '미완료 루틴을 불러오지 못했습니다.');
    });
  }, [chatId, user]);

  async function generate(routineId: string) {
    if (!targetId) return;
    setError(null);
    try {
      let current = await requestAiImage(targetId, routineId);
      setJob(current);
      for (let attempt = 0; attempt < 45 && !TERMINAL.has(current.status); attempt += 1) {
        await new Promise(resolve => window.setTimeout(resolve, 2000));
        current = await getAiJob(current.id);
        setJob(current);
      }
      if (!TERMINAL.has(current.status)) setError('생성이 오래 걸리고 있어요. 잠시 후 채팅방에서 확인해주세요.');
      if (current.status === 'FAILED' || current.status === 'BLOCKED') {
        const code = current.failureCode ?? current.status;
        setError(FAILURE_MESSAGES[code] ?? `이미지를 만들지 못했습니다. (${code})`);
      }
    } catch (requestError) {
      console.error(requestError);
      if (requestError instanceof ApiError) {
        setError(FAILURE_MESSAGES[requestError.code] ?? requestError.message);
      } else {
        setError(requestError instanceof Error ? requestError.message : 'AI 이미지 요청에 실패했습니다.');
      }
    }
  }

  const generating = job && !TERMINAL.has(job.status);

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex items-center gap-2 px-4 pt-8 pb-4 border-b border-gray-100">
        <button onClick={() => navigate(-1)} aria-label="뒤로"><ChevronLeft size={22} /></button>
        <div className="w-8 h-8 rounded-full bg-[#a2bfff] flex items-center justify-center"><Sparkles size={16} color="white" /></div>
        <span className="text-base font-bold">AI 미래 이미지</span>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-6">
        <p className="text-sm leading-relaxed text-gray-600 mb-2">친구가 놓친 건강 루틴을 선택하면, 서버가 루틴 종류와 최근 미실천 횟수를 안전한 프롬프트에 반영해 미래 이미지를 만들고 이 채팅방에 자동으로 전송합니다.</p>
        <p className="mb-6 text-xs leading-relaxed text-gray-400">친구 프로필에 한 명의 얼굴이 선명한 실제 사진이 있어야 같은 인물로 만들 수 있습니다. 실루엣·캐릭터·얼굴이 작은 사진은 생성 전에 차단됩니다.</p>
        {routines.length === 0 && !error && <p className="text-sm text-gray-400">생성 가능한 미완료 루틴이 없어요.</p>}
        <div className="space-y-3">
          {routines.map(routine => <button key={routine.routineId} disabled={Boolean(generating)} onClick={() => void generate(routine.routineId)} className="w-full rounded-xl border border-gray-200 px-4 py-3 text-left disabled:opacity-40"><p className="text-sm font-semibold">{routine.title}</p><p className="text-xs text-gray-400">최근 {routine.missedCount}회 미실천 · 마지막 {routine.missedDate}</p></button>)}
        </div>
        {generating && <div className="mt-6 rounded-xl bg-gray-100 px-4 py-4 text-sm text-gray-600">이미지를 생성하고 있어요… ({job?.status})</div>}
        {job?.status === 'SUCCEEDED' && job.outputUrl && <div className="mt-6"><img src={job.outputUrl} alt="생성된 미래 이미지" className="w-full rounded-2xl" /><p className="mt-2 text-xs text-gray-500">채팅방에 자동 전송되었습니다.</p></div>}
        {error && <p className="mt-5 text-sm text-red-500">{error}</p>}
      </div>
    </div>
  );
}
