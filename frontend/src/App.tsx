import { useState } from 'react';
import NavigationBar, { type NavTab } from './components/NavigationBar';
import NotificationPage from './pages/FriendRoutinePage';

export default function App() {
  const [activeTab, setActiveTab] = useState<NavTab>('home');

  return (
    <div className="relative flex h-svh flex-col overflow-hidden">
      <div className="flex-1 overflow-y-auto">
        {activeTab === 'notifications' && <NotificationPage />}
      </div>
      <NavigationBar active={activeTab} onTabChange={setActiveTab} />
    </div>
  );
}
