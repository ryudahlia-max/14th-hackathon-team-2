import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppNavigationBar from '../components/AppNavigationBar';
import ConfirmModal from '../components/ConfirmModal';
import Profile from '../components/Profile';
import UserMenuButton from '../components/UserMenuButton';
import { clearAuthToken, deleteAccount, logout } from '../api/auth';

export default function UserPage() {
  const navigate = useNavigate();

  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [understoodDeletion, setUnderstoodDeletion] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  function closeLogoutModal() {
    setShowLogoutModal(false);
    setLogoutError(null);
  }

  function closeDeleteModal() {
    setShowDeleteModal(false);
    setUnderstoodDeletion(false);
    setDeleteError(null);
  }

  async function handleLogout() {
    setIsLoggingOut(true);
    setLogoutError(null);
    try {
      await logout();
      clearAuthToken();
      setShowLogoutModal(false);
      navigate('/');
    } catch (err) {
      console.error('로그아웃에 실패했습니다.', err);
      setLogoutError('로그아웃에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoggingOut(false);
    }
  }

  async function handleDeleteAccount() {
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await deleteAccount();
      clearAuthToken();
      setShowDeleteModal(false);
      navigate('/');
    } catch (err) {
      console.error('계정 삭제에 실패했습니다.', err);
      setDeleteError('삭제에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex-1 overflow-y-auto flex flex-col items-start gap-8 px-8 pt-8">
        <div className="flex items-center gap-4">
          <Profile />
          <div className="flex flex-col items-start gap-1">
            <p className="text-lg font-bold text-black">이가영</p>
            <p className="text-sm text-[#6e6e6e]">emilygylee@naver.com</p>
            <p className="text-sm text-[#6e6e6e]">친구 3명</p>
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
          description="계정을 삭제하면 루틴, 친구, 기록 등 모든 데이터가 영구적으로 사라지며 복구할 수 없습니다."
          confirmLabel={isDeleting ? '삭제 중...' : '계정 삭제'}
          confirmDisabled={!understoodDeletion || isDeleting}
          onCancel={closeDeleteModal}
          onConfirm={handleDeleteAccount}
        >
          <label className="flex items-center gap-2 text-sm text-black">
            <input
              type="checkbox"
              checked={understoodDeletion}
              onChange={(e) => setUnderstoodDeletion(e.target.checked)}
              disabled={isDeleting}
              className="size-4"
            />
            삭제 내용을 이해했습니다
          </label>
          {deleteError && <p className="mt-2 text-sm text-red-500">{deleteError}</p>}
        </ConfirmModal>
      )}
    </div>
  );
}