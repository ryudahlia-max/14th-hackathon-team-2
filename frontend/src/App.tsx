import { BrowserRouter, Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import FriendRoutinePage from './pages/FriendRoutinePage';
import MessagesPage from './pages/MessagesPage';
import UserPage from './pages/UserPage';

export default function App() {
  return (
    <BrowserRouter>
      <div className="flex justify-center bg-gray-100 min-h-svh">
        <div className="w-full max-w-[393px] bg-white flex flex-col h-svh overflow-hidden">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/notifications" element={<FriendRoutinePage />} />
            <Route path="/messages" element={<MessagesPage />} />
            <Route path="/profile" element={<UserPage />} />
          </Routes>
        </div>
      </div>
    </BrowserRouter>
  );
}
