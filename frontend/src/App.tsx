import { useState } from 'react';
import NavigationBar, { type NavTab } from './components/NavigationBar';
import UserPage from './pages/UserPage';

export default function App() {
  const [activeTab, setActiveTab] = useState<NavTab>('home');

  return (
  <div className="mx-auto max-w-md min-h-svh w-full flex flex-col bg-white shadow-md">
  <div className="flex-1 pb-16">
    {activeTab === 'profile' && <UserPage />}
  </div>
  <NavigationBar active={activeTab} onTabChange={setActiveTab} />
</div>
  );
}