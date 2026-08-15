import { BrowserRouter, Routes, Route } from 'react-router-dom';
import NavigationBar from './components/NavigationBar';

function Placeholder({ title }: { title: string }) {
  return (
    <div className="flex flex-1 items-center justify-center text-gray-400 text-lg pb-16">
      {title}
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <div className="flex flex-col min-h-svh">
        <Routes>
          <Route path="/" element={<Placeholder title="홈" />} />
          <Route path="/notifications" element={<Placeholder title="알림" />} />
          <Route path="/messages" element={<Placeholder title="메일" />} />
          <Route path="/profile" element={<Placeholder title="프로필" />} />
        </Routes>
        <NavigationBar />
      </div>
    </BrowserRouter>
  );
}
