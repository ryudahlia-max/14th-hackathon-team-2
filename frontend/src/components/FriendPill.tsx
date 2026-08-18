import type { Friend } from '../types';

interface Props {
  friend: Friend;
  isActive: boolean;
  onClick: () => void;
}

export default function FriendPill({ friend, isActive, onClick }: Props) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-[8px] pl-[3px] pr-[12px] py-[3px] rounded-[100px] border border-[#6e6e6e] shrink-0 ${
        isActive ? 'bg-[#a2bfff]' : 'bg-white'
      }`}
    >
      <div className="w-[33px] h-[33px] rounded-full bg-gray-300 overflow-hidden shrink-0" />
      <span className={`text-[13px] font-normal whitespace-nowrap ${isActive ? 'text-white' : 'text-black'}`}>
        {friend.name}
      </span>
    </button>
  );
}
