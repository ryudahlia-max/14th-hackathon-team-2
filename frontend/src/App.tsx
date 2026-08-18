import { useState } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import FriendRoutinePage from './pages/FriendRoutinePage';
import {
  GROUPS as NOTIFICATION_GROUPS,
  initialReactions,
  postReaction,
  deleteReaction,
  type Reaction,
} from './data/notificationReactions';
import MessagesPage from './pages/MessagesPage';
import ChatRoomPage from './pages/ChatRoomPage';
import AiImageGenPage from './pages/AiImageGenPage';
import UserPage from './pages/UserPage';

export default function App() {
  // 알림 페이지가 라우트 전환으로 언마운트/재마운트되어도 남긴 반응이 유지되도록,
  // 이 상태를 페이지 컴포넌트가 아니라 항상 마운트된 상위(App)에서 소유한다.
  const [reactions, setReactions] = useState<Record<string, Reaction>>(() =>
    initialReactions(NOTIFICATION_GROUPS)
  );

  async function handleSelectReaction(key: string, reaction: Reaction) {
    const previous = reactions[key];
    const isCancel = previous === reaction;

    setReactions((prev) => {
      if (isCancel) {
        const { [key]: _removed, ...rest } = prev;
        return rest;
      }
      return { ...prev, [key]: reaction };
    });

    try {
      // key(예: "연진-0")를 이 프로토타입의 routineId 대신으로 사용합니다.
      // 실제 routineId가 도입되면 이 값을 그것으로 교체하세요.
      if (isCancel) {
        await deleteReaction(key);
      } else {
        await postReaction(key, reaction);
      }
    } catch (err) {
      console.error('반응 저장에 실패했습니다.', err);
      setReactions((prev) => {
        if (previous) return { ...prev, [key]: previous };
        const { [key]: _removed, ...rest } = prev;
        return rest;
      });
    }
  }

  return (
    <BrowserRouter>
      <div className="flex justify-center bg-gray-100 min-h-svh">
        <div className="w-full max-w-[393px] bg-white flex flex-col h-svh overflow-hidden">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route
              path="/notifications"
              element={
                <FriendRoutinePage reactions={reactions} onSelectReaction={handleSelectReaction} />
              }
            />
            <Route path="/messages" element={<MessagesPage />} />
            <Route path="/messages/:chatId" element={<ChatRoomPage />} />
            <Route path="/messages/:chatId/ai-image" element={<AiImageGenPage />} />
            <Route path="/profile" element={<UserPage />} />
          </Routes>
        </div>
      </div>
    </BrowserRouter>
  );
}
