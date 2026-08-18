import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import AppNavigationBar from '../components/AppNavigationBar';
import Avatar from '../components/Avatar';
import NewChatModal from '../components/NewChatModal';
import { FRIENDS, ME_ID } from '../data/mockData';
import { createGroupChat, getChats, getLastMessage, getOrCreateDirectChat } from '../data/chatStore';
import { formatRelativeTime } from '../utils/formatRelativeTime';
import { useAutoRefresh } from '../utils/useAutoRefresh';

export default function MessagesPage() {
  const navigate = useNavigate();
  const [chats, setChats] = useState(() => getChats());
  const [showNewChat, setShowNewChat] = useState(false);
  useAutoRefresh(30000);

  const conversations = chats
    .map(chat => ({ chat, lastMessage: getLastMessage(chat.id) }))
    .sort((a, b) => {
      if (!a.lastMessage) return 1;
      if (!b.lastMessage) return -1;
      return b.lastMessage.sentAt.localeCompare(a.lastMessage.sentAt);
    });

  function handleCreateChat(friendIds: string[], groupName: string) {
    let chatId: string;
    if (friendIds.length === 1 && !groupName) {
      chatId = getOrCreateDirectChat(friendIds[0]);
    } else {
      const me = FRIENDS.find(f => f.id === ME_ID)?.name;
      const fallbackName = [me, ...friendIds.map(id => FRIENDS.find(f => f.id === id)?.name)]
        .filter(Boolean)
        .join(', ');
      chatId = createGroupChat(groupName || fallbackName, friendIds);
    }
    setChats(getChats());
    navigate(`/messages/${chatId}`);
  }

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex items-center justify-between px-7 pt-4 pb-4">
        <span className="text-lg font-bold">메시지</span>
        <button
          aria-label="새 채팅방"
          onClick={() => setShowNewChat(true)}
          className="w-8 h-8 rounded-full border border-[#6e6e6e] flex items-center justify-center"
        >
          <Plus size={18} color="#333" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {conversations.map(({ chat, lastMessage }) => (
          <button
            key={chat.id}
            onClick={() => navigate(`/messages/${chat.id}`)}
            className="w-full flex items-center gap-3 px-7 py-3 border-b border-gray-100 text-left"
          >
            <Avatar friendId={chat.isGroup ? '' : chat.participantIds[0]} className="w-12 h-12" />
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">{chat.name}</span>
                {lastMessage && (
                  <span className="text-xs text-gray-400 shrink-0">{formatRelativeTime(lastMessage.sentAt)}</span>
                )}
              </div>
              <p className="text-sm text-gray-500 truncate">
                {lastMessage ? (lastMessage.text ?? '사진을 보냈어요') : '아직 대화가 없어요'}
              </p>
            </div>
          </button>
        ))}
      </div>

      <AppNavigationBar />

      {showNewChat && (
        <NewChatModal onCreate={handleCreateChat} onClose={() => setShowNewChat(false)} />
      )}
    </div>
  );
}
