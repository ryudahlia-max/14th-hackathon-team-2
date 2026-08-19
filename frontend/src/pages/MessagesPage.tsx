import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import AppNavigationBar from '../components/AppNavigationBar';
import Avatar from '../components/Avatar';
import NewChatModal from '../components/NewChatModal';
import { createGroup, getFriends } from '../api/friend';
import { createDirectRoom, createGroupRoom, getChatRooms, type ChatRoomResponse } from '../api/chat';
import { formatRelativeTime } from '../utils/formatRelativeTime';
import type { Friend } from '../types/friend';
import { useAuth } from '../auth/authState';
import { supabase } from '../services/supabaseClient';
import type { RealtimeChannel } from '@supabase/supabase-js';
import type { ChatMessageResponse } from '../api/chat';

export default function MessagesPage() {
  const navigate = useNavigate();
  const { session, user } = useAuth();
  const [chats, setChats] = useState<ChatRoomResponse[]>([]);
  const [friends, setFriends] = useState<Friend[]>([]);
  const [showNewChat, setShowNewChat] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([getChatRooms(), getFriends()])
      .then(([rooms, friendList]) => { setChats(rooms); setFriends(friendList); })
      .catch(loadError => { console.error(loadError); setError('채팅 목록을 불러오지 못했습니다.'); });
  }, []);

  const roomIdsKey = chats.map(chat => chat.id).sort().join(',');
  useEffect(() => {
    if (!session?.access_token || !roomIdsKey) return;
    let cancelled = false;
    const channels: RealtimeChannel[] = [];
    void (async () => {
      await supabase.realtime.setAuth(session.access_token);
      if (cancelled) return;
      for (const roomId of roomIdsKey.split(',')) {
        const channel = supabase.channel(`chat-room:${roomId}`, { config: { private: true } });
        channels.push(channel);
        channel
          .on('broadcast', { event: 'message.created' }, event => {
            const message = event.payload as ChatMessageResponse;
            setChats(previous => previous.map(chat =>
              chat.id === roomId ? { ...chat, lastMessage: message } : chat));
          })
          .subscribe(status => {
            if (status === 'CHANNEL_ERROR' || status === 'TIMED_OUT') {
              setError('채팅 목록 실시간 연결이 끊겼습니다.');
            }
          });
      }
    })();
    return () => {
      cancelled = true;
      channels.forEach(channel => { void supabase.removeChannel(channel); });
    };
  }, [roomIdsKey, session?.access_token]);

  async function handleCreateChat(friendIds: string[], groupName: string) {
    try {
      const room = friendIds.length === 1
        ? await createDirectRoom(friendIds[0])
        : await createGroupRoom((await createGroup(groupName || '새 그룹', friendIds)).id);
      setShowNewChat(false);
      navigate(`/messages/${room.id}`);
    } catch (createError) {
      console.error(createError);
      setError('채팅방을 만들지 못했습니다.');
    }
  }

  const conversations = [...chats].sort((a, b) =>
    (b.lastMessage?.createdAt ?? b.createdAt).localeCompare(a.lastMessage?.createdAt ?? a.createdAt));

  function directPeer(chat: ChatRoomResponse) {
    if (chat.type !== 'DIRECT') return null;
    const peerId = chat.memberIds.find(id => id !== user?.id) ?? null;
    return peerId ? friends.find(friend => friend.id === peerId) ?? null : null;
  }

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex items-center justify-between px-7 pt-4 pb-4">
        <span className="text-lg font-bold">메시지</span>
        <button aria-label="새 채팅방" onClick={() => setShowNewChat(true)} className="w-8 h-8 rounded-full border border-[#6e6e6e] flex items-center justify-center"><Plus size={18} /></button>
      </div>
      {error && <p className="px-7 pb-2 text-xs text-red-500">{error}</p>}
      <div className="flex-1 overflow-y-auto">
        {conversations.map(chat => {
          const peer = directPeer(chat);
          return <button key={chat.id} onClick={() => navigate(`/messages/${chat.id}`)} className="w-full flex items-center gap-3 px-7 py-3 border-b border-gray-100 text-left">
            <Avatar friendId={peer?.id ?? chat.id} src={peer?.avatarUrl} className="w-12 h-12" />
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">{chat.name}</span>
                {chat.lastMessage && <span className="text-xs text-gray-400">{formatRelativeTime(chat.lastMessage.createdAt)}</span>}
              </div>
              <p className="text-sm text-gray-500 truncate">{chat.lastMessage?.content ?? (chat.lastMessage?.mediaUrl ? '사진을 보냈어요' : '아직 대화가 없어요')}</p>
            </div>
          </button>;
        })}
      </div>
      <AppNavigationBar />
      {showNewChat && <NewChatModal friends={friends} onCreate={handleCreateChat} onClose={() => setShowNewChat(false)} />}
    </div>
  );
}
