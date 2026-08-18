import { useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, LogOut, Plus, Image as ImageIcon, Camera, Smile } from 'lucide-react';
import { addMessage, deleteChat, getChat, getChatMessages } from '../data/chatStore';

function formatTime(sentAt: string) {
  const d = new Date(sentAt);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

export default function ChatRoomPage() {
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const chat = chatId ? getChat(chatId) : undefined;

  const [messages, setMessages] = useState(() => (chatId ? getChatMessages(chatId) : []));
  const [text, setText] = useState('');
  const [showAttach, setShowAttach] = useState(false);
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false);
  const photoInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  if (!chatId || !chat) {
    return (
      <div className="flex flex-col h-full items-center justify-center gap-4 bg-white">
        <p className="text-sm text-gray-400">채팅방을 찾을 수 없어요</p>
        <button onClick={() => navigate('/messages')} className="text-sm text-[#6e6e6e] underline">
          메시지 목록으로
        </button>
      </div>
    );
  }

  function handleSend() {
    if (!text.trim() || !chatId) return;
    addMessage(chatId, { text: text.trim(), fromMe: true });
    setMessages(getChatMessages(chatId));
    setText('');
  }

  function handleImagePick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !chatId) return;
    const imageUrl = URL.createObjectURL(file);
    addMessage(chatId, { imageUrl, fromMe: true });
    setMessages(getChatMessages(chatId));
    setShowAttach(false);
  }

  function handleLeaveChat() {
    if (!chatId) return;
    deleteChat(chatId);
    navigate('/messages');
  }

  return (
    <div className="flex flex-col h-full bg-white">
      {/* Header */}
      <div className="flex items-center justify-between px-4 pt-8 pb-4">
        <button onClick={() => navigate('/messages')} aria-label="뒤로">
          <ChevronLeft size={22} color="#333" />
        </button>
        <span className="text-base font-bold">{chat.name}</span>
        <button onClick={() => setShowLeaveConfirm(true)} aria-label="채팅방 나가기">
          <LogOut size={20} color="#6e6e6e" />
        </button>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-2 space-y-3">
        {messages.map(m => (
          <div key={m.id} className={`flex flex-col ${m.fromMe ? 'items-end' : 'items-start'}`}>
            {m.imageUrl ? (
              <img src={m.imageUrl} alt="" className="w-40 h-40 object-cover rounded-2xl" />
            ) : (
              <div
                className="max-w-[75%] px-4 py-2.5 rounded-2xl text-sm"
                style={{
                  background: m.fromMe ? '#a2bfff' : '#f3f4f6',
                  color: m.fromMe ? 'white' : '#111827',
                }}
              >
                {m.text}
              </div>
            )}
            <span className="text-[10px] text-gray-400 mt-1">{formatTime(m.sentAt)}</span>
          </div>
        ))}
      </div>

      {/* Input bar */}
      <div className="px-4 pb-3">
        <div className="flex items-center gap-2 border border-[#6e6e6e] rounded-full px-3 py-2">
          <button onClick={() => setShowAttach(v => !v)} aria-label="첨부">
            <Plus size={20} color="#6e6e6e" />
          </button>
          <input
            value={text}
            onChange={e => setText(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSend()}
            placeholder="메시지 입력"
            className="flex-1 text-sm outline-none placeholder:text-gray-400"
          />
        </div>
      </div>

      {/* Attach row */}
      {showAttach && (
        <div className="flex items-center justify-around px-6 pb-6">
          <input ref={photoInputRef} type="file" accept="image/*" hidden onChange={handleImagePick} />
          <input
            ref={cameraInputRef}
            type="file"
            accept="image/*"
            capture="environment"
            hidden
            onChange={handleImagePick}
          />

          <button onClick={() => photoInputRef.current?.click()} className="flex flex-col items-center gap-1.5">
            <div className="w-14 h-14 rounded-full bg-[#bccff8]/50 flex items-center justify-center">
              <ImageIcon size={22} color="#6e6e6e" />
            </div>
            <span className="text-xs text-gray-500">사진</span>
          </button>

          <button onClick={() => cameraInputRef.current?.click()} className="flex flex-col items-center gap-1.5">
            <div className="w-14 h-14 rounded-full bg-[#bccff8]/50 flex items-center justify-center">
              <Camera size={22} color="#6e6e6e" />
            </div>
            <span className="text-xs text-gray-500">카메라</span>
          </button>

          <button
            onClick={() => navigate(`/messages/${chatId}/ai-image`)}
            className="flex flex-col items-center gap-1.5"
          >
            <div className="w-14 h-14 rounded-full bg-[#bccff8]/50 flex items-center justify-center">
              <Smile size={22} color="#6e6e6e" />
            </div>
            <span className="text-xs text-gray-500">AI 이미지</span>
          </button>
        </div>
      )}

      {showLeaveConfirm && (
        <div className="fixed inset-0 z-50 flex items-end justify-center">
          <div className="absolute inset-0 bg-black/40" onClick={() => setShowLeaveConfirm(false)} />
          <div className="relative bg-white rounded-t-2xl w-full max-w-[393px] px-6 pt-6 pb-10">
            <h2 className="text-lg font-bold mb-2">채팅방을 나가시겠어요?</h2>
            <p className="text-sm text-gray-500 mb-6">나가면 대화 내용이 모두 삭제되고 목록에서 사라져요.</p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowLeaveConfirm(false)}
                className="flex-1 py-3 rounded-xl border border-gray-300 text-sm font-semibold text-gray-700"
              >
                취소
              </button>
              <button
                onClick={handleLeaveChat}
                className="flex-1 py-3 rounded-xl bg-red-500 text-white text-sm font-semibold"
              >
                나가기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
