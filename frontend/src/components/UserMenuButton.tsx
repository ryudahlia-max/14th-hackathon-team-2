import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
  onClick?: () => void;
}

export default function UserMenuButton({ children, onClick }: Props) {
  return (
    <button
      onClick={onClick}
      className="flex items-center justify-center gap-2.5 self-stretch rounded-full bg-[rgba(188,207,248,0.5)] px-[50px] py-2.5"
    >
      {children}
    </button>
  );
}
