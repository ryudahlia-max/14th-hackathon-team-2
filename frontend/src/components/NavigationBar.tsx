import { Home, Bell, Mail, User } from 'lucide-react';

export type NavTab = 'home' | 'notifications' | 'messages' | 'profile';

interface Props {
  active?: NavTab;
  onTabChange?: (tab: NavTab) => void;
}

const NAV_ITEMS: { icon: typeof Home; tab: NavTab }[] = [
  { icon: Home, tab: 'home' },
  { icon: Bell, tab: 'notifications' },
  { icon: Mail, tab: 'messages' },
  { icon: User, tab: 'profile' },
];

export default function NavigationBar({ active, onTabChange }: Props) {
  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 flex justify-around items-center h-16 px-4 z-50">
      {NAV_ITEMS.map(({ icon: Icon, tab }) => {
        const isActive = tab === active;
        return (
          <button
            key={tab}
            onClick={() => onTabChange?.(tab)}
            aria-label={tab}
            className="flex items-center justify-center p-2 transition-colors"
          >
            <Icon
              size={24}
              strokeWidth={1.5}
              color={isActive ? '#6E6E6E' : '#9CA3AF'}
              fill={isActive ? '#BCCFF8' : 'none'}
            />
          </button>
        );
      })}
    </nav>
  );
}
