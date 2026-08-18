import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Pencil } from 'lucide-react';
import AppNavigationBar from '../components/AppNavigationBar';
import ConfirmModal from '../components/ConfirmModal';
import Profile from '../components/Profile';
import UserMenuButton from '../components/UserMenuButton';
import { useAuth } from '../auth/AuthContext';
import { FRIENDS, ME_ID } from '../data/mockData';
import { getProfilePhoto, setProfilePhoto } from '../data/profileStore';

export default function UserPage() {
  const navigate = useNavigate();
  const { signOut } = useAuth();
  const friendCount = FRIENDS.filter(f => f.id !== ME_ID).length;
  const [photo, setPhoto] = useState(() => getProfilePhoto());
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);

  const [showDeleteModal, setShowDeleteModal] = useState(false);

  function handlePhotoPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      setProfilePhoto(dataUrl);
      setPhoto(dataUrl);
    };
    reader.readAsDataURL(file);
  }

  function closeLogoutModal() {
    setShowLogoutModal(false);
    setLogoutError(null);
  }

  async function handleLogout() {
    setIsLoggingOut(true);
    setLogoutError(null);
    try {
      await signOut();
      setShowLogoutModal(false);
      navigate('/login');
    } catch (err) {
      console.error('로그아웃에 실패했습니다.', err);
      setLogoutError('로그아웃에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoggingOut(false);
    }
  }

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex-1 overflow-y-auto flex flex-col items-start gap-8 px-7 pt-4">
        <div className="flex items-center gap-4">
          <button
            onClick={() => fileInputRef.current?.click()}
            aria-label="프로필 사진 변경"
            className="relative shrink-0"
          >
            <Profile src={photo} />
            <span className="absolute bottom-0 right-0 w-6 h-6 rounded-full bg-[#a2bfff] border-2 border-white flex items-center justify-center">
              <Pencil size={11} color="white" />
            </span>
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            hidden
            onChange={handlePhotoPick}
          />
          <div className="flex flex-col items-start gap-1">
            <p className="text-lg font-bold text-black">이가영</p>
            <p className="text-sm text-[#6e6e6e]">emilygylee@naver.com</p>
            <p className="text-sm text-[#6e6e6e]">친구 {friendCount}명</p>
          </div>
        </div>
        <div className="flex w-full flex-col gap-3">
          <UserMenuButton onClick={() => navigate('/friends')}>친구 관리</UserMenuButton>
          <UserMenuButton onClick={() => setShowLogoutModal(true)}>로그아웃</UserMenuButton>
          <UserMenuButton onClick={() => setShowDeleteModal(true)}>계정 삭제</UserMenuButton>
        </div>
      </div>
      <AppNavigationBar />

      {showLogoutModal && (
        <ConfirmModal
          title="로그아웃"
          description="로그아웃 하시겠습니까?"
          confirmLabel={isLoggingOut ? '로그아웃 중...' : '로그아웃'}
          confirmDisabled={isLoggingOut}
          onCancel={closeLogoutModal}
          onConfirm={handleLogout}
        >
          {logoutError && <p className="text-sm text-red-500">{logoutError}</p>}
        </ConfirmModal>
      )}

      {showDeleteModal && (
        <ConfirmModal
          title="계정 삭제"
          description="현재 계정 삭제 기능은 준비 중입니다. 조금만 기다려주세요."
          confirmLabel="확인"
          onCancel={() => setShowDeleteModal(false)}
          onConfirm={() => setShowDeleteModal(false)}
        />
      )}
    </div>
  );
}
