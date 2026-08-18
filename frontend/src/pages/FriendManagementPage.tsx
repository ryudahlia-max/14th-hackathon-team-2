import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Plus } from 'lucide-react';
import InviteFriendModal from '../components/InviteFriendModal';
import { getFriends } from '../api/friend';
import type { Friend, Group } from '../types/friend';

type ManagementTab = 'friends' | 'groups';

const MOCK_GROUPS: Group[] = [
  { id: 'g-1', name: '아침 루틴 클럽', memberCount: 3, maxMember: 10 },
  { id: 'g-2', name: '물 마시기 챌린지', memberCount: 5, maxMember: 8 },
];

function AddButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center gap-1 self-end rounded-full bg-[#a2bfff] px-4 py-2 text-sm font-medium text-white"
    >
      <Plus size={16} strokeWidth={2} />
      {label}
    </button>
  );
}

function ActionButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="rounded-full border border-[#6e6e6e] px-3 py-1 text-xs text-black"
    >
      {label}
    </button>
  );
}

function FriendRow({ friend }: { friend: Friend }) {
  return (
    <div className="flex w-full items-center gap-3">
      <div className="size-12 shrink-0 rounded-full bg-gray-300" />
      <p className="flex-1 truncate text-sm text-black">{friend.name}</p>
      <div className="flex shrink-0 gap-2">
        <ActionButton label="차단" onClick={() => console.log('친구 차단', friend.id)} />
        <ActionButton label="삭제" onClick={() => console.log('친구 삭제', friend.id)} />
      </div>
    </div>
  );
}

function GroupRow({ group }: { group: Group }) {
  return (
    <div className="flex w-full items-center gap-3">
      <div className="size-12 shrink-0 rounded-full bg-gray-300" />
      <div className="flex flex-1 flex-col gap-0.5 truncate">
        <p className="truncate text-sm text-black">{group.name}</p>
        <p className="text-xs text-[#8b8b8b]">
          {group.memberCount}/{group.maxMember}명
        </p>
      </div>
      <div className="flex shrink-0 gap-2">
        <ActionButton label="관리" onClick={() => console.log('그룹 관리', group.id)} />
        <ActionButton label="나가기" onClick={() => console.log('그룹 나가기', group.id)} />
      </div>
    </div>
  );
}

function EmptyState({ message, actionLabel, onAction }: { message: string; actionLabel: string; onAction: () => void }) {
  return (
    <div className="flex w-full flex-col items-center gap-4 py-12 text-center">
      <p className="text-sm text-[#8b8b8b]">{message}</p>
      <AddButton label={actionLabel} onClick={onAction} />
    </div>
  );
}

export default function FriendManagementPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<ManagementTab>('friends');
  const [showInviteModal, setShowInviteModal] = useState(false);

  const [friends, setFriends] = useState<Friend[]>([]);
  const [isLoadingFriends, setIsLoadingFriends] = useState(true);
  const [friendsError, setFriendsError] = useState<string | null>(null);

  const loadFriends = useCallback(async () => {
    setIsLoadingFriends(true);
    setFriendsError(null);
    try {
      const result = await getFriends();
      setFriends(result);
    } catch (err) {
      console.error('친구 목록을 불러오지 못했습니다.', err);
      setFriendsError('친구 목록을 불러오지 못했습니다.');
    } finally {
      setIsLoadingFriends(false);
    }
  }, []);

  useEffect(() => {
    loadFriends();
  }, [loadFriends]);

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex items-center gap-3 px-4 pt-8 pb-4">
        <button type="button" onClick={() => navigate('/profile')} aria-label="뒤로">
          <ChevronLeft size={22} color="#333" />
        </button>
        <span className="text-base font-bold">친구 관리</span>
      </div>

      <div className="flex-1 overflow-y-auto flex w-full flex-col items-start gap-4 px-4 pb-8">
        <div className="flex gap-2.5 items-center">
          <button
            type="button"
            onClick={() => setTab('friends')}
            className={`h-8 px-4 rounded-full border border-[#6e6e6e] text-sm ${
              tab === 'friends' ? 'bg-[#a2bfff] text-white' : 'bg-white text-black'
            }`}
          >
            친구
          </button>
          <button
            type="button"
            onClick={() => setTab('groups')}
            className={`h-8 px-4 rounded-full border border-[#6e6e6e] text-sm ${
              tab === 'groups' ? 'bg-[#a2bfff] text-white' : 'bg-white text-black'
            }`}
          >
            그룹
          </button>
        </div>

        {tab === 'friends' ? (
          isLoadingFriends ? (
            <p className="w-full py-12 text-center text-sm text-[#8b8b8b]">불러오는 중...</p>
          ) : friendsError ? (
            <p className="w-full py-12 text-center text-sm text-red-500">{friendsError}</p>
          ) : friends.length > 0 ? (
            <div className="flex w-full flex-col gap-4">
              <AddButton label="친구 추가" onClick={() => setShowInviteModal(true)} />
              <div className="flex w-full flex-col gap-4">
                {friends.map((friend) => (
                  <FriendRow key={friend.id} friend={friend} />
                ))}
              </div>
            </div>
          ) : (
            <EmptyState
              message="아직 친구가 없어요"
              actionLabel="친구 추가"
              onAction={() => setShowInviteModal(true)}
            />
          )
        ) : MOCK_GROUPS.length > 0 ? (
          <div className="flex w-full flex-col gap-4">
            <AddButton label="그룹 만들기" onClick={() => console.log('그룹 만들기')} />
            <div className="flex w-full flex-col gap-4">
              {MOCK_GROUPS.map((group) => (
                <GroupRow key={group.id} group={group} />
              ))}
            </div>
          </div>
        ) : (
          <EmptyState
            message="아직 그룹이 없어요"
            actionLabel="그룹 만들기"
            onAction={() => console.log('그룹 만들기')}
          />
        )}
      </div>

      {showInviteModal && (
        <InviteFriendModal onClose={() => setShowInviteModal(false)} onAccepted={loadFriends} />
      )}
    </div>
  );
}
