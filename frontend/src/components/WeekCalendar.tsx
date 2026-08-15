import RoutineIcon from './RoutineIcon';
import type { Routine, MonthProgress } from '../types';

interface Props {
  weekStart: Date; // Monday
  routines: Routine[];
  progress: MonthProgress;
  onAddRoutine?: () => void;
}

const DAY_NAMES = ['월', '화', '수', '목', '금', '토', '일'];

function toDateStr(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export default function WeekCalendar({ weekStart, routines, progress, onAddRoutine }: Props) {
  const today = new Date();
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(weekStart);
    d.setDate(weekStart.getDate() + i);
    return d;
  });

  function buildSegments(dateStr: string) {
    const dayProgress = progress[dateStr] ?? {};
    return routines.flatMap(r =>
      Array.from({ length: r.totalCount }, (_, i) => ({
        color: r.color,
        filled: (dayProgress[r.id] ?? 0) > i,
      }))
    );
  }

  function isAllComplete(dateStr: string) {
    const dayProgress = progress[dateStr] ?? {};
    return routines.length > 0 && routines.every(r => (dayProgress[r.id] ?? 0) >= r.totalCount);
  }

  return (
    <div>
      {/* Day name header */}
      <div className="grid grid-cols-7 mb-1">
        {DAY_NAMES.map((name, i) => (
          <div
            key={name}
            className={`text-center text-xs py-1 font-medium ${
              i === 5 ? 'text-blue-400' : i === 6 ? 'text-red-400' : 'text-gray-400'
            }`}
          >
            {name}
          </div>
        ))}
      </div>

      {/* Single week row */}
      <div className="grid grid-cols-7">
        {days.map((d, i) => {
          const dateStr = toDateStr(d);
          const segments = buildSegments(dateStr);
          const allComplete = isAllComplete(dateStr);
          const isToday =
            d.getFullYear() === today.getFullYear() &&
            d.getMonth() === today.getMonth() &&
            d.getDate() === today.getDate();

          return (
            <div key={i} className="flex flex-col items-center gap-1">
              <RoutineIcon segments={segments} allComplete={allComplete} size={40} />
              <span
                className={`text-xs leading-none px-1.5 py-0.5 rounded-full ${
                  isToday ? 'bg-gray-200 font-bold' : ''
                } ${i === 5 ? 'text-blue-400' : i === 6 ? 'text-red-400' : 'text-gray-700'}`}
              >
                {d.getDate()}
              </span>
            </div>
          );
        })}
      </div>

      {/* 루틴 등록 button */}
      <div className="mt-6">
        <button
          onClick={onAddRoutine}
          className="flex items-center gap-2 px-4 py-2 rounded-full border border-gray-300 text-sm text-gray-700"
        >
          <span>루틴 등록</span>
          <span className="w-5 h-5 rounded-full border border-gray-400 flex items-center justify-center text-gray-500 text-xs font-bold">+</span>
        </button>
      </div>
    </div>
  );
}
