import { useEffect, useState } from 'react';
import { Sun } from 'lucide-react';
import FriendPill from '../components/FriendPill';
import MonthCalendar from '../components/MonthCalendar';
import WeekCalendar from '../components/WeekCalendar';
import AddRoutineModal from '../components/AddRoutineModal';
import AppNavigationBar from '../components/AppNavigationBar';
import { FRIENDS } from '../data/mockData';
import { removeRoutineCompletionNotification, setRoutineCompletionNotification } from '../data/notificationStore';
import type { Routine, MonthProgress, DayProgress } from '../types';

type View = 'month' | 'week';

function getMonday(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay();
  d.setDate(d.getDate() - (day === 0 ? 6 : day - 1));
  return d;
}

// 해당 달(labelYear/labelMonth)로 표기될 때의 주차 계산
// 달의 첫 날을 포함하는 교차주가 이 달로 표기되면 그 주가 1주차이므로 이후 주차를 +1
function weekOfMonth(weekStart: Date, labelYear: number, labelMonth: number): number {
  const firstOfMonth = new Date(labelYear, labelMonth, 1);
  const firstDow = firstOfMonth.getDay();
  const daysBack = firstDow === 0 ? 6 : firstDow - 1;
  const firstMonday = new Date(labelYear, labelMonth, 1 - daysBack);
  // 달 시작 교차주(첫 번째 월요일이 이전 달에 있는 경우)에서 이 달이 차지하는 날 수
  const daysInOpeningWeek = firstDow === 0 ? 1 : firstDow === 1 ? 0 : 8 - firstDow;
  // 교차주가 이 달로 표기되면(≥4일) 그 주가 이미 1주차를 썼으므로 weekIndex+1
  const diffMs = weekStart.getTime() - firstMonday.getTime();
  const weekIndex = Math.round(diffMs / (7 * 24 * 60 * 60 * 1000));
  // 교차주가 없거나(1일=월요일) 교차주가 이 달로 표기된 경우: weekIndex+1
  // 교차주가 이전 달로 표기된 경우(3일 이하): 첫 월요일이 weekIndex=1 → 그대로
  return (daysInOpeningWeek === 0 || daysInOpeningWeek >= 4) ? weekIndex + 1 : weekIndex;
}

function getWeekLabel(weekStart: Date): { year: number; month: number; week: number } {
  const sunday = new Date(weekStart);
  sunday.setDate(weekStart.getDate() + 6);

  const sYear = weekStart.getFullYear();
  const sMonth = weekStart.getMonth();
  const eYear = sunday.getFullYear();
  const eMonth = sunday.getMonth();

  if (sYear === eYear && sMonth === eMonth) {
    return { year: sYear, month: sMonth, week: weekOfMonth(weekStart, sYear, sMonth) };
  }

  const lastDayOfStartMonth = new Date(sYear, sMonth + 1, 0).getDate();
  const daysInStartMonth = lastDayOfStartMonth - weekStart.getDate() + 1;

  if (daysInStartMonth >= 4) {
    return { year: sYear, month: sMonth, week: weekOfMonth(weekStart, sYear, sMonth) };
  } else {
    return { year: eYear, month: eMonth, week: 1 };
  }
}

function toDateStr(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const ROUTINES_KEY = 'routine-app:routines';
const PROGRESS_KEY = 'routine-app:progress';

function loadFromStorage<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

// 예전엔 루틴별 진행도를 누적 카운트(number)로 저장했던 데이터가 남아있을 수 있어
// 완료된 인스턴스 인덱스 배열(number[])로 변환해준다.
function loadProgressFromStorage(): Record<string, MonthProgress> {
  const raw = loadFromStorage<Record<string, Record<string, Record<string, unknown>>>>(PROGRESS_KEY, {});
  const migrated: Record<string, MonthProgress> = {};
  for (const [friendId, monthProgress] of Object.entries(raw)) {
    const days: MonthProgress = {};
    for (const [dateStr, dayProgress] of Object.entries(monthProgress)) {
      const normalizedDay: DayProgress = {};
      for (const [routineId, value] of Object.entries(dayProgress)) {
        normalizedDay[routineId] = Array.isArray(value)
          ? (value as number[])
          : Array.from({ length: Number(value) || 0 }, (_, i) => i);
      }
      days[dateStr] = normalizedDay;
    }
    migrated[friendId] = days;
  }
  return migrated;
}

export default function HomePage() {
  const [selectedFriendId, setSelectedFriendId] = useState(FRIENDS[0].id);
  const [view, setView] = useState<View>('month');
  const [viewDate, setViewDate] = useState(new Date(2026, 7, 1));
  const [weekStart, setWeekStart] = useState(() => getMonday(new Date(2026, 7, 15)));
  const [routinesByFriend, setRoutinesByFriend] = useState<Record<string, Routine[]>>(() =>
    loadFromStorage(ROUTINES_KEY, {})
  );
  const [progressByFriend, setProgressByFriend] = useState<Record<string, MonthProgress>>(loadProgressFromStorage);
  const [selectedDay, setSelectedDay] = useState(() => toDateStr(new Date()));
  const [showRoutineModal, setShowRoutineModal] = useState(false);
  const [editingRoutine, setEditingRoutine] = useState<Routine | null>(null);

  useEffect(() => {
    localStorage.setItem(ROUTINES_KEY, JSON.stringify(routinesByFriend));
  }, [routinesByFriend]);

  useEffect(() => {
    localStorage.setItem(PROGRESS_KEY, JSON.stringify(progressByFriend));
  }, [progressByFriend]);

  const routines = routinesByFriend[selectedFriendId] ?? [];
  const progress = progressByFriend[selectedFriendId] ?? {};

  function handleSaveRoutine(name: string, count: number, color: string) {
    setRoutinesByFriend(prev => {
      const friendRoutines = prev[selectedFriendId] ?? [];
      const next = editingRoutine
        ? friendRoutines.map(r => (r.id === editingRoutine.id ? { ...r, name, color, totalCount: count } : r))
        : [...friendRoutines, { id: `r-${Date.now()}`, name, color, totalCount: count }];
      return { ...prev, [selectedFriendId]: next };
    });
  }

  function handleDeleteRoutine(routineId: string) {
    setRoutinesByFriend(prev => ({
      ...prev,
      [selectedFriendId]: (prev[selectedFriendId] ?? []).filter(r => r.id !== routineId),
    }));
    setProgressByFriend(prev => {
      const friendProgress = prev[selectedFriendId] ?? {};
      const nextProgress: MonthProgress = {};
      for (const [dateStr, dayProgress] of Object.entries(friendProgress)) {
        const { [routineId]: _removed, ...rest } = dayProgress;
        nextProgress[dateStr] = rest;
      }
      return { ...prev, [selectedFriendId]: nextProgress };
    });
  }

  function openAddRoutine() {
    setEditingRoutine(null);
    setShowRoutineModal(true);
  }

  function openEditRoutine(routine: Routine) {
    setEditingRoutine(routine);
    setShowRoutineModal(true);
  }

  function handleToggleInstance(dateStr: string, routineId: string, instanceIndex: number) {
    const current = (progressByFriend[selectedFriendId]?.[dateStr] ?? {})[routineId] ?? [];
    const willCheck = !current.includes(instanceIndex);

    setProgressByFriend(prev => {
      const friendProgress = prev[selectedFriendId] ?? {};
      const dayProgress = friendProgress[dateStr] ?? {};
      const prevList = dayProgress[routineId] ?? [];
      const next = prevList.includes(instanceIndex)
        ? prevList.filter(i => i !== instanceIndex)
        : [...prevList, instanceIndex];
      return {
        ...prev,
        [selectedFriendId]: {
          ...friendProgress,
          [dateStr]: { ...dayProgress, [routineId]: next },
        },
      };
    });

    const routine = routines.find(r => r.id === routineId);
    if (willCheck && routine) {
      setRoutineCompletionNotification({
        friendId: selectedFriendId,
        friendName: selectedFriend.name,
        routineId,
        routineName: routine.name,
        dateStr,
        instanceIndex,
      });
    } else if (!willCheck) {
      removeRoutineCompletionNotification(selectedFriendId, routineId, dateStr, instanceIndex);
    }
  }

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

  const weekLabel = view === 'week' ? getWeekLabel(weekStart) : null;
  const headerLabel =
    view === 'month'
      ? `${year}년 ${month + 1}월`
      : `${weekLabel!.year}년 ${weekLabel!.month + 1}월 ${weekLabel!.week}주차`;

  return (
    <div className="flex flex-col h-full bg-white">
      {/* Friend pills */}
      <div className="flex gap-3 overflow-x-auto px-7 pt-4 pb-8 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
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
      <div className="flex items-center gap-3 px-7 pt-5 pb-5">
        <div className="w-12 h-12 rounded-full bg-gray-300 overflow-hidden">
          {selectedFriend.profileImage && (
            <img src={selectedFriend.profileImage} alt={selectedFriend.name} className="w-full h-full object-cover" />
          )}
        </div>
        <span className="text-base font-medium">{selectedFriend.name}</span>
      </div>

      {/* Calendar header */}
      <div className="flex items-center justify-between px-7 py-2 mb-3">
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
      <div className="flex-1 overflow-y-auto px-7 pb-2">
        {view === 'month' ? (
          <MonthCalendar
            year={year}
            month={month}
            routines={routines}
            progress={progress}
          />
        ) : (
          <WeekCalendar
            weekStart={weekStart}
            routines={routines}
            progress={progress}
            selectedDay={selectedDay}
            onDaySelect={setSelectedDay}
            onToggleInstance={handleToggleInstance}
            onAddRoutine={openAddRoutine}
            onEditRoutine={openEditRoutine}
          />
        )}
      </div>

      <AppNavigationBar />

      {showRoutineModal && (
        <AddRoutineModal
          initial={editingRoutine ?? undefined}
          onSave={handleSaveRoutine}
          onDelete={editingRoutine ? () => handleDeleteRoutine(editingRoutine.id) : undefined}
          onClose={() => setShowRoutineModal(false)}
        />
      )}
    </div>
  );
}
