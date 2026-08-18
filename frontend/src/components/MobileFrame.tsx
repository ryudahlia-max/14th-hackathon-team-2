import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

export default function MobileFrame({ children }: Props) {
  return (
    <div className="flex justify-center bg-gray-100 min-h-svh">
      <div className="w-full max-w-[393px] bg-white flex flex-col h-svh overflow-hidden">
        {children}
      </div>
    </div>
  );
}
