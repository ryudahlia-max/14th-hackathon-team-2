import { useState } from 'react';
import { X, Trash2 } from 'lucide-react';
import type { Routine } from '../types';

const COLORS = [
  '#60A5FA',
  '#F87171',
  '#34D399',
  '#FBBF24',
  '#A78BFA',
  '#FB923C',
  '#F472B6',
  '#2DD4BF',
];

interface Props {
  initial?: Routine;
  onSave: (name: string, color: string) => void;
  onDelete?: () => void;
  onClose: () => void;
}

export default function AddRoutineModal({ initial, onSave, onDelete, onClose }: Props) {
  const isEditing = !!initial;
  const [name, setName] = useState(initial?.name ?? '');
  const [color, setColor] = useState(initial?.color ?? COLORS[0]);

  function handleSubmit() {
    if (!name.trim()) return;
    onSave(name.trim(), color);
    onClose();
  }

  function handleDelete() {
    onDelete?.();
    onClose();
  }

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-white rounded-t-2xl w-full max-w-[393px] px-6 pt-6 pb-10">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold">{isEditing ? '루틴 수정' : '루틴 등록'}</h2>
          <button onClick={onClose} className="p-1">
            <X size={20} color="#6b7280" />
          </button>
        </div>

        <div className="mb-5">
          <label className="text-sm text-gray-500 mb-1.5 block">루틴 이름</label>
          <input
            value={name}
            onChange={e => setName(e.target.value)}
            className="w-full border border-gray-300 rounded-xl px-4 py-3 text-sm outline-none focus:border-[#a2bfff]"
            autoFocus
            onKeyDown={e => e.key === 'Enter' && handleSubmit()}
          />
        </div>

        <div className="mb-8">
          <label className="text-sm text-gray-500 mb-2 block">색상</label>
          <div className="flex gap-3 flex-wrap">
            {COLORS.map(c => (
              <button
                key={c}
                onClick={() => setColor(c)}
                className="w-8 h-8 rounded-full transition-transform"
                style={{
                  background: c,
                  outline: color === c ? `3px solid ${c}` : 'none',
                  outlineOffset: '2px',
                  transform: color === c ? 'scale(1.15)' : 'scale(1)',
                }}
              />
            ))}
          </div>
        </div>

        <div className="flex gap-3">
          {isEditing && onDelete && (
            <button
              onClick={handleDelete}
              aria-label="루틴 삭제"
              className="w-12 shrink-0 rounded-xl border border-red-200 flex items-center justify-center"
            >
              <Trash2 size={18} color="#ef4444" />
            </button>
          )}
          <button
            onClick={handleSubmit}
            disabled={!name.trim()}
            className="flex-1 py-3 rounded-xl bg-[#a2bfff] text-white font-semibold text-sm disabled:opacity-40"
          >
            {isEditing ? '수정' : '추가'}
          </button>
        </div>
      </div>
    </div>
  );
}
