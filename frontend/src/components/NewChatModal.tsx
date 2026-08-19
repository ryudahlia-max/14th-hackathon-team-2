import { useState } from 'react';
import { X, Check } from 'lucide-react';
import type { Friend } from '../types/friend';
import Avatar from './Avatar';

interface Props {
  friends: Friend[];
  onCreate: (friendIds: string[], groupName: string) => Promise<void>;
  onClose: () => void;
}

export default function NewChatModal({ friends, onCreate, onClose }: Props) {
  const [selected, setSelected] = useState<string[]>([]);
  const [groupName, setGroupName] = useState('');
  const [submitting, setSubmitting] = useState(false);

  function toggleFriend(id: string) {
    setSelected(prev => (prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]));
  }

  async function handleSubmit() {
    if (selected.length === 0) return;
    setSubmitting(true);
    try {
      await onCreate(selected, groupName.trim());
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-white rounded-t-2xl w-full max-w-[393px] px-6 pt-6 pb-10">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold">새 채팅방</h2>
          <button onClick={onClose} className="p-1">
            <X size={20} color="#6b7280" />
          </button>
        </div>

        <div className="mb-5">
          <label className="text-sm text-gray-500 mb-1.5 block">친구 선택</label>
          <div className="space-y-2">
            {friends.map(friend => {
              const checked = selected.includes(friend.id);
              return (
                <button
                  key={friend.id}
                  onClick={() => toggleFriend(friend.id)}
                  className="w-full flex items-center gap-3 px-1 py-2"
                >
                  <div
                    className="w-5 h-5 rounded border-2 flex items-center justify-center shrink-0"
                    style={{
                      borderColor: '#a2bfff',
                      background: checked ? '#a2bfff' : 'white',
                    }}
                  >
                    {checked && <Check size={11} color="white" strokeWidth={3} />}
                  </div>
                  <Avatar friendId={friend.id} src={friend.avatarUrl} className="w-9 h-9" />
                  <span className="text-sm">{friend.name}</span>
                </button>
              );
            })}
          </div>
        </div>

        {selected.length > 1 && (
          <div className="mb-5">
            <label className="text-sm text-gray-500 mb-1.5 block">채팅방 이름 (선택)</label>
            <input
              value={groupName}
              onChange={e => setGroupName(e.target.value)}
              placeholder="입력하지 않으면 참여자 이름으로 표시돼요"
              className="w-full border border-gray-300 rounded-xl px-4 py-3 text-sm outline-none focus:border-[#a2bfff]"
            />
          </div>
        )}

        <button
          onClick={() => void handleSubmit()}
          disabled={selected.length === 0 || submitting}
          className="w-full py-3 rounded-xl bg-[#a2bfff] text-white font-semibold text-sm disabled:opacity-40"
        >
          {submitting ? '만드는 중...' : '만들기'}
        </button>
      </div>
    </div>
  );
}
