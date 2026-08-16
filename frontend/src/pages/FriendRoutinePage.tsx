import { useState } from 'react';
import { Sun } from 'lucide-react';

type NotificationTab = 'friendRoutine' | 'receivedLikes';

interface RoutineGroup {
  name: string;
  count: number;
  timeAgo: string;
  routines: string[];
}

const GROUPS: RoutineGroup[] = [
  {
    name: '연진',
    count: 5,
    timeAgo: '1시간 전',
    routines: ['물 마시기', '물 마시기', '물 마시기', '선크림 바르기', '영양제먹기'],
  },
  {
    name: '쪙',
    count: 3,
    timeAgo: '1시간 전',
    routines: ['물 마시기', '선크림 바르기', '영양제먹기'],
  },
  {
    name: '현정',
    count: 9,
    timeAgo: '1시간 전',
    routines: [
      '물 마시기',
      '물 마시기',
      '물 마시기',
      '선크림 바르기',
      '선크림 바르기',
      '선크림 바르기',
      '영양제먹기',
      '영양제먹기',
      '영양제먹기',
    ],
  },
];

function NotificationGroup({ name, count, timeAgo, routines }: RoutineGroup) {
  return (
    <div className="flex flex-col items-end gap-2.5 w-full">
      <div className="flex items-center gap-1.5 pl-7 w-full">
        <div className="size-8 rounded-full bg-gray-300 shrink-0" />
        <p className="text-xs text-black">
          <span className="font-bold">{name}</span>님이 루틴을{' '}
          <span className="font-bold">{count}회</span> 완료했습니다.{' '}
          <span className="text-[#8b8b8b]">{timeAgo}</span>
        </p>
      </div>
      <div className="flex flex-col items-center gap-1.5 w-full border-l border-[#6e6e6e]">
        {routines.map((routine, i) => (
          <div key={i} className="flex items-center justify-between pl-3.5 pr-5 w-full">
            <p className="text-sm text-black">{routine}</p>
            <Sun size={16} strokeWidth={1.5} color="#6e6e6e" />
          </div>
        ))}
      </div>
    </div>
  );
}

export default function NotificationPage() {
  const [tab, setTab] = useState<NotificationTab>('friendRoutine');

  return (
    <div className="flex w-full flex-col items-start gap-4 pl-7 pt-4">
      <p className="text-lg font-bold text-black">알림</p>
      <div className="flex gap-2.5 items-center pr-7">
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
      <div className="flex flex-col gap-5 pr-7">
        {tab === 'friendRoutine' ? (
          GROUPS.map((group) => <NotificationGroup key={group.name} {...group} />)
        ) : (
          <p className="text-sm text-[#8b8b8b] pl-7">받은 공감이 없습니다.</p>
        )}
      </div>
    </div>
  );
}
