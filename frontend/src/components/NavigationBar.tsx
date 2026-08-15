import { Home, Bell, Mail, User } from 'lucide-react';

export type NavTab = 'home' | 'notifications' | 'messages' | 'profile';

interface Props {
  active?: NavTab;
  onTabChange?: (tab: NavTab) => void;
}

const FILL = '#BCCFF8';
const STROKE_ON = '#6E6E6E';
const STROKE_OFF = '#9CA3AF';
const SW = 2;

const ActiveHome = () => (
  <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
    <path
      d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"
      fill={FILL}
      stroke={STROKE_ON}
      strokeWidth={SW}
      strokeLinecap="round"
      strokeLinejoin="round"
    />

    <path
      d="M9 22V12h6v10Z"
      fill={FILL}
      stroke={STROKE_ON}
      strokeWidth={SW}
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

const ActiveBell = () => (
  <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"
      fill={FILL} stroke={STROKE_ON} strokeWidth={SW} strokeLinecap="round" strokeLinejoin="round" />
    <path d="M13.73 21a2 2 0 0 1-3.46 0"
      stroke={STROKE_ON} strokeWidth={SW} strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const ActiveMail = () => (
  <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
    <rect x="2" y="4" width="20" height="16" rx="2"
      fill={FILL} stroke={STROKE_ON} strokeWidth={SW} strokeLinecap="round" strokeLinejoin="round" />
    <path d="M22 7l-10 7L2 7"
      stroke={STROKE_ON} strokeWidth={SW} strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const ActiveUser = () => (
  <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
    <circle cx="12" cy="8" r="4"
      fill={FILL} stroke={STROKE_ON} strokeWidth={SW} strokeLinecap="round" strokeLinejoin="round" />
    <path d="M20 21a8 8 0 0 0-16 0"
      fill={FILL} stroke={STROKE_ON} strokeWidth={SW} strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const NAV_ITEMS: { tab: NavTab; Icon: typeof Home; ActiveIcon: () => JSX.Element }[] = [
  { tab: 'home', Icon: Home, ActiveIcon: ActiveHome },
  { tab: 'notifications', Icon: Bell, ActiveIcon: ActiveBell },
  { tab: 'messages', Icon: Mail, ActiveIcon: ActiveMail },
  { tab: 'profile', Icon: User, ActiveIcon: ActiveUser },
];

export default function NavigationBar({ active, onTabChange }: Props) {
  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 flex justify-around items-center h-16 px-4 z-50">
      {NAV_ITEMS.map(({ tab, Icon, ActiveIcon }) => {
        const isActive = tab === active;
        return (
          <button
            key={tab}
            onClick={() => onTabChange?.(tab)}
            aria-label={tab}
            className="flex items-center justify-center p-2 transition-colors"
          >
            {isActive
              ? <ActiveIcon />
              : <Icon size={24} strokeWidth={SW} color={STROKE_OFF} />
            }
          </button>
        );
      })}
    </nav>
  );
}
