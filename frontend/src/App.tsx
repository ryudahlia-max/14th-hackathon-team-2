import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import HomePage from './pages/HomePage';
import FriendRoutinePage from './pages/FriendRoutinePage';
import MessagesPage from './pages/MessagesPage';
import ChatRoomPage from './pages/ChatRoomPage';
import AiImageGenPage from './pages/AiImageGenPage';
import UserPage from './pages/UserPage';
import LoginPage from './pages/LoginPage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="flex justify-center bg-gray-100 min-h-svh">
          <div className="w-full max-w-[393px] bg-white flex flex-col h-svh overflow-hidden">
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/notifications" element={<FriendRoutinePage />} />
              <Route path="/messages" element={<MessagesPage />} />
              <Route path="/messages/:chatId" element={<ChatRoomPage />} />
              <Route path="/messages/:chatId/ai-image" element={<AiImageGenPage />} />
              <Route path="/profile" element={<UserPage />} />
              <Route path="/login" element={<LoginPage />} />
            </Routes>
          </div>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}
