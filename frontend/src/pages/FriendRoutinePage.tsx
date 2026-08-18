import { useState } from 'react';
import { Sun, Heart, Smile } from 'lucide-react';
import AppNavigationBar from '../components/AppNavigationBar';

type NotificationTab = 'friendRoutine' | 'receivedLikes';
type Reaction = 'heart' | 'smile';

interface RoutineEntry {
  name: string;
  photoUrl?: string;
}

interface RoutineGroup {
  name: string;
  count: number;
  timeAgo: string;
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

const GROUPS: RoutineGroup[] = [
  {
    name: '연진',
    count: 5,
    timeAgo: '1시간 전',
    routines: [
      { name: '물 마시기' },
      { name: '물 마시기' },
      { name: '물 마시기' },
      { name: '선크림 바르기' },
      { name: '영양제먹기' },
    ],
  },
  {
    name: '쪙',
    count: 3,
    timeAgo: '1시간 전',
    routines: [
      { name: '물 마시기' },
      { name: '선크림 바르기' },
      { name: '영양제먹기' },
    ],
  },
  {
    name: '현정',
    count: 9,
    timeAgo: '1시간 전',
    routines: [
      { name: '물 마시기', photoUrl: 'https://example.com/photo.jpg' },
      { name: '물 마시기' },
      { name: '물 마시기' },
      { name: '선크림 바르기' },
      { name: '선크림 바르기' },
      { name: '선크림 바르기' },
      { name: '영양제먹기' },
      { name: '영양제먹기' },
      { name: '영양제먹기' },
    ],
  },
];

function NotificationGroup({ name, count, timeAgo, routines }: RoutineGroup) {
  return (
    <div className="flex flex-col w-full">
      <div className="flex items-center gap-1.5 w-full">
        <div className="size-8 rounded-full bg-gray-300 shrink-0" />
        <p className="text-xs text-black">
          <span className="font-bold">{name}</span>님이 루틴을{' '}
          <span className="font-bold">{count}회</span> 완료했습니다.{' '}
          <span className="text-[#8b8b8b]">{timeAgo}</span>
        </p>
      </div>
      <div className="flex flex-col w-full border-l border-[#6e6e6e] ml-12">
        {routines.map((routine, i) => (
          <div key={i} className="flex flex-col gap-1 w-full">
            <div className="flex items-center justify-between pl-2 pr-4 w-full">
              <p className="text-sm text-black">{routine.name}</p>
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
        <p className="text-xs text-black">
          <span className="font-bold">{name}</span>님이 내 루틴에 공감을 남겼습니다.{' '}
          <span className="text-[#8b8b8b]">{timeAgo}</span>
        </p>
      </div>
      <div className="flex flex-col items-center gap-1 w-full border-l border-[#6e6e6e] ml-12">
        {routines.map((routine, i) => {
          const Icon = REACTION_ICONS[routine.reaction];
          return (
            <div key={i} className="flex items-center justify-between pl-2 pr-4 w-full">
              <p className="text-sm text-black">{routine.name}</p>
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

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex-1 overflow-y-auto flex w-full flex-col items-start gap-4 px-4 pt-8">
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
          {tab === 'friendRoutine'
            ? GROUPS.map((group) => <NotificationGroup key={group.name} {...group} />)
            : LIKE_GROUPS.map((group) => (
                <ReceivedLikeGroupItem key={group.name} {...group} />
              ))}
        </div>
      </div>
      <AppNavigationBar />
    </div>
  );
}