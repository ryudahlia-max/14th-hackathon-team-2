import { useState } from 'react';
import { X, Check } from 'lucide-react';
import { FRIENDS } from '../data/mockData';

interface Props {
  onCreate: (friendIds: string[], groupName: string) => void;
  onClose: () => void;
}

export default function NewChatModal({ onCreate, onClose }: Props) {
  const [selected, setSelected] = useState<string[]>([]);
  const [groupName, setGroupName] = useState('');

  function toggleFriend(id: string) {
    setSelected(prev => (prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]));
  }

  function handleSubmit() {
    if (selected.length === 0) return;
    onCreate(selected, groupName.trim());
    onClose();
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
            {FRIENDS.map(friend => {
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
                  <div className="w-9 h-9 rounded-full bg-gray-300 shrink-0" />
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
          onClick={handleSubmit}
          disabled={selected.length === 0}
          className="w-full py-3 rounded-xl bg-[#a2bfff] text-white font-semibold text-sm disabled:opacity-40"
        >
          만들기
        </button>
      </div>
    </div>
  );
}
