import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import { addMessage } from '../data/chatStore';

const ALLOWED_KEYWORDS = [
  '루틴', '건강', '운동', '물', '수분', '다이어트', '수면', '잠', '담배', '금연',
  '음주', '술', '자외선', '선크림', '영양제', '야식', '피부', '스트레칭', '스트레스',
  '식단', '헬스', '러닝', '걷기', '자세', '거북목', '눈건강', '치아', '양치',
];

const BLOCKED_KEYWORDS = ['선정', '노출', '폭력', '혐오', '차별', '정치', '성적'];

function validatePrompt(prompt: string): string | null {
  const trimmed = prompt.trim();
  if (!trimmed) return '프롬프트를 입력해주세요';
  if (BLOCKED_KEYWORDS.some(k => trimmed.includes(k))) {
    return '건강 관리 루틴과 무관하거나 부적절한 내용은 생성할 수 없어요';
  }
  if (!ALLOWED_KEYWORDS.some(k => trimmed.includes(k))) {
    return '건강 관리 루틴 관련 키워드를 포함해주세요 (예: 물, 운동, 수면, 피부, 자외선 등)';
  }
  return null;
}

function buildMockImage(prompt: string): string {
  const safeText = prompt.trim().slice(0, 24) || '루틴 미이행 미래 모습';
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="320" height="320">
      <rect width="320" height="320" fill="#f4ede4"/>
      <circle cx="160" cy="130" r="70" fill="#e3b98f"/>
      <path d="M100 150 q60 60 120 0" stroke="#8a5a3a" stroke-width="6" fill="none"/>
      <circle cx="130" cy="115" r="7" fill="#5c3a24"/>
      <circle cx="190" cy="115" r="7" fill="#5c3a24"/>
      <path d="M90 90 q70 -40 140 0" stroke="#5c3a24" stroke-width="5" fill="none"/>
      <text x="160" y="250" font-size="16" text-anchor="middle" fill="#6e6e6e" font-family="sans-serif">AI 생성 이미지 (목업)</text>
      <text x="160" y="275" font-size="13" text-anchor="middle" fill="#9ca3af" font-family="sans-serif">${safeText}</text>
    </svg>
  `.trim();
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

export default function AiImageGenPage() {
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();

  const [prompt, setPrompt] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<'idle' | 'generating' | 'done'>('idle');
  const [imageUrl, setImageUrl] = useState<string | null>(null);

  function handleGenerate() {
    const validationError = validatePrompt(prompt);
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    setStatus('generating');
    setTimeout(() => {
      setImageUrl(buildMockImage(prompt));
      setStatus('done');
    }, 900);
  }

  function handleSend() {
    if (!chatId || !imageUrl) return;
    addMessage(chatId, { imageUrl, fromMe: true });
    navigate(`/messages/${chatId}`);
  }

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex items-center gap-3 px-4 pt-8 pb-4">
        <button onClick={() => navigate(-1)} aria-label="뒤로">
          <ChevronLeft size={22} color="#333" />
        </button>
        <span className="text-base font-bold">AI 이미지 생성</span>
      </div>

      <div className="flex-1 overflow-y-auto px-4 pb-4 space-y-5">
        <p className="text-sm text-gray-500 leading-relaxed">
          루틴을 안 지키면 벌어질 미래의 피부·노화 모습을 유머러스하게 만들어 친구에게 보내보세요.
          악용 방지를 위해 건강 관리 루틴과 관련된 내용만 생성할 수 있어요.
        </p>

        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-full bg-gray-300 shrink-0" />
          <span className="text-xs text-gray-500">가입 시 등록한 얼굴 사진을 사용해요</span>
        </div>

        <div>
          <label className="text-sm text-gray-500 mb-1.5 block">프롬프트</label>
          <textarea
            value={prompt}
            onChange={e => {
              setPrompt(e.target.value);
              setError(null);
            }}
            placeholder="예: 물을 안 마셔서 푸석푸석해진 피부"
            rows={3}
            className="w-full border border-gray-300 rounded-xl px-4 py-3 text-sm outline-none focus:border-[#a2bfff] resize-none"
          />
          {error && <p className="text-xs text-red-500 mt-1.5">{error}</p>}
        </div>

        <button
          onClick={handleGenerate}
          disabled={status === 'generating'}
          className="w-full py-3 rounded-xl bg-[#a2bfff] text-white font-semibold text-sm disabled:opacity-40"
        >
          {status === 'generating' ? '생성 중...' : '이미지 생성'}
        </button>

        {status === 'done' && imageUrl && (
          <div className="flex flex-col items-center gap-4 pt-2">
            <img src={imageUrl} alt="생성된 이미지" className="w-full max-w-[280px] rounded-2xl border border-gray-100" />
            <button
              onClick={handleSend}
              className="w-full py-3 rounded-xl bg-black text-white font-semibold text-sm"
            >
              채팅방에 보내기
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
