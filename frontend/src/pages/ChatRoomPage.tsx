import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, LogOut, Plus, Image as ImageIcon, Camera, Smile } from 'lucide-react';
import {
  addMessageReaction,
  getChatMessages,
  getChatRooms,
  leaveChatRoom,
  removeMessageReaction,
  sendChatMessage,
  sendImageMessage,
  uploadChatMedia,
  type ChatMessageResponse,
  type ChatReactionEvent,
  type ChatRoomResponse,
} from '../api/chat';
import { useAuth } from '../auth/authState';
import { supabase } from '../services/supabaseClient';
import type { RealtimeChannel } from '@supabase/supabase-js';

const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const REACTIONS = [
  { type: 'HEART', label: '❤️' },
  { type: 'SAD', label: '😢' },
  { type: 'THUMBS_UP', label: '👍' },
  { type: 'FIRE', label: '🔥' },
  { type: 'SMILE', label: '😊' },
] as const;

function formatTime(sentAt: string) {
  return new Date(sentAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
}

function appendUnique(messages: ChatMessageResponse[], incoming: ChatMessageResponse) {
  if (messages.some(message => message.id === incoming.id)) return messages;
  return [...messages, { ...incoming, reactions: incoming.reactions ?? [] }];
}

function applyReaction(messages: ChatMessageResponse[], event: ChatReactionEvent) {
  return messages.map(message => {
    if (message.id !== event.messageId) return message;
    const without = message.reactions.filter(reaction =>
      !(reaction.userId === event.userId && reaction.type === event.type));
    return {
      ...message,
      reactions: event.active
        ? [...without, { userId: event.userId, type: event.type, createdAt: new Date().toISOString() }]
        : without,
    };
  });
}

export default function ChatRoomPage() {
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const { session, user } = useAuth();
  const [chat, setChat] = useState<ChatRoomResponse | null>(null);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [cursor, setCursor] = useState<{ createdAt: string; id: string } | null>(null);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [text, setText] = useState('');
  const [showAttach, setShowAttach] = useState(false);
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const photoInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const messageListRef = useRef<HTMLDivElement>(null);

  function scrollToBottom() {
    requestAnimationFrame(() => {
      const list = messageListRef.current;
      if (list) list.scrollTop = list.scrollHeight;
    });
  }

  useEffect(() => {
    if (!chatId) return;
    setError(null);
    Promise.all([getChatRooms(), getChatMessages(chatId)])
      .then(([rooms, page]) => {
        setChat(rooms.find(room => room.id === chatId) ?? null);
        setMessages([...page.items].reverse().map(message => ({ ...message, reactions: message.reactions ?? [] })));
        setCursor(page.nextCursorCreatedAt && page.nextCursorId
          ? { createdAt: page.nextCursorCreatedAt, id: page.nextCursorId }
          : null);
        scrollToBottom();
      })
      .catch(loadError => { console.error(loadError); setError('채팅을 불러오지 못했습니다.'); });
  }, [chatId]);

  useEffect(() => {
    if (!chatId || !session?.access_token) return;
    let channel: RealtimeChannel | null = null;
    let cancelled = false;
    void (async () => {
      await supabase.realtime.setAuth(session.access_token);
      if (cancelled) return;
      channel = supabase.channel(`chat-room:${chatId}`, { config: { private: true } });
      channel
        .on('broadcast', { event: 'message.created' }, event => {
          const incoming = event.payload as ChatMessageResponse;
          setMessages(previous => appendUnique(previous, incoming));
          scrollToBottom();
        })
        .on('broadcast', { event: 'message.reaction.updated' }, event => {
          setMessages(previous => applyReaction(previous, event.payload as ChatReactionEvent));
        })
        .subscribe(status => {
          if (status === 'CHANNEL_ERROR' || status === 'TIMED_OUT') {
            setError('실시간 연결이 끊겼습니다. 다시 접속해 주세요.');
          }
        });
    })();
    return () => {
      cancelled = true;
      if (channel) void supabase.removeChannel(channel);
    };
  }, [chatId, session?.access_token]);

  async function loadOlder() {
    if (!chatId || !cursor || loadingOlder) return;
    const list = messageListRef.current;
    const previousHeight = list?.scrollHeight ?? 0;
    const previousTop = list?.scrollTop ?? 0;
    setLoadingOlder(true);
    try {
      const page = await getChatMessages(chatId, cursor);
      setMessages(previous => [
        ...[...page.items].reverse().filter(item => !previous.some(existing => existing.id === item.id)),
        ...previous,
      ]);
      setCursor(page.nextCursorCreatedAt && page.nextCursorId
        ? { createdAt: page.nextCursorCreatedAt, id: page.nextCursorId }
        : null);
      requestAnimationFrame(() => {
        if (list) list.scrollTop = list.scrollHeight - previousHeight + previousTop;
      });
    } catch (loadError) {
      console.error(loadError);
      setError('이전 메시지를 불러오지 못했습니다.');
    } finally {
      setLoadingOlder(false);
    }
  }

  async function handleSend() {
    if (!text.trim() || !chatId) return;
    const content = text.trim();
    setText('');
    try {
      const message = await sendChatMessage(chatId, content);
      setMessages(previous => appendUnique(previous, message));
      scrollToBottom();
    } catch (sendError) {
      console.error(sendError);
      setError('메시지를 보내지 못했습니다.');
      setText(content);
    }
  }

  async function handleImagePick(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || !chatId) return;
    if (file.size > MAX_IMAGE_BYTES) {
      setError('이미지는 10MB 이하만 업로드할 수 있습니다.');
      return;
    }
    try {
      const uploaded = await uploadChatMedia(file);
      const message = await sendImageMessage(chatId, uploaded.objectKey);
      setMessages(previous => appendUnique(previous, { ...message, mediaUrl: uploaded.url }));
      setShowAttach(false);
      scrollToBottom();
    } catch (uploadError) {
      console.error(uploadError);
      setError('사진을 보내지 못했습니다.');
    }
  }

  async function toggleReaction(message: ChatMessageResponse, type: string) {
    if (!user) return;
    const active = message.reactions.some(reaction => reaction.userId === user.id && reaction.type === type);
    try {
      if (active) await removeMessageReaction(message.id, type);
      else await addMessageReaction(message.id, type);
      setMessages(previous => applyReaction(previous, {
        messageId: message.id,
        userId: user.id,
        type,
        active: !active,
      }));
    } catch (reactionError) {
      console.error(reactionError);
      setError('메시지 공감을 저장하지 못했습니다.');
    }
  }

  async function handleLeaveChat() {
    if (!chatId) return;
    await leaveChatRoom(chatId);
    navigate('/messages');
  }

  if (!chatId) return null;

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex items-center justify-between px-4 pt-8 pb-4">
        <button onClick={() => navigate('/messages')} aria-label="뒤로"><ChevronLeft size={22} /></button>
        <span className="text-base font-bold">{chat?.name ?? '채팅'}</span>
        <button onClick={() => setShowLeaveConfirm(true)} aria-label="채팅방 나가기"><LogOut size={20} color="#6e6e6e" /></button>
      </div>
      {error && <p className="px-4 pb-1 text-xs text-red-500">{error}</p>}
      <div ref={messageListRef} className="flex-1 overflow-y-auto px-4 py-2 space-y-3">
        {cursor && (
          <button
            onClick={() => void loadOlder()}
            disabled={loadingOlder}
            className="mx-auto block rounded-full border px-4 py-1.5 text-xs text-gray-500 disabled:opacity-50"
          >
            {loadingOlder ? '불러오는 중...' : '이전 메시지 보기'}
          </button>
        )}
        {messages.map(message => {
          const fromMe = message.senderId === user?.id;
          return (
            <div key={message.id} className={`flex flex-col ${fromMe ? 'items-end' : 'items-start'}`}>
              {message.mediaUrl ? <img src={message.mediaUrl} alt="" className="w-40 h-40 object-cover rounded-2xl" /> : (
                <div className={`max-w-[75%] px-4 py-2.5 rounded-2xl text-sm ${fromMe ? 'bg-[#a2bfff] text-white' : 'bg-gray-100 text-gray-900'}`}>{message.content}</div>
              )}
              <div className="mt-1 flex flex-wrap gap-1">
                {REACTIONS.map(reaction => {
                  const matching = message.reactions.filter(item => item.type === reaction.type);
                  const mine = matching.some(item => item.userId === user?.id);
                  return (
                    <button
                      key={reaction.type}
                      aria-label={`${reaction.type} 공감`}
                      onClick={() => void toggleReaction(message, reaction.type)}
                      className={`rounded-full border px-1.5 py-0.5 text-[10px] ${mine ? 'border-[#7ea2f4] bg-[#e8efff]' : 'border-gray-200'}`}
                    >
                      {reaction.label}{matching.length > 0 ? ` ${matching.length}` : ''}
                    </button>
                  );
                })}
              </div>
              <span className="text-[10px] text-gray-400 mt-1">{formatTime(message.createdAt)}</span>
            </div>
          );
        })}
      </div>
      <div className="px-4 pb-3"><div className="flex items-center gap-2 border border-[#6e6e6e] rounded-full px-3 py-2">
        <button onClick={() => setShowAttach(value => !value)} aria-label="첨부"><Plus size={20} color="#6e6e6e" /></button>
        <input value={text} onChange={event => setText(event.target.value)} onKeyDown={event => event.key === 'Enter' && void handleSend()} placeholder="메시지 입력" className="flex-1 text-sm outline-none" />
      </div></div>
      {showAttach && <div className="flex items-center justify-around px-6 pb-6">
        <input ref={photoInputRef} type="file" accept="image/png,image/jpeg,image/webp" hidden onChange={handleImagePick} />
        <input ref={cameraInputRef} type="file" accept="image/png,image/jpeg,image/webp" capture="environment" hidden onChange={handleImagePick} />
        <button onClick={() => photoInputRef.current?.click()} className="flex flex-col items-center gap-1.5"><div className="w-14 h-14 rounded-full bg-[#bccff8]/50 flex items-center justify-center"><ImageIcon size={22} /></div><span className="text-xs">사진</span></button>
        <button onClick={() => cameraInputRef.current?.click()} className="flex flex-col items-center gap-1.5"><div className="w-14 h-14 rounded-full bg-[#bccff8]/50 flex items-center justify-center"><Camera size={22} /></div><span className="text-xs">카메라</span></button>
        {chat?.type === 'DIRECT' && <button onClick={() => navigate(`/messages/${chatId}/ai-image`)} className="flex flex-col items-center gap-1.5"><div className="w-14 h-14 rounded-full bg-[#bccff8]/50 flex items-center justify-center"><Smile size={22} /></div><span className="text-xs">AI 이미지</span></button>}
      </div>}
      {showLeaveConfirm && <div className="fixed inset-0 z-50 flex items-end justify-center"><div className="absolute inset-0 bg-black/40" onClick={() => setShowLeaveConfirm(false)} /><div className="relative bg-white rounded-t-2xl w-full max-w-[393px] px-6 pt-6 pb-10"><h2 className="text-lg font-bold mb-2">채팅방을 나가시겠어요?</h2><div className="flex gap-3 mt-6"><button onClick={() => setShowLeaveConfirm(false)} className="flex-1 py-3 rounded-xl border">취소</button><button onClick={() => void handleLeaveChat()} className="flex-1 py-3 rounded-xl bg-red-500 text-white">나가기</button></div></div></div>}
    </div>
  );
}
