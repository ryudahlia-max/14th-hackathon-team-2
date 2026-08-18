import { useEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import { acceptInvite, createInvite } from '../api/friend';
import type { InviteToken } from '../types/friend';

interface InviteFriendModalProps {
  onClose: () => void;
  onAccepted?: () => void;
}

function formatExpiry(expiresAt: string): string {
  const diffMs = new Date(expiresAt).getTime() - Date.now();
  const days = Math.max(0, Math.ceil(diffMs / (1000 * 60 * 60 * 24)));
  return `${days}일 후 만료됩니다`;
}

async function copyToClipboard(text: string) {
  await navigator.clipboard.writeText(text);
}

export default function InviteFriendModal({ onClose, onAccepted }: InviteFriendModalProps) {
  const contentRef = useRef<HTMLDivElement>(null);

  const [invite, setInvite] = useState<InviteToken | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [copiedField, setCopiedField] = useState<'code' | 'link' | null>(null);

  const [inputToken, setInputToken] = useState('');
  const [isAccepting, setIsAccepting] = useState(false);
  const [acceptError, setAcceptError] = useState<string | null>(null);
  const [accepted, setAccepted] = useState(false);

  useEffect(() => {
    if (accepted) return;
    function handlePointerDown(e: MouseEvent) {
      if (contentRef.current && !contentRef.current.contains(e.target as Node)) {
        onClose();
      }
    }
    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, [onClose, accepted]);

  useEffect(() => {
    if (!accepted) return;
    const timer = setTimeout(onClose, 1200);
    return () => clearTimeout(timer);
  }, [accepted, onClose]);

  async function handleCreateInvite() {
    setIsCreating(true);
    setCreateError(null);
    try {
      const result = await createInvite();
      setInvite(result);
    } catch (err) {
      console.error('초대 코드 발급에 실패했습니다.', err);
      setCreateError(err instanceof Error ? err.message : '초대 코드 발급에 실패했습니다.');
    } finally {
      setIsCreating(false);
    }
  }

  async function handleCopy(field: 'code' | 'link', text: string) {
    await copyToClipboard(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField((prev) => (prev === field ? null : prev)), 1500);
  }

  async function handleAccept() {
    const token = inputToken.trim();
    if (!token) return;
    setIsAccepting(true);
    setAcceptError(null);
    try {
      await acceptInvite(token);
      setAccepted(true);
      onAccepted?.();
    } catch (err) {
      console.error('친구 추가에 실패했습니다.', err);
      setAcceptError(err instanceof Error ? err.message : '친구 추가에 실패했습니다.');
    } finally {
      setIsAccepting(false);
    }
  }

  const inviteLink = invite ? `${window.location.origin}/invite/${invite.token}` : '';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-6">
      <div ref={contentRef} className="w-full max-w-xs rounded-2xl bg-white p-6 shadow-lg">
        <div className="flex items-center justify-between">
          <p className="text-base font-bold text-black">친구 추가</p>
          <button type="button" onClick={onClose} aria-label="닫기">
            <X size={18} color="#6e6e6e" />
          </button>
        </div>

        {accepted ? (
          <p className="mt-6 py-6 text-center text-sm text-black">친구로 추가되었습니다</p>
        ) : (
          <>
            <div className="mt-4">
              <p className="text-sm font-bold text-black">내 코드/링크 공유하기</p>
              {!invite ? (
                <button
                  type="button"
                  onClick={handleCreateInvite}
                  disabled={isCreating}
                  className="mt-2 w-full rounded-full bg-[#a2bfff] py-2 text-sm text-white disabled:bg-gray-200 disabled:text-[#8b8b8b]"
                >
                  {isCreating ? '만드는 중...' : '만들기'}
                </button>
              ) : (
                <div className="mt-2 flex flex-col gap-2">
                  <div className="rounded-xl border border-gray-200 p-3">
                    <p className="break-all font-mono text-xs text-black">{invite.token}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleCopy('code', invite.token)}
                    className="w-full rounded-full border border-[#6e6e6e] py-1.5 text-xs text-black"
                  >
                    {copiedField === 'code' ? '복사됨' : '복사'}
                  </button>

                  <div className="rounded-xl border border-gray-200 p-3">
                    <p className="break-all text-xs text-black">{inviteLink}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleCopy('link', inviteLink)}
                    className="w-full rounded-full border border-[#6e6e6e] py-1.5 text-xs text-black"
                  >
                    {copiedField === 'link' ? '복사됨' : '링크 복사'}
                  </button>

                  <p className="text-xs text-[#8b8b8b]">{formatExpiry(invite.expiresAt)}</p>
                </div>
              )}
              {createError && <p className="mt-2 text-xs text-red-500">{createError}</p>}
            </div>

            <div className="mt-6 border-t border-gray-200 pt-4">
              <p className="text-sm font-bold text-black">코드 입력해서 친구 추가하기</p>
              <input
                type="text"
                value={inputToken}
                onChange={(e) => setInputToken(e.target.value)}
                placeholder="상대방 코드 입력"
                disabled={isAccepting}
                className="mt-2 w-full rounded-full border border-[#6e6e6e] px-4 py-2 text-sm text-black"
              />
              <button
                type="button"
                onClick={handleAccept}
                disabled={isAccepting || !inputToken.trim()}
                className="mt-2 w-full rounded-full bg-[#a2bfff] py-2 text-sm text-white disabled:bg-gray-200 disabled:text-[#8b8b8b]"
              >
                {isAccepting ? '추가하는 중...' : '추가하기'}
              </button>
              {acceptError && <p className="mt-2 text-xs text-red-500">{acceptError}</p>}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
