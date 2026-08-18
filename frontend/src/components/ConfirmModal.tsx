import { useEffect, useRef, type ReactNode } from 'react';

interface ConfirmModalProps {
  title: string;
  description: string;
  cancelLabel?: string;
  confirmLabel: string;
  confirmDisabled?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
  children?: ReactNode;
}

export default function ConfirmModal({
  title,
  description,
  cancelLabel = '취소',
  confirmLabel,
  confirmDisabled = false,
  onCancel,
  onConfirm,
  children,
}: ConfirmModalProps) {
  const contentRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handlePointerDown(e: MouseEvent) {
      if (contentRef.current && !contentRef.current.contains(e.target as Node)) {
        onCancel();
      }
    }
    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, [onCancel]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-6">
      <div ref={contentRef} className="w-full max-w-xs rounded-2xl bg-white p-6 shadow-lg">
        <p className="text-base font-bold text-black">{title}</p>
        <p className="mt-2 text-sm text-[#6e6e6e]">{description}</p>
        {children && <div className="mt-4">{children}</div>}
        <div className="mt-6 flex gap-3">
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 rounded-full border border-[#6e6e6e] py-2 text-sm text-black"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={confirmDisabled}
            className="flex-1 rounded-full bg-[#a2bfff] py-2 text-sm text-white disabled:bg-gray-200 disabled:text-[#8b8b8b]"
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
