import HomePage from './pages/HomePage';

export default function App() {
  return (
    <div className="flex justify-center bg-gray-100 min-h-svh">
      <div className="w-full max-w-[393px] bg-white flex flex-col h-svh overflow-hidden">
        <HomePage />
      </div>
    </div>
  );
}
