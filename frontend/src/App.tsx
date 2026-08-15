import NavigationBar from './components/NavigationBar';

export default function App() {
  return (
    <div className="flex flex-col min-h-svh">
      <div className="flex-1 pb-16" />
      <NavigationBar active="home" />
    </div>
  );
}
