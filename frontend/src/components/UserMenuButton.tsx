import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
  onClick?: () => void;
}

export default function UserMenuButton({ children, onClick }: Props) {
  return (
    <button
      onClick={onClick}
      className="flex w-90 items-center justify-center gap-2.5 self-center rounded-full bg-[rgba(188,207,248,0.5)] px-6 py-1"
    >
      {children}
    </button>
  );
}