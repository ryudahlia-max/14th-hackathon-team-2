import { useCallback, useEffect, useMemo, useState } from 'react';
import { Sun } from 'lucide-react';
import FriendPill from '../components/FriendPill';
import Avatar from '../components/Avatar';
import MonthCalendar from '../components/MonthCalendar';
import WeekCalendar from '../components/WeekCalendar';
import AddRoutineModal from '../components/AddRoutineModal';
import AppNavigationBar from '../components/AppNavigationBar';
import { getFriends } from '../api/friend';
import { getProfile } from '../api/profile';
import {
  completeRoutine,
  createRoutine,
  deleteRoutine,
  getCalendar,
  getFriendCalendar,
  getFriendRoutines,
  getRoutines,
  uncompleteRoutine,
  updateRoutine,
  type RoutineResponse,
} from '../api/routine';
import type { Friend, Routine, MonthProgress } from '../types';

type View = 'month' | 'week';

function getMonday(date: Date) {
  const result = new Date(date);
  const day = result.getDay();
  result.setDate(result.getDate() - (day === 0 ? 6 : day - 1));
  return result;
}

function toDateStr(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function monthKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

function colorOf(color: string) {
  return /^#[0-9A-F]{6}$/i.test(color) ? color : '#60A5FA';
}

function toRoutine(response: RoutineResponse): Routine {
  return {
    id: response.id,
    name: response.title,
    color: colorOf(response.color),
    totalCount: 1,
    api: response,
  };
}

export default function HomePage() {
  const [people, setPeople] = useState<Friend[]>([]);
  const [meId, setMeId] = useState('');
  const [selectedFriendId, setSelectedFriendId] = useState('');
  const [view, setView] = useState<View>('month');
  const [viewDate, setViewDate] = useState(() => new Date());
  const [weekStart, setWeekStart] = useState(() => getMonday(new Date()));
  const [routines, setRoutines] = useState<Routine[]>([]);
  const [progress, setProgress] = useState<MonthProgress>({});
  const [selectedDay, setSelectedDay] = useState(() => toDateStr(new Date()));
  const [showRoutineModal, setShowRoutineModal] = useState(false);
  const [editingRoutine, setEditingRoutine] = useState<Routine | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([getProfile(), getFriends()])
      .then(([profile, friends]) => {
        const allPeople = [
          { id: profile.id, name: profile.nickname, profileImage: profile.avatarUrl ?? undefined },
          ...friends.map(friend => ({ id: friend.id, name: friend.name, profileImage: friend.avatarUrl })),
        ];
        setPeople(allPeople);
        setMeId(profile.id);
        setSelectedFriendId(profile.id);
      })
      .catch(() => setError('프로필과 친구 목록을 불러오지 못했습니다.'));
  }, []);

  const loadData = useCallback(async () => {
    if (!selectedFriendId || !meId) return;
    setError(null);
    try {
      if (selectedFriendId === meId) {
        const [routineResponses, calendar] = await Promise.all([
          getRoutines(),
          getCalendar(monthKey(view === 'month' ? viewDate : weekStart)),
        ]);
        setRoutines(routineResponses.map(toRoutine));
        const nextProgress: MonthProgress = {};
        for (const day of calendar) {
          nextProgress[day.date] = Object.fromEntries(day.completedRoutineIds.map(id => [id, [0]]));
        }
        setProgress(nextProgress);
      } else {
        const [routineResponses, calendar] = await Promise.all([
          getFriendRoutines(selectedFriendId),
          getFriendCalendar(selectedFriendId, monthKey(view === 'month' ? viewDate : weekStart)),
        ]);
        const nextProgress: MonthProgress = {};
        for (const day of calendar) {
          nextProgress[day.date] = Object.fromEntries(day.completedRoutineIds.map(id => [id, [0]]));
        }
        setRoutines(routineResponses.map(toRoutine));
        setProgress(nextProgress);
      }
    } catch (loadError) {
      console.error(loadError);
      setError('루틴 정보를 불러오지 못했습니다.');
    }
  }, [meId, selectedFriendId, view, viewDate, weekStart]);

  useEffect(() => { void loadData(); }, [loadData]);

  async function handleSaveRoutine(name: string, category: string, color: string) {
    try {
      if (editingRoutine?.api) await updateRoutine(editingRoutine.api, name, category, color);
      else await createRoutine(name, category, color, selectedDay);
      await loadData();
    } catch (saveError) {
      console.error(saveError);
      setError('루틴을 저장하지 못했습니다.');
    }
  }

  async function handleDeleteRoutine(id: string) {
    try {
      await deleteRoutine(id);
      await loadData();
    } catch (deleteError) {
      console.error(deleteError);
      setError('루틴을 삭제하지 못했습니다.');
    }
  }

  async function handleToggle(date: string, routineId: string) {
    if (selectedFriendId !== meId) return;
    const checked = (progress[date]?.[routineId] ?? []).includes(0);
    setProgress(previous => ({
      ...previous,
      [date]: { ...previous[date], [routineId]: checked ? [] : [0] },
    }));
    try {
      if (checked) await uncompleteRoutine(routineId, date);
      else await completeRoutine(routineId, date);
    } catch (toggleError) {
      console.error(toggleError);
      await loadData();
      setError('완료 상태를 변경하지 못했습니다.');
    }
  }

  const selectedFriend = useMemo(
    () => people.find(person => person.id === selectedFriendId) ?? people[0],
    [people, selectedFriendId],
  );
  const displayDate = view === 'month' ? viewDate : weekStart;
  const headerLabel = view === 'month'
    ? `${displayDate.getFullYear()}년 ${displayDate.getMonth() + 1}월`
    : `${weekStart.getFullYear()}년 ${weekStart.getMonth() + 1}월 ${Math.ceil(weekStart.getDate() / 7)}주차`;
  const isMine = selectedFriendId === meId;

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex gap-3 overflow-x-auto px-7 pt-4 pb-8 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {people.map(person => (
          <FriendPill key={person.id} friend={person} isActive={person.id === selectedFriendId} onClick={() => setSelectedFriendId(person.id)} />
        ))}
      </div>
      {selectedFriend && (
        <div className="flex items-center gap-3 px-7 pt-5 pb-5">
          <Avatar friendId={selectedFriend.id} src={selectedFriend.profileImage} className="w-12 h-12" />
          <span className="text-base font-medium">{selectedFriend.name}</span>
        </div>
      )}
      <div className="flex items-center justify-between px-7 py-2 mb-3">
        <div className="flex items-center gap-2"><Sun size={18} className="text-[#a2bfff]" /><span className="font-bold text-lg">{headerLabel}</span></div>
        <div className="flex items-center gap-2">
          <button onClick={() => setView(current => current === 'month' ? 'week' : 'month')} className="px-3 py-1.5 rounded-full border border-[#6e6e6e] text-sm">{view === 'month' ? '주' : '월'}</button>
          <button onClick={() => view === 'month' ? setViewDate(date => new Date(date.getFullYear(), date.getMonth() - 1, 1)) : setWeekStart(date => new Date(date.getFullYear(), date.getMonth(), date.getDate() - 7))} className="px-3 py-1.5 rounded-full border border-[#6e6e6e] text-sm">{'<'}</button>
          <button onClick={() => view === 'month' ? setViewDate(date => new Date(date.getFullYear(), date.getMonth() + 1, 1)) : setWeekStart(date => new Date(date.getFullYear(), date.getMonth(), date.getDate() + 7))} className="px-3 py-1.5 rounded-full border border-[#6e6e6e] text-sm">{'>'}</button>
        </div>
      </div>
      {error && <p className="px-7 pb-2 text-xs text-red-500">{error}</p>}
      <div className="flex-1 overflow-y-auto px-7 pb-2">
        {view === 'month' ? (
          <MonthCalendar year={viewDate.getFullYear()} month={viewDate.getMonth()} routines={routines} progress={progress} />
        ) : (
          <WeekCalendar
            weekStart={weekStart}
            routines={routines}
            progress={progress}
            selectedDay={selectedDay}
            onDaySelect={setSelectedDay}
            onToggleInstance={(date, id) => void handleToggle(date, id)}
            onAddRoutine={() => { setEditingRoutine(null); setShowRoutineModal(true); }}
            onEditRoutine={routine => { setEditingRoutine(routine); setShowRoutineModal(true); }}
            readOnly={!isMine}
          />
        )}
      </div>
      <AppNavigationBar />
      {showRoutineModal && isMine && (
        <AddRoutineModal
          initial={editingRoutine ?? undefined}
          onSave={handleSaveRoutine}
          onDelete={editingRoutine ? () => void handleDeleteRoutine(editingRoutine.id) : undefined}
          onClose={() => setShowRoutineModal(false)}
        />
      )}
    </div>
  );
}
