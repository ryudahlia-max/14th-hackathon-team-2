import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, Send, Sparkles } from 'lucide-react';
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
    return '건강 관리 루틴과 무관하거나 부적절한 내용은 생성할 수 없어요. 다른 프롬프트로 다시 시도해볼까요?';
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

type Turn =
  | { id: string; role: 'bot'; kind: 'text'; text: string }
  | { id: string; role: 'user'; kind: 'text'; text: string }
  | { id: string; role: 'bot'; kind: 'error'; text: string }
  | { id: string; role: 'bot'; kind: 'loading' }
  | { id: string; role: 'bot'; kind: 'image'; imageUrl: string; sent: boolean };

function makeId() {
  return `t-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
}

const INTRO_TEXT =
  '안녕하세요! 루틴을 안 지키면 벌어질 미래의 피부·노화 모습을 유머러스하게 만들어드려요. ' +
  '악용 방지를 위해 건강 관리 루틴과 관련된 내용만 생성할 수 있어요. 어떤 모습을 만들어드릴까요?';

export default function AiImageGenPage() {
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();

  const [turns, setTurns] = useState<Turn[]>(() => [{ id: makeId(), role: 'bot', kind: 'text', text: INTRO_TEXT }]);
  const [input, setInput] = useState('');
  const [generating, setGenerating] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [turns]);

  function handleSubmit() {
    const prompt = input.trim();
    if (!prompt || generating) return;
    setInput('');

    const userTurn: Turn = { id: makeId(), role: 'user', kind: 'text', text: prompt };
    setTurns(prev => [...prev, userTurn]);

    const validationError = validatePrompt(prompt);
    if (validationError) {
      setTurns(prev => [...prev, { id: makeId(), role: 'bot', kind: 'error', text: validationError }]);
      return;
    }

    const loadingId = makeId();
    setGenerating(true);
    setTurns(prev => [...prev, { id: loadingId, role: 'bot', kind: 'loading' }]);

    setTimeout(() => {
      const imageUrl = buildMockImage(prompt);
      setTurns(prev =>
        prev.map(t => (t.id === loadingId ? { id: loadingId, role: 'bot', kind: 'image', imageUrl, sent: false } : t))
      );
      setGenerating(false);
    }, 900);
  }

  function handleSend(turnId: string, imageUrl: string) {
    if (!chatId) return;
    addMessage(chatId, { imageUrl, fromMe: true });
    setTurns(prev =>
      prev.map(t => (t.id === turnId && t.kind === 'image' ? { ...t, sent: true } : t))
    );
  }

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex items-center gap-2 px-4 pt-8 pb-4 border-b border-gray-100">
        <button onClick={() => navigate(-1)} aria-label="뒤로">
          <ChevronLeft size={22} color="#333" />
        </button>
        <div className="w-8 h-8 rounded-full bg-[#a2bfff] flex items-center justify-center shrink-0">
          <Sparkles size={16} color="white" />
        </div>
        <span className="text-base font-bold">AI 이미지 생성</span>
      </div>

      <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
        {turns.map(turn => {
          if (turn.role === 'user') {
            return (
              <div key={turn.id} className="flex justify-end">
                <div className="max-w-[75%] px-4 py-2.5 rounded-2xl rounded-tr-sm text-sm bg-[#a2bfff] text-white">
                  {turn.text}
                </div>
              </div>
            );
          }

          if (turn.kind === 'text' || turn.kind === 'error') {
            return (
              <div key={turn.id} className="flex items-start gap-2">
                <div className="w-7 h-7 rounded-full bg-[#a2bfff] flex items-center justify-center shrink-0 mt-0.5">
                  <Sparkles size={13} color="white" />
                </div>
                <div
                  className={`max-w-[80%] px-4 py-2.5 rounded-2xl rounded-tl-sm text-sm leading-relaxed ${
                    turn.kind === 'error' ? 'bg-red-50 text-red-500' : 'bg-gray-100 text-gray-800'
                  }`}
                >
                  {turn.text}
                </div>
              </div>
            );
          }

          if (turn.kind === 'loading') {
            return (
              <div key={turn.id} className="flex items-start gap-2">
                <div className="w-7 h-7 rounded-full bg-[#a2bfff] flex items-center justify-center shrink-0 mt-0.5">
                  <Sparkles size={13} color="white" />
                </div>
                <div className="px-4 py-3 rounded-2xl rounded-tl-sm bg-gray-100 flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:-0.3s]" />
                  <span className="w-1.5 h-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:-0.15s]" />
                  <span className="w-1.5 h-1.5 rounded-full bg-gray-400 animate-bounce" />
                </div>
              </div>
            );
          }

          // image
          return (
            <div key={turn.id} className="flex items-start gap-2">
              <div className="w-7 h-7 rounded-full bg-[#a2bfff] flex items-center justify-center shrink-0 mt-0.5">
                <Sparkles size={13} color="white" />
              </div>
              <div className="flex flex-col gap-2 max-w-[75%]">
                <img src={turn.imageUrl} alt="생성된 이미지" className="w-full rounded-2xl rounded-tl-sm border border-gray-100" />
                <button
                  onClick={() => handleSend(turn.id, turn.imageUrl)}
                  disabled={turn.sent}
                  className="py-2 rounded-xl bg-black text-white font-semibold text-xs disabled:opacity-40"
                >
                  {turn.sent ? '채팅방에 전송됨' : '채팅방에 보내기'}
                </button>
              </div>
            </div>
          );
        })}
      </div>

      <div className="px-4 pb-6 pt-2">
        <div className="flex items-center gap-2 border border-[#6e6e6e] rounded-full px-3 py-2">
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSubmit()}
            placeholder="예: 물을 안 마셔서 푸석푸석해진 피부"
            className="flex-1 text-sm outline-none placeholder:text-gray-400"
          />
          <button
            onClick={handleSubmit}
            disabled={!input.trim() || generating}
            aria-label="보내기"
            className="w-7 h-7 rounded-full bg-[#a2bfff] flex items-center justify-center shrink-0 disabled:opacity-40"
          >
            <Send size={13} color="white" />
          </button>
        </div>
      </div>
    </div>
  );
}
