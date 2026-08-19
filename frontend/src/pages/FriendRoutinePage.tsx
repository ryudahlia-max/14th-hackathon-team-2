import { useCallback, useEffect, useState } from 'react';
import { Heart, Frown, ThumbsUp, Flame, Smile, Sun } from 'lucide-react';
import AppNavigationBar from '../components/AppNavigationBar';
import Avatar from '../components/Avatar';
import { getFeed, getReceivedReactions, reactToCompletion, removeCompletionReaction, type FeedItem, type ReceivedReaction } from '../api/feed';
import { useAuth } from '../auth/authState';
import { formatRelativeTime } from '../utils/formatRelativeTime';
import type { Reaction } from '../types/reaction';
import { getServiceNotifications, markAllNotificationsRead, markNotificationRead, type ServiceNotification } from '../api/notification';
import { useAutoRefresh } from '../utils/useAutoRefresh';

type Tab = 'friendRoutine' | 'receivedLikes' | 'service';
const REACTIONS: Reaction[] = ['heart', 'sad', 'thumbsUp', 'fire', 'smile'];
const ICONS = { heart: Heart, sad: Frown, thumbsUp: ThumbsUp, fire: Flame, smile: Smile };
const COLORS = { heart: 'text-red-500', sad: 'text-yellow-500', thumbsUp: 'text-blue-500', fire: 'text-orange-500', smile: 'text-yellow-500' };

function fromApiType(type: string | null): Reaction | undefined {
  const value = type?.toLowerCase().replace(/_([a-z])/g, (_, char: string) => char.toUpperCase());
  return REACTIONS.find(reaction => reaction === value);
}

export default function FriendRoutinePage() {
  const { user } = useAuth();
  const [tab, setTab] = useState<Tab>('friendRoutine');
  const [items, setItems] = useState<FeedItem[]>([]);
  const [received, setReceived] = useState<ReceivedReaction[]>([]);
  const [serviceNotifications, setServiceNotifications] = useState<ServiceNotification[]>([]);
  const [openId, setOpenId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [feed, reactions, notifications] = await Promise.all([
        getFeed(),
        getReceivedReactions(),
        getServiceNotifications(),
      ]);
      setItems(feed.items.filter(item => item.userId !== user?.id));
      setReceived(reactions);
      setServiceNotifications(notifications);
    } catch (loadError) { console.error(loadError); setError('알림을 불러오지 못했습니다.'); }
  }, [user?.id]);
  useEffect(() => { void load(); }, [load]);
  useAutoRefresh(60_000);

  async function select(item: FeedItem, reaction: Reaction) {
    const current = fromApiType(item.myReaction);
    setOpenId(null);
    try {
      if (current === reaction) await removeCompletionReaction(item.completionId);
      else await reactToCompletion(item.completionId, reaction);
      await load();
    } catch (reactionError) { console.error(reactionError); setError('반응을 저장하지 못했습니다.'); }
  }

  async function readNotification(notification: ServiceNotification) {
    if (notification.readAt) return;
    try {
      await markNotificationRead(notification.id);
      setServiceNotifications(previous => previous.map(item =>
        item.id === notification.id ? { ...item, readAt: new Date().toISOString() } : item));
    } catch (readError) {
      console.error(readError);
      setError('알림을 읽음 처리하지 못했습니다.');
    }
  }

  async function readAll() {
    try {
      await markAllNotificationsRead();
      const readAt = new Date().toISOString();
      setServiceNotifications(previous => previous.map(item => ({ ...item, readAt: item.readAt ?? readAt })));
    } catch (readError) {
      console.error(readError);
      setError('알림을 읽음 처리하지 못했습니다.');
    }
  }

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex-1 overflow-y-auto px-7 pt-4">
        <p className="text-lg font-bold mb-4">알림</p>
        <div className="flex gap-2.5 mb-5">
          <button onClick={() => setTab('friendRoutine')} className={`h-8 px-4 rounded-full border text-sm ${tab === 'friendRoutine' ? 'bg-[#a2bfff] text-white' : ''}`}>친구의 루틴</button>
          <button onClick={() => setTab('receivedLikes')} className={`h-8 px-4 rounded-full border text-sm ${tab === 'receivedLikes' ? 'bg-[#a2bfff] text-white' : ''}`}>받은 공감</button>
          <button onClick={() => setTab('service')} className={`h-8 px-4 rounded-full border text-sm ${tab === 'service' ? 'bg-[#a2bfff] text-white' : ''}`}>서비스 알림</button>
        </div>
        {tab === 'service' && serviceNotifications.some(item => !item.readAt) && (
          <button onClick={() => void readAll()} className="mb-4 text-xs text-[#6685c7] underline">모두 읽음</button>
        )}
        {error && <p className="mb-4 text-xs text-red-500">{error}</p>}
        <div className="space-y-5">
          {tab === 'friendRoutine' ? items.map(item => {
            const reaction = fromApiType(item.myReaction);
            const CurrentIcon = reaction ? ICONS[reaction] : Sun;
            return <div key={item.completionId} className="flex gap-3">
              <Avatar friendId={item.userId} src={item.avatarUrl} className="size-8" />
              <div className="flex-1"><p className="text-xs"><b>{item.nickname}</b>님이 <b>{item.routineTitle}</b> 루틴을 완료했습니다. <span className="text-gray-400">{formatRelativeTime(item.completedAt)}</span></p>
                <div className="relative mt-2 flex justify-end"><button aria-label="반응 남기기" onClick={() => setOpenId(openId === item.completionId ? null : item.completionId)}><CurrentIcon size={17} className={reaction ? COLORS[reaction] : ''} /></button>
                  {openId === item.completionId && <div className="absolute right-0 bottom-full mb-2 flex gap-1 rounded-full border bg-white p-2 shadow-lg">{REACTIONS.map(value => { const Icon = ICONS[value]; return <button key={value} aria-label={value} onClick={() => void select(item, value)}><Icon size={17} className={COLORS[value]} /></button>; })}</div>}
                </div>
              </div>
            </div>;
          }) : tab === 'receivedLikes' ? received.map(item => { const reaction = fromApiType(item.type) ?? 'heart'; const Icon = ICONS[reaction]; return <div key={`${item.completionId}-${item.reactorId}`} className="flex gap-3"><Avatar friendId={item.reactorId} src={item.reactorAvatarUrl} className="size-8" /><p className="flex-1 text-xs"><b>{item.reactorNickname}</b>님이 <b>{item.routineTitle}</b>에 공감을 남겼습니다. <span className="text-gray-400">{formatRelativeTime(item.createdAt)}</span></p><Icon size={17} className={COLORS[reaction]} /></div>; })
            : serviceNotifications.map(notification => (
              <button
                key={notification.id}
                onClick={() => void readNotification(notification)}
                className={`w-full rounded-xl px-3 py-3 text-left ${notification.readAt ? 'bg-white' : 'bg-[#eef3ff]'}`}
              >
                <p className="text-xs">{notification.content}</p>
                <span className="mt-1 block text-[10px] text-gray-400">{formatRelativeTime(notification.createdAt)}</span>
              </button>
            ))}
          {((tab === 'friendRoutine' && items.length === 0)
            || (tab === 'receivedLikes' && received.length === 0)
            || (tab === 'service' && serviceNotifications.length === 0))
            && <p className="text-sm text-gray-400">아직 알림이 없어요.</p>}
        </div>
      </div>
      <AppNavigationBar />
    </div>
  );
}
