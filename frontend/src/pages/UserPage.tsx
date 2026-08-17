import Profile from '../components/Profile';
import UserMenuButton from '../components/UserMenuButton';

export default function UserPage() {
  return (
    <div className="flex flex-col items-start gap-8 px-8 pt-8">
      <div className="flex items-center gap-4">
        <Profile />
        <div className="flex flex-col items-start gap-1">
          <p className="text-lg font-bold text-black">이가영</p>
          <p className="text-sm text-[#6e6e6e]">emilygylee@naver.com</p>
          <p className="text-sm text-[#6e6e6e]">친구 3명</p>
        </div>
      </div>
      <div className="flex w-full flex-col gap-3">
        <UserMenuButton>친구 관리</UserMenuButton>
        <UserMenuButton>로그아웃</UserMenuButton>
        <UserMenuButton>계정 삭제</UserMenuButton>
      </div>
    </div>
  );
}