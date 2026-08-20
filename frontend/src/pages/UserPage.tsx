import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Pencil } from 'lucide-react';
import AppNavigationBar from '../components/AppNavigationBar';
import ConfirmModal from '../components/ConfirmModal';
import Profile from '../components/Profile';
import UserMenuButton from '../components/UserMenuButton';
import { useAuth } from '../auth/authState';
import { getProfile, updateProfile, uploadAvatar, type ProfileResponse } from '../api/profile';
import { getFriends } from '../api/friend';

const MAX_IMAGE_BYTES = 10 * 1024 * 1024;

export default function UserPage() {
  const navigate = useNavigate();
  const { signOut, user } = useAuth();
  const [friendCount, setFriendCount] = useState(0);
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([getProfile(), getFriends()]).then(([profileResult, friends]) => {
      setProfile(profileResult);
      setFriendCount(friends.length);
    }).catch(error => console.error('프로필을 불러오지 못했습니다.', error));
  }, []);

  async function handlePhotoPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    if (file.size > MAX_IMAGE_BYTES) {
      setProfileError('프로필 사진은 10MB 이하만 업로드할 수 있습니다.');
      return;
    }
    try {
      setProfileError(null);
      setProfile(await uploadAvatar(file));
    } catch (error) {
      console.error('프로필 사진 업로드에 실패했습니다.', error);
      setProfileError('프로필 사진 업로드에 실패했습니다.');
    }
  }

  async function handleAiConsent(enabled: boolean) {
    if (!profile) return;
    try {
      setProfileError(null);
      setProfile(await updateProfile({
        nickname: profile.nickname,
        timezone: profile.timezone,
        avatarObjectPath: profile.avatarObjectPath,
        aiFaceConsent: enabled,
      }));
    } catch (error) {
      console.error('AI 이미지 동의 설정에 실패했습니다.', error);
      setProfileError('AI 이미지 동의 설정에 실패했습니다.');
    }
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
            <Profile src={profile?.avatarUrl} />
            <span className="absolute bottom-0 right-0 w-6 h-6 rounded-full bg-[#a2bfff] border-2 border-white flex items-center justify-center">
              <Pencil size={11} color="white" />
            </span>
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/png,image/jpeg,image/webp"
            hidden
            onChange={handlePhotoPick}
          />
          <div className="flex flex-col items-start gap-1">
            <p className="text-lg font-bold text-black">{profile?.nickname ?? '사용자'}</p>
            <p className="text-sm text-[#6e6e6e]">{user?.email ?? ''}</p>
            <p className="text-sm text-[#6e6e6e]">친구 {friendCount}명</p>
          </div>
        </div>
        <div className="flex w-full flex-col gap-3">
          {profileError && <p className="text-sm text-red-500">{profileError}</p>}
          <label className="flex w-90 items-center justify-between self-center rounded-full bg-[rgba(188,207,248,0.5)] px-6 py-1 text-sm">
            <span>AI 이미지에 내 사진 사용</span>
            <input
              type="checkbox"
              checked={profile?.aiFaceConsent ?? false}
              disabled={!profile?.avatarObjectPath}
              onChange={event => void handleAiConsent(event.target.checked)}
            />
          </label>
          <p className="w-90 self-center px-2 text-xs leading-relaxed text-[#8a8a8a]">
            같은 인물로 생성하려면 한 명의 얼굴이 정면에서 선명하게 보이는 실제 사진을 사용해주세요. 실루엣·캐릭터·여러 명 사진은 AI 생성에 사용할 수 없습니다.
          </p>
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
