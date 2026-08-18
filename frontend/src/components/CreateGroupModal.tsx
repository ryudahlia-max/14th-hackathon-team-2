import { useEffect, useRef, useState } from 'react';
import { createGroup, GROUP_MAX_INVITE_MEMBERS } from '../api/friend';
import type { Friend } from '../types/friend';

interface CreateGroupModalProps {
  friends: Friend[];
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateGroupModal({ friends, onClose, onCreated }: CreateGroupModalProps) {
  const contentRef = useRef<HTMLDivElement>(null);

  const [name, setName] = useState('');
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    function handlePointerDown(e: MouseEvent) {
      if (contentRef.current && !contentRef.current.contains(e.target as Node)) {
        onClose();
      }
    }
    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, [onClose]);

  const atMax = selectedIds.length >= GROUP_MAX_INVITE_MEMBERS;

  function toggleMember(id: string) {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : atMax ? prev : [...prev, id]
    );
  }

  async function handleSubmit() {
    const trimmed = name.trim();
    if (!trimmed) return;
    setIsSubmitting(true);
    setError(null);
    try {
      await createGroup(trimmed, selectedIds);
      onCreated();
      onClose();
    } catch (err) {
      console.error('그룹 생성에 실패했습니다.', err);
      setError(err instanceof Error ? err.message : '그룹 생성에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-6">
      <div ref={contentRef} className="w-full max-w-xs rounded-2xl bg-white p-6 shadow-lg">
        <p className="text-base font-bold text-black">그룹 만들기</p>

        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="그룹 이름"
          disabled={isSubmitting}
          className="mt-4 w-full rounded-full border border-[#6e6e6e] px-4 py-2 text-sm text-black"
        />

        <div className="mt-4 flex items-center justify-between">
          <p className="text-sm font-bold text-black">멤버 선택</p>
          <p className="text-xs text-[#8b8b8b]">
            {selectedIds.length}/{GROUP_MAX_INVITE_MEMBERS}명
          </p>
        </div>

        <div className="mt-2 flex max-h-48 flex-col gap-2 overflow-y-auto">
          {friends.length === 0 ? (
            <p className="text-xs text-[#8b8b8b]">초대할 친구가 없어요</p>
          ) : (
            friends.map((friend) => {
              const checked = selectedIds.includes(friend.id);
              return (
                <label key={friend.id} className="flex items-center gap-2 text-sm text-black">
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() => toggleMember(friend.id)}
                    disabled={isSubmitting || (!checked && atMax)}
                    className="size-4"
                  />
                  {friend.name}
                </label>
              );
            })
          )}
        </div>

        {error && <p className="mt-2 text-xs text-red-500">{error}</p>}

        <div className="mt-6 flex gap-3">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 rounded-full border border-[#6e6e6e] py-2 text-sm text-black"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={isSubmitting || !name.trim()}
            className="flex-1 rounded-full bg-[#a2bfff] py-2 text-sm text-white disabled:bg-gray-200 disabled:text-[#8b8b8b]"
          >
            {isSubmitting ? '만드는 중...' : '만들기'}
          </button>
        </div>
      </div>
    </div>
  );
}
