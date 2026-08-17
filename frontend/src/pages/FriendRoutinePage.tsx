import { useNavigate } from 'react-router-dom';
import { MessageCircle } from 'lucide-react';
import AppNavigationBar from '../components/AppNavigationBar';
import { FRIENDS } from '../data/mockData';
import { getOrCreateDirectChat } from '../data/chatStore';

export default function FriendRoutinePage() {
  const navigate = useNavigate();

  function handleMessageFriend(friendId: string) {
    const chatId = getOrCreateDirectChat(friendId);
    navigate(`/messages/${chatId}`);
  }

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-4 pt-8 pb-2 text-sm text-gray-400">
        알림 페이지 준비 중 (feat/notification-page 병합 후 교체 예정)
      </div>

      <div className="flex-1 overflow-y-auto">
        {FRIENDS.map(friend => (
          <div key={friend.id} className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-gray-300 shrink-0" />
              <span className="text-sm font-medium">{friend.name}</span>
            </div>
            <button
              onClick={() => handleMessageFriend(friend.id)}
              aria-label={`${friend.name}에게 메시지 보내기`}
              className="w-9 h-9 rounded-full border border-[#6e6e6e] flex items-center justify-center"
            >
              <MessageCircle size={16} color="#333" />
            </button>
          </div>
        ))}
      </div>

      <AppNavigationBar />
    </div>
  );
}
