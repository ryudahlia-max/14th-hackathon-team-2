import RoutineIcon from './RoutineIcon';
import type { Routine, MonthProgress } from '../types';

interface Props {
  year: number;
  month: number; // 0-indexed
  routines: Routine[];
  progress: MonthProgress;
}

const DAY_NAMES = ['월', '화', '수', '목', '금', '토', '일'];

function toDateStr(year: number, month: number, day: number) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

export default function MonthCalendar({ year, month, routines, progress }: Props) {
  const firstDow = new Date(year, month, 1).getDay(); // 0=Sun
  const startOffset = (firstDow + 6) % 7; // Mon-first
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const today = new Date();
  const isCurrentMonth = today.getFullYear() === year && today.getMonth() === month;

  function buildSegments(dateStr: string) {
    const dayProgress = progress[dateStr] ?? {};
    return routines.flatMap(r =>
      Array.from({ length: r.totalCount }, (_, i) => ({
        color: r.color,
        filled: (dayProgress[r.id] ?? []).includes(i),
      }))
    );
  }

  function isAllComplete(dateStr: string) {
    const dayProgress = progress[dateStr] ?? {};
    return routines.length > 0 && routines.every(r => (dayProgress[r.id]?.length ?? 0) >= r.totalCount);
  }

  const rows = Math.ceil((startOffset + daysInMonth) / 7);
  const cells = Array.from({ length: rows * 7 }, (_, i) => {
    const day = i - startOffset + 1;
    return day >= 1 && day <= daysInMonth ? day : null;
  });

  return (
    <div>
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

      <div className="grid grid-cols-7 gap-y-3">
        {cells.map((day, idx) => {
          if (!day) return <div key={idx} />;
          const col = idx % 7;
          const dateStr = toDateStr(year, month, day);
          const segments = buildSegments(dateStr);
          const allComplete = isAllComplete(dateStr);
          const isToday = isCurrentMonth && today.getDate() === day;

          return (
            <div key={idx} className="flex flex-col items-center gap-1">
              <RoutineIcon segments={segments} allComplete={allComplete} size={36} />
              <span
                className={`text-xs leading-none px-1 py-0.5 rounded-full ${
                  isToday ? 'bg-gray-200 font-bold' : ''
                } ${col === 5 ? 'text-blue-400' : col === 6 ? 'text-red-400' : 'text-gray-700'}`}
              >
                {day}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
