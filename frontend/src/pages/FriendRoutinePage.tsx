import { useState } from 'react';
import { Sun, Heart, Smile } from 'lucide-react';
import AppNavigationBar from '../components/AppNavigationBar';
import Avatar from '../components/Avatar';
import { getNotifications } from '../data/notificationStore';
import { ME_ID } from '../data/mockData';
import { formatRelativeTime } from '../utils/formatRelativeTime';
import { useAutoRefresh } from '../utils/useAutoRefresh';
import type { RoutineCompletionNotification } from '../types';

type NotificationTab = 'friendRoutine' | 'receivedLikes';
type Reaction = 'heart' | 'smile';

interface RoutineEntry {
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

const REACTION_ICONS: Record<Reaction, typeof Heart> = {
  heart: Heart,
  smile: Smile,
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
        routines: sorted.map(n => ({ name: n.routineName })),
      };
    })
    .sort((a, b) => b.latestAt.localeCompare(a.latestAt));
}

function NotificationGroup({ friendId, name, count, latestAt, routines }: RoutineGroup) {
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
        {routines.map((routine, i) => (
          <div key={i} className="flex flex-col gap-1 w-full">
            <div className="flex items-center justify-between gap-2 pl-2 pr-4 w-full">
              <p className="text-sm text-black flex-1 min-w-0 truncate">{routine.name}</p>
              <Sun size={16} strokeWidth={1.5} color="#6e6e6e" />
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

export default function NotificationPage() {
  const [tab, setTab] = useState<NotificationTab>('friendRoutine');
  const [groups] = useState<RoutineGroup[]>(buildRoutineGroups);
  useAutoRefresh(30000);

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex-1 overflow-y-auto flex w-full flex-col items-start gap-4 px-7 pt-4">
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
              groups.map((group) => <NotificationGroup key={group.name} {...group} />)
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