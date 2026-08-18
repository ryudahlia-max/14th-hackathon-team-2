import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from './authState';

export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const { session, loading } = useAuth();
  if (loading) {
    return <div className="flex h-full items-center justify-center text-sm text-gray-400">불러오는 중...</div>;
  }
  if (!session) return <Navigate to="/login" replace />;
  return children;
}
