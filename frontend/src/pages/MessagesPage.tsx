import AppNavigationBar from '../components/AppNavigationBar';
import { FRIENDS, MESSAGES } from '../data/mockData';
import type { Message } from '../types';

function getLastMessage(friendId: string): Message | null {
  const msgs = MESSAGES.filter(m => m.friendId === friendId);
  if (msgs.length === 0) return null;
  return msgs.reduce((latest, m) => (m.sentAt > latest.sentAt ? m : latest));
}

function formatDate(sentAt: string) {
  const d = new Date(sentAt);
  return `${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
}

export default function MessagesPage() {
  const conversations = FRIENDS.map(friend => ({
    friend,
    lastMessage: getLastMessage(friend.id),
  })).sort((a, b) => {
    if (!a.lastMessage) return 1;
    if (!b.lastMessage) return -1;
    return b.lastMessage.sentAt.localeCompare(a.lastMessage.sentAt);
  });

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-4 pt-8 pb-4">
        <span className="text-lg font-bold">메시지</span>
      </div>

      <div className="flex-1 overflow-y-auto">
        {conversations.map(({ friend, lastMessage }) => (
          <button
            key={friend.id}
            className="w-full flex items-center gap-3 px-4 py-3 border-b border-gray-100 text-left"
          >
            <div className="w-12 h-12 rounded-full bg-gray-300 overflow-hidden shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">{friend.name}</span>
                {lastMessage && (
                  <span className="text-xs text-gray-400 shrink-0">{formatDate(lastMessage.sentAt)}</span>
                )}
              </div>
              <p className="text-sm text-gray-500 truncate">
                {lastMessage ? lastMessage.text : '아직 대화가 없어요'}
              </p>
            </div>
          </button>
        ))}
      </div>

      <AppNavigationBar />
    </div>
  );
}
