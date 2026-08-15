import { Menu } from 'lucide-react';
import Profile from '../components/Profile';
import UserMenuButton from '../components/UserMenuButton';

export default function UserPage() {
  return (
    <div className="flex flex-col gap-6 px-6 pt-6">
      <div className="flex justify-end">
        <Menu size={28} strokeWidth={1.5} color="#000000" />
      </div>
      <div className="flex flex-col items-center gap-3">
        <Profile />
        <div className="flex flex-col items-center gap-1">
          <p className="text-lg font-bold text-black">이가영</p>
          <p className="text-sm text-[#6e6e6e]">emilygylee@naver.com</p>
          <p className="text-sm text-[#6e6e6e]">친구 3명</p>
        </div>
      </div>
      <div className="flex flex-col gap-4">
        <UserMenuButton>친구 관리</UserMenuButton>
        <UserMenuButton>로그아웃</UserMenuButton>
        <UserMenuButton>계정 삭제</UserMenuButton>
      </div>
    </div>
  );
}
