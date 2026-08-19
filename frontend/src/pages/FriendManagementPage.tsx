import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Plus } from 'lucide-react';
import InviteFriendModal from '../components/InviteFriendModal';
import CreateGroupModal from '../components/CreateGroupModal';
import ConfirmModal from '../components/ConfirmModal';
import { getFriends, getGroups, removeFriend } from '../api/friend';
import type { Friend, Group } from '../types/friend';
import Avatar from '../components/Avatar';

type ManagementTab = 'friends' | 'groups';

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

function FriendRow({
  friend,
  onDelete,
}: {
  friend: Friend;
  onDelete: (friend: Friend) => void;
}) {
  return (
    <div className="flex w-full items-center gap-3">
      <Avatar friendId={friend.id} src={friend.avatarUrl} className="size-12" />
      <p className="flex-1 truncate text-sm text-black">{friend.name}</p>
      <div className="flex shrink-0 gap-2">
        <ActionButton label="삭제" onClick={() => onDelete(friend)} />
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
  const [showCreateGroupModal, setShowCreateGroupModal] = useState(false);

  const [friends, setFriends] = useState<Friend[]>([]);
  const [isLoadingFriends, setIsLoadingFriends] = useState(true);
  const [friendsError, setFriendsError] = useState<string | null>(null);

  const [groups, setGroups] = useState<Group[]>([]);
  const [isLoadingGroups, setIsLoadingGroups] = useState(true);
  const [groupsError, setGroupsError] = useState<string | null>(null);

  const [pendingDeleteFriend, setPendingDeleteFriend] = useState<Friend | null>(null);
  const [isActionRunning, setIsActionRunning] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

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

  const loadGroups = useCallback(async () => {
    setIsLoadingGroups(true);
    setGroupsError(null);
    try {
      const result = await getGroups();
      setGroups(result);
    } catch (err) {
      console.error('그룹 목록을 불러오지 못했습니다.', err);
      setGroupsError('그룹 목록을 불러오지 못했습니다.');
    } finally {
      setIsLoadingGroups(false);
    }
  }, []);

  useEffect(() => {
    loadFriends();
    loadGroups();
  }, [loadFriends, loadGroups]);

  function closeActionModal() {
    setPendingDeleteFriend(null);
    setActionError(null);
  }

  async function handleConfirmDelete() {
    if (!pendingDeleteFriend) return;
    setIsActionRunning(true);
    setActionError(null);
    try {
      await removeFriend(pendingDeleteFriend.id);
      setFriends((prev) => prev.filter((f) => f.id !== pendingDeleteFriend.id));
      setPendingDeleteFriend(null);
    } catch (err) {
      console.error('친구 삭제에 실패했습니다.', err);
      setActionError(err instanceof Error ? err.message : '삭제에 실패했습니다.');
    } finally {
      setIsActionRunning(false);
    }
  }

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
                  <FriendRow
                    key={friend.id}
                    friend={friend}
                    onDelete={(f) => setPendingDeleteFriend(f)}
                  />
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
        ) : isLoadingGroups ? (
          <p className="w-full py-12 text-center text-sm text-[#8b8b8b]">불러오는 중...</p>
        ) : groupsError ? (
          <p className="w-full py-12 text-center text-sm text-red-500">{groupsError}</p>
        ) : groups.length > 0 ? (
          <div className="flex w-full flex-col gap-4">
            <AddButton label="그룹 만들기" onClick={() => setShowCreateGroupModal(true)} />
            <div className="flex w-full flex-col gap-4">
              {groups.map((group) => (
                <GroupRow key={group.id} group={group} />
              ))}
            </div>
          </div>
        ) : (
          <EmptyState
            message="아직 그룹이 없어요"
            actionLabel="그룹 만들기"
            onAction={() => setShowCreateGroupModal(true)}
          />
        )}
      </div>

      {showInviteModal && (
        <InviteFriendModal onClose={() => setShowInviteModal(false)} onAccepted={loadFriends} />
      )}

      {showCreateGroupModal && (
        <CreateGroupModal
          friends={friends}
          onClose={() => setShowCreateGroupModal(false)}
          onCreated={loadGroups}
        />
      )}

      {pendingDeleteFriend && (
        <ConfirmModal
          title="친구 삭제"
          description={`${pendingDeleteFriend.name}님을 친구 목록에서 삭제하시겠습니까?`}
          confirmLabel={isActionRunning ? '삭제 중...' : '삭제'}
          confirmDisabled={isActionRunning}
          onCancel={closeActionModal}
          onConfirm={handleConfirmDelete}
        >
          {actionError && <p className="text-sm text-red-500">{actionError}</p>}
        </ConfirmModal>
      )}
    </div>
  );
}
