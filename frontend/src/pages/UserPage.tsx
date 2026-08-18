import AppNavigationBar from '../components/AppNavigationBar';
import Profile from '../components/Profile';
import UserMenuButton from '../components/UserMenuButton';
import { FRIENDS } from '../data/mockData';

const ME_ID = '1'; // 이가영 (본인) — 친구 수 계산 시 제외

export default function UserPage() {
  const friendCount = FRIENDS.filter(f => f.id !== ME_ID).length;

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex-1 overflow-y-auto flex flex-col items-start gap-8 px-7 pt-4">
        <div className="flex items-center gap-4">
          <Profile />
          <div className="flex flex-col items-start gap-1">
            <p className="text-lg font-bold text-black">이가영</p>
            <p className="text-sm text-[#6e6e6e]">emilygylee@naver.com</p>
            <p className="text-sm text-[#6e6e6e]">친구 {friendCount}명</p>
          </div>
        </div>
        <div className="flex w-full flex-col gap-3">
          <UserMenuButton>친구 관리</UserMenuButton>
          <UserMenuButton>로그아웃</UserMenuButton>
          <UserMenuButton>계정 삭제</UserMenuButton>
        </div>
      </div>
      <AppNavigationBar />
    </div>
  );
}