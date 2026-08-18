import { Check, Pencil } from 'lucide-react';
import RoutineIcon from './RoutineIcon';
import type { Routine, MonthProgress } from '../types';

interface Props {
  weekStart: Date;
  routines: Routine[];
  progress: MonthProgress;
  selectedDay: string;
  onDaySelect: (dateStr: string) => void;
  onToggleInstance: (dateStr: string, routineId: string, instanceIndex: number) => void;
  onAddRoutine: () => void;
  onEditRoutine: (routine: Routine) => void;
}

const DAY_NAMES = ['월', '화', '수', '목', '금', '토', '일'];

function toDateStr(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export default function WeekCalendar({
  weekStart,
  routines,
  progress,
  selectedDay,
  onDaySelect,
  onToggleInstance,
  onAddRoutine,
  onEditRoutine,
}: Props) {
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
        filled: (dayProgress[r.id] ?? []).includes(i),
      }))
    );
  }

  function isAllComplete(dateStr: string) {
    const dayProgress = progress[dateStr] ?? {};
    return routines.length > 0 && routines.every(r => (dayProgress[r.id]?.length ?? 0) >= r.totalCount);
  }

  const selectedDayProgress = progress[selectedDay] ?? {};

  // Each count of a routine is one row
  const routineRows = routines.flatMap(r =>
    Array.from({ length: r.totalCount }, (_, i) => ({ routine: r, index: i }))
  );

  function handleCheck(routineId: string, instanceIndex: number) {
    onToggleInstance(selectedDay, routineId, instanceIndex);
  }

  return (
    <div>
      {/* Day name header */}
      <div className="grid grid-cols-7 mb-1">
        {DAY_NAMES.map((name, i) => (
          <div
            key={name}
            className={`text-center text-xs py-1 font-medium ${i === 5 ? 'text-blue-400' : i === 6 ? 'text-red-400' : 'text-gray-400'
              }`}
          >
            {name}
          </div>
        ))}
      </div>

      {/* Week row */}
      <div className="grid grid-cols-7">
        {days.map((d, i) => {
          const dateStr = toDateStr(d);
          const segments = buildSegments(dateStr);
          const allComplete = isAllComplete(dateStr);
          const isToday =
            d.getFullYear() === today.getFullYear() &&
            d.getMonth() === today.getMonth() &&
            d.getDate() === today.getDate();
          const isSelected = dateStr === selectedDay;

          return (
            <button key={i} onClick={() => onDaySelect(dateStr)} className="flex flex-col items-center py-1">
              <div
                className={`flex flex-col items-center gap-1 px-1 py-1 rounded-full ${isSelected ? 'border border-[#6E6E6E]' : ''
                  }`}
              >
                <RoutineIcon segments={segments} allComplete={allComplete} size={40} />
                <span
                  className={`text-xs leading-none px-1.5 py-0.5 rounded-full ${isToday ? 'bg-gray-200 font-bold' : ''
                    } ${i === 5
                      ? 'text-blue-400'
                      : i === 6
                        ? 'text-red-400'
                        : 'text-gray-700'
                    }`}
                >
                  {d.getDate()}
                </span>
              </div>
            </button>
          );
        })}
      </div>

      {/* Divider */}
      <div className="h-px bg-gray-100 mt-6 mb-6" />

      {/* Add routine button */}
      <div className="mb-6">
        <button
          onClick={onAddRoutine}
          className="flex items-center gap-2 px-4 py-2 rounded-full border border-gray-300 text-sm text-gray-600"
        >
          <span>루틴 등록</span>
          <span className="w-5 h-5 rounded-full border border-gray-400 flex items-center justify-center text-gray-500 text-xs font-bold leading-none">
            +
          </span>
        </button>
      </div>

      {/* Routine list */}
      <div className="space-y-5">
        {routineRows.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-2"></p>
        ) : (
          routineRows.map(({ routine, index }) => {
            const checked = (selectedDayProgress[routine.id] ?? []).includes(index);
            return (
              <div key={`${routine.id}-${index}`} className="flex items-center gap-3">
                <button
                  onClick={() => handleCheck(routine.id, index)}
                  className="w-5 h-5 rounded border-2 flex items-center justify-center shrink-0"
                  style={{
                    borderColor: routine.color,
                    background: checked ? routine.color : 'white',
                  }}
                >
                  {checked && <Check size={11} color="white" strokeWidth={3} />}
                </button>
                <span
                  className="flex-1 text-sm"
                  style={{ color: checked ? routine.color : '#374151' }}
                >
                  {routine.name}
                </span>
                {index === 0 && (
                  <button
                    onClick={() => onEditRoutine(routine)}
                    aria-label={`${routine.name} 수정`}
                    className="p-1 text-gray-400"
                  >
                    <Pencil size={14} />
                  </button>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
