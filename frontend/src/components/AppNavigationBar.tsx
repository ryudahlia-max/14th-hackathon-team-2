import { useLocation, useNavigate } from 'react-router-dom';
import NavigationBar, { type NavTab } from './NavigationBar';

const TAB_PATHS: Record<NavTab, string> = {
  home: '/',
  notifications: '/notifications',
  messages: '/messages',
  profile: '/profile',
};

const PATH_TABS: Record<string, NavTab> = Object.fromEntries(
  Object.entries(TAB_PATHS).map(([tab, path]) => [path, tab as NavTab])
);

export default function AppNavigationBar() {
  const location = useLocation();
  const navigate = useNavigate();
  const active = PATH_TABS[location.pathname] ?? 'home';

  return (
    <NavigationBar
      active={active}
      onTabChange={tab => navigate(TAB_PATHS[tab])}
    />
  );
}
