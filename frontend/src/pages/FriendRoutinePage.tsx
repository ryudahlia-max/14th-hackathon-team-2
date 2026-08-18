import { useEffect, useRef, useState } from 'react';
import { Sun, Heart, Frown, ThumbsUp, Flame, Smile } from 'lucide-react';
import AppNavigationBar from '../components/AppNavigationBar';
import Avatar from '../components/Avatar';
import { getNotifications } from '../data/notificationStore';
import { ME_ID } from '../data/mockData';
import { formatRelativeTime } from '../utils/formatRelativeTime';
import { useAutoRefresh } from '../utils/useAutoRefresh';
import type { RoutineCompletionNotification } from '../types';
import type { Reaction } from '../data/notificationReactions';

type NotificationTab = 'friendRoutine' | 'receivedLikes';

interface RoutineEntry {
  id: string;
  name: string;
  photoUrl?: string;
}

interface RoutineGroup {
  friendId: string;
  name: string;
  count: number;
  latestAt: string; // ISO timestamp, formatted at render time so it stays live
  routines: RoutineEntry[];
}

interface LikedRoutine {
  name: string;
  reaction: Reaction;
}

interface ReceivedLikeGroup {
  name: string;
  timeAgo: string;
  routines: LikedRoutine[];
}

const LIKE_GROUPS: ReceivedLikeGroup[] = [
  {
    name: '연진',
    timeAgo: '1시간 전',
    routines: [
      { name: '물 마시기', reaction: 'heart' },
      { name: '물 마시기', reaction: 'smile' },
    ],
  },
];

const REACTION_ORDER: Reaction[] = ['heart', 'sad', 'thumbsUp', 'fire', 'smile'];

const REACTION_ICONS: Record<Reaction, typeof Heart> = {
  heart: Heart,
  sad: Frown,
  thumbsUp: ThumbsUp,
  fire: Flame,
  smile: Smile,
};

const REACTION_COLORS: Record<Reaction, string> = {
  heart: 'text-red-500',
  sad: 'text-yellow-500',
  thumbsUp: 'text-blue-500',
  fire: 'text-orange-500',
  smile: 'text-yellow-500',
};

function buildRoutineGroups(): RoutineGroup[] {
  const byFriend = new Map<string, RoutineCompletionNotification[]>();
  for (const n of getNotifications()) {
    if (n.friendId === ME_ID) continue; // 내가 완료한 루틴은 알림 대상이 아님
    const list = byFriend.get(n.friendId) ?? [];
    list.push(n);
    byFriend.set(n.friendId, list);
  }

  return Array.from(byFriend.values())
    .map(list => [...list].sort((a, b) => a.completedAt.localeCompare(b.completedAt)))
    .map(sorted => {
      const latest = sorted[sorted.length - 1];
      return {
        friendId: latest.friendId,
        name: latest.friendName,
        count: sorted.length,
        latestAt: latest.completedAt,
        routines: sorted.map(n => ({ id: n.id, name: n.routineName })),
      };
    })
    .sort((a, b) => b.latestAt.localeCompare(a.latestAt));
}

interface RoutineRowProps {
  routine: RoutineEntry;
  reaction: Reaction | undefined;
  isPopoverOpen: boolean;
  onToggle: () => void;
  onSelect: (reaction: Reaction) => void;
  onClose: () => void;
}

function RoutineRow({ routine, reaction, isPopoverOpen, onToggle, onSelect, onClose }: RoutineRowProps) {
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isPopoverOpen) return;
    function handlePointerDown(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        onClose();
      }
    }
    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, [isPopoverOpen, onClose]);

  const CurrentIcon = reaction ? REACTION_ICONS[reaction] : Sun;

  return (
    <div className="flex flex-col gap-1 w-full">
      <div className="flex items-center justify-between gap-2 pl-2 pr-4 w-full">
        <p className="text-sm text-black flex-1 min-w-0 truncate">{routine.name}</p>
        <div ref={wrapperRef} className="relative shrink-0">
          <button
            type="button"
            onClick={onToggle}
            aria-label="반응 남기기"
            className="flex items-center justify-center p-1"
          >
            <CurrentIcon
              size={16}
              strokeWidth={1.5}
              className={reaction ? REACTION_COLORS[reaction] : undefined}
              color={reaction ? undefined : '#6e6e6e'}
              fill={reaction === 'heart' ? 'currentColor' : 'none'}
            />
          </button>
          {isPopoverOpen && (
            <div className="absolute right-0 bottom-full z-10 mb-2 flex items-center gap-1 rounded-full border border-gray-200 bg-white px-2 py-1.5 shadow-lg">
              {REACTION_ORDER.map((r) => {
                const Icon = REACTION_ICONS[r];
                const isSelected = r === reaction;
                return (
                  <button
                    key={r}
                    type="button"
                    onClick={() => onSelect(r)}
                    aria-label={r}
                    aria-pressed={isSelected}
                    className={`flex items-center justify-center rounded-full p-1.5 hover:bg-gray-100 ${
                      isSelected ? 'bg-gray-100 ring-2 ring-gray-300' : ''
                    }`}
                  >
                    <Icon
                      size={16}
                      strokeWidth={1.5}
                      className={REACTION_COLORS[r]}
                      fill={r === 'heart' ? 'currentColor' : 'none'}
                    />
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>
      {routine.photoUrl && (
        <div className="flex items-center gap-2 pl-2">
          <img
            src={routine.photoUrl}
            alt="인증 사진"
            className="size-16 rounded-lg object-cover bg-gray-200"
          />
        </div>
      )}
    </div>
  );
}

interface NotificationGroupProps extends RoutineGroup {
  reactions: Record<string, Reaction>;
  openReactionKey: string | null;
  onToggleReaction: (key: string) => void;
  onSelectReaction: (key: string, reaction: Reaction) => void;
  onCloseReaction: () => void;
}

function NotificationGroup({
  friendId,
  name,
  count,
  latestAt,
  routines,
  reactions,
  openReactionKey,
  onToggleReaction,
  onSelectReaction,
  onCloseReaction,
}: NotificationGroupProps) {
  return (
    <div className="flex flex-col w-full">
      <div className="flex items-center gap-1.5 w-full">
        <Avatar friendId={friendId} className="size-8" />
        <p className="text-xs text-black flex-1 min-w-0">
          <span className="font-bold">{name}</span>님이 루틴을{' '}
          <span className="font-bold">{count}회</span> 완료했습니다.{' '}
          <span className="text-[#8b8b8b]">{formatRelativeTime(latestAt)}</span>
        </p>
      </div>
      <div className="flex flex-col border-l border-[#6e6e6e] ml-12">
        {routines.map((routine) => (
          <RoutineRow
            key={routine.id}
            routine={routine}
            reaction={reactions[routine.id]}
            isPopoverOpen={openReactionKey === routine.id}
            onToggle={() => onToggleReaction(routine.id)}
            onSelect={(r) => onSelectReaction(routine.id, r)}
            onClose={onCloseReaction}
          />
        ))}
      </div>
    </div>
  );
}

function ReceivedLikeGroupItem({ name, timeAgo, routines }: ReceivedLikeGroup) {
  return (
    <div className="flex flex-col w-full">
      <div className="flex items-center gap-1.5 w-full">
        <div className="size-8 rounded-full bg-gray-300 shrink-0" />
        <p className="text-xs text-black flex-1 min-w-0">
          <span className="font-bold">{name}</span>님이 내 루틴에 공감을 남겼습니다.{' '}
          <span className="text-[#8b8b8b]">{timeAgo}</span>
        </p>
      </div>
      <div className="flex flex-col items-center gap-1 border-l border-[#6e6e6e] ml-12">
        {routines.map((routine, i) => {
          const Icon = REACTION_ICONS[routine.reaction];
          return (
            <div key={i} className="flex items-center justify-between gap-2 pl-2 pr-4 w-full">
              <p className="text-sm text-black flex-1 min-w-0 truncate">{routine.name}</p>
              <Icon size={16} strokeWidth={1.5} color="#6e6e6e" />
            </div>
          );
        })}
      </div>
    </div>
  );
}

interface NotificationPageProps {
  reactions: Record<string, Reaction>;
  onSelectReaction: (key: string, reaction: Reaction) => void;
}

export default function NotificationPage({ reactions, onSelectReaction }: NotificationPageProps) {
  const [tab, setTab] = useState<NotificationTab>('friendRoutine');
  const [groups] = useState<RoutineGroup[]>(buildRoutineGroups);
  useAutoRefresh(30000);

  const [openReactionKey, setOpenReactionKey] = useState<string | null>(null);

  function handleToggleReaction(key: string) {
    setOpenReactionKey((prev) => (prev === key ? null : key));
  }

  function handleCloseReaction() {
    setOpenReactionKey(null);
  }

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex-1 overflow-y-auto flex w-full flex-col items-start gap-4 pl-7 pr-4 pt-4">
        <p className="text-lg font-bold text-black">알림</p>
        <div className="flex gap-2.5 items-center">
          <button
            onClick={() => setTab('friendRoutine')}
            className={`h-8 px-4 rounded-full border border-[#6e6e6e] text-sm ${
              tab === 'friendRoutine' ? 'bg-[#a2bfff] text-white' : 'bg-white text-black'
            }`}
          >
            친구의 루틴
          </button>
          <button
            onClick={() => setTab('receivedLikes')}
            className={`h-8 px-4 rounded-full border border-[#6e6e6e] text-sm ${
              tab === 'receivedLikes' ? 'bg-[#a2bfff] text-white' : 'bg-white text-black'
            }`}
          >
            받은 공감
          </button>
        </div>
        <div className="flex flex-col gap-5 w-full">
          {tab === 'friendRoutine' ? (
            groups.length === 0 ? (
              <p className="text-sm text-gray-400">아직 알림이 없어요</p>
            ) : (
              groups.map((group) => (
                <NotificationGroup
                  key={group.friendId}
                  {...group}
                  reactions={reactions}
                  openReactionKey={openReactionKey}
                  onToggleReaction={handleToggleReaction}
                  onSelectReaction={onSelectReaction}
                  onCloseReaction={handleCloseReaction}
                />
              ))
            )
          ) : (
            LIKE_GROUPS.map((group) => (
              <ReceivedLikeGroupItem key={group.name} {...group} />
            ))
          )}
        </div>
      </div>
      <AppNavigationBar />
    </div>
  );
}
