import { useState } from 'react';
import { Menu, Sun } from 'lucide-react';
import FriendPill from '../components/FriendPill';
import MonthCalendar from '../components/MonthCalendar';
import WeekCalendar from '../components/WeekCalendar';
import NavigationBar from '../components/NavigationBar';
import { FRIENDS, ROUTINES, MONTH_PROGRESS } from '../data/mockData';

type View = 'month' | 'week';

function getMonday(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay();
  d.setDate(d.getDate() - (day === 0 ? 6 : day - 1));
  return d;
}

function weekOfMonth(monday: Date): number {
  return Math.ceil((monday.getDate() + 6) / 7);
}

export default function HomePage() {
  const [selectedFriendId, setSelectedFriendId] = useState(FRIENDS[0].id);
  const [view, setView] = useState<View>('month');
  const [viewDate, setViewDate] = useState(new Date(2026, 7, 1)); // month view: first of month
  const [weekStart, setWeekStart] = useState(() => getMonday(new Date(2026, 7, 15))); // week view: current week

  const selectedFriend = FRIENDS.find(f => f.id === selectedFriendId) ?? FRIENDS[0];
  const year = view === 'month' ? viewDate.getFullYear() : weekStart.getFullYear();
  const month = view === 'month' ? viewDate.getMonth() : weekStart.getMonth();

  function switchToWeek() {
    const today = new Date();
    const inSameMonth =
      today.getFullYear() === viewDate.getFullYear() && today.getMonth() === viewDate.getMonth();
    setWeekStart(getMonday(inSameMonth ? today : new Date(viewDate.getFullYear(), viewDate.getMonth(), 10)));
    setView('week');
  }

  function switchToMonth() {
    setViewDate(new Date(weekStart.getFullYear(), weekStart.getMonth(), 1));
    setView('month');
  }

  function prevPeriod() {
    if (view === 'month') {
      setViewDate(d => new Date(d.getFullYear(), d.getMonth() - 1, 1));
    } else {
      setWeekStart(d => { const next = new Date(d); next.setDate(d.getDate() - 7); return next; });
    }
  }

  function nextPeriod() {
    if (view === 'month') {
      setViewDate(d => new Date(d.getFullYear(), d.getMonth() + 1, 1));
    } else {
      setWeekStart(d => { const next = new Date(d); next.setDate(d.getDate() + 7); return next; });
    }
  }

  const headerLabel =
    view === 'month'
      ? `${year}년 ${month + 1}월`
      : `${year}년 ${month + 1}월 ${weekOfMonth(weekStart)}주차`;

  return (
    <div className="flex flex-col h-full bg-white">
      {/* Hamburger */}
      <div className="flex justify-end px-4 pt-5 pb-2">
        <button aria-label="메뉴">
          <Menu size={22} color="#333" />
        </button>
      </div>

      {/* Friend pills */}
      <div className="flex gap-3 overflow-x-auto px-4 pb-3 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {FRIENDS.map(friend => (
          <FriendPill
            key={friend.id}
            friend={friend}
            isActive={friend.id === selectedFriendId}
            onClick={() => setSelectedFriendId(friend.id)}
          />
        ))}
      </div>

      {/* Selected user profile */}
      <div className="flex items-center gap-3 px-4 py-3">
        <div className="w-12 h-12 rounded-full bg-gray-300 overflow-hidden">
          {selectedFriend.profileImage && (
            <img src={selectedFriend.profileImage} alt={selectedFriend.name} className="w-full h-full object-cover" />
          )}
        </div>
        <span className="text-base font-medium">{selectedFriend.name}</span>
      </div>

      {/* Calendar header */}
      <div className="flex items-center justify-between px-4 py-2">
        <div className="flex items-center gap-2">
          <Sun size={18} className="text-[#a2bfff]" />
          <span className="font-bold text-lg">{headerLabel}</span>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={view === 'month' ? switchToWeek : switchToMonth}
            className="px-3 py-1.5 rounded-full border border-[#6e6e6e] text-sm font-medium"
          >
            {view === 'month' ? '주' : '월'}
          </button>
          <button onClick={prevPeriod} className="px-3 py-1.5 rounded-full border border-[#6e6e6e] text-sm font-medium">
            {'<'}
          </button>
          <button onClick={nextPeriod} className="px-3 py-1.5 rounded-full border border-[#6e6e6e] text-sm font-medium">
            {'>'}
          </button>
        </div>
      </div>

      {/* Calendar */}
      <div className="flex-1 overflow-y-auto px-4 pb-2">
        {view === 'month' ? (
          <MonthCalendar
            year={year}
            month={month}
            routines={ROUTINES}
            progress={MONTH_PROGRESS}
          />
        ) : (
          <WeekCalendar
            weekStart={weekStart}
            routines={ROUTINES}
            progress={MONTH_PROGRESS}
            onAddRoutine={() => alert('루틴 등록 (미구현)')}
          />
        )}
      </div>

      <NavigationBar active="home" />
    </div>
  );
}
