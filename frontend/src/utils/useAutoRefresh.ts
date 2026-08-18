import { useEffect, useState } from 'react';

// 주기적으로 리렌더를 트리거해서 formatRelativeTime 같은 값이
// 새로고침 없이도 시간이 지나면서 자동으로 갱신되도록 한다.
export function useAutoRefresh(intervalMs: number) {
  const [, setTick] = useState(0);

  useEffect(() => {
    const id = setInterval(() => setTick(t => t + 1), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs]);
}
