import { useState } from 'react';
import NavigationBar, { type NavTab } from './components/NavigationBar';
import NotificationPage from './pages/NotificationPage';

export default function App() {
  const [activeTab, setActiveTab] = useState<NavTab>('home');

  return (
    <div className="flex flex-col min-h-svh">
      <div className="flex-1 pb-16">
        {activeTab === 'notifications' && <NotificationPage />}
      </div>
      <NavigationBar active={activeTab} onTabChange={setActiveTab} />
    </div>
  );
}
