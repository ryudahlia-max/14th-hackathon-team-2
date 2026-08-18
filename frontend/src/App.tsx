import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import HomePage from './pages/HomePage';
import FriendRoutinePage from './pages/FriendRoutinePage';
import MessagesPage from './pages/MessagesPage';
import ChatRoomPage from './pages/ChatRoomPage';
import AiImageGenPage from './pages/AiImageGenPage';
import UserPage from './pages/UserPage';
import FriendManagementPage from './pages/FriendManagementPage';
import LoginPage from './pages/LoginPage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="flex justify-center bg-gray-100 min-h-svh">
          <div className="w-full max-w-[393px] bg-white flex flex-col h-svh overflow-hidden">
            <Routes>
              <Route path="/" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
              <Route path="/notifications" element={<ProtectedRoute><FriendRoutinePage /></ProtectedRoute>} />
              <Route path="/messages" element={<ProtectedRoute><MessagesPage /></ProtectedRoute>} />
              <Route path="/messages/:chatId" element={<ProtectedRoute><ChatRoomPage /></ProtectedRoute>} />
              <Route path="/messages/:chatId/ai-image" element={<ProtectedRoute><AiImageGenPage /></ProtectedRoute>} />
              <Route path="/profile" element={<ProtectedRoute><UserPage /></ProtectedRoute>} />
              <Route path="/friends" element={<ProtectedRoute><FriendManagementPage /></ProtectedRoute>} />
              <Route path="/login" element={<LoginPage />} />
            </Routes>
          </div>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}
