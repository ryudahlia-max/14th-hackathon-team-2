import { Home, Bell, Mail, User } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';

const NAV_ITEMS = [
  { icon: Home, label: '홈', path: '/' },
  { icon: Bell, label: '알림', path: '/notifications' },
  { icon: Mail, label: '메일', path: '/messages' },
  { icon: User, label: '프로필', path: '/profile' },
] as const;

export default function NavigationBar() {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 flex justify-around items-center h-16 px-4 z-50">
      {NAV_ITEMS.map(({ icon: Icon, label, path }) => {
        const isActive = location.pathname === path;
        return (
          <button
            key={path}
            onClick={() => navigate(path)}
            className={`flex flex-col items-center gap-1 p-2 transition-colors ${
              isActive ? 'text-black' : 'text-gray-400 hover:text-gray-600'
            }`}
            aria-label={label}
          >
            <Icon size={24} strokeWidth={isActive ? 2 : 1.5} />
            <span className="text-xs">{label}</span>
          </button>
        );
      })}
    </nav>
  );
}
