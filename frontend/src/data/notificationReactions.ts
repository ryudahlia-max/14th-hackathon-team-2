export type Reaction = 'heart' | 'sad' | 'thumbsUp' | 'fire' | 'smile';

export interface RoutineEntry {
  name: string;
  photoUrl?: string;
  myReaction?: Reaction;
}

export interface RoutineGroup {
  name: string;
  count: number;
  timeAgo: string;
  routines: RoutineEntry[];
}

export const GROUPS: RoutineGroup[] = [
  {
    name: '연진',
    count: 5,
    timeAgo: '1시간 전',
    routines: [
      { name: '물 마시기' },
      { name: '물 마시기' },
      { name: '물 마시기' },
      { name: '선크림 바르기' },
      { name: '영양제먹기' },
    ],
  },
  {
    name: '쪙',
    count: 3,
    timeAgo: '1시간 전',
    routines: [
      { name: '물 마시기' },
      { name: '선크림 바르기' },
      { name: '영양제먹기' },
    ],
  },
  {
    name: '현정',
    count: 9,
    timeAgo: '1시간 전',
    routines: [
      { name: '물 마시기', photoUrl: 'https://example.com/photo.jpg' },
      { name: '물 마시기' },
      { name: '물 마시기' },
      { name: '선크림 바르기' },
      { name: '선크림 바르기' },
      { name: '선크림 바르기' },
      { name: '영양제먹기' },
      { name: '영양제먹기' },
      { name: '영양제먹기' },
    ],
  },
];

export function reactionKey(groupName: string, index: number) {
  return `${groupName}-${index}`;
}

export function initialReactions(groups: RoutineGroup[]): Record<string, Reaction> {
  const initial: Record<string, Reaction> = {};
  for (const group of groups) {
    group.routines.forEach((routine, i) => {
      if (routine.myReaction) initial[reactionKey(group.name, i)] = routine.myReaction;
    });
  }
  return initial;
}

// TODO: 백엔드에 POST/DELETE /api/v1/routines/:routineId/reactions 엔드포인트가 아직 없어
// (RoutineController에는 completions만 존재) 실제 네트워크 호출은 보류합니다.
// 엔드포인트가 추가되면 아래 두 함수 안의 fetch 호출 주석을 해제해 연동하세요.
export async function postReaction(routineId: string, reaction: Reaction): Promise<void> {
  void routineId;
  void reaction;
  // await fetch(`/api/v1/routines/${routineId}/reactions`, {
  //   method: 'POST',
  //   headers: { 'Content-Type': 'application/json' },
  //   body: JSON.stringify({ reaction }),
  // });
}

export async function deleteReaction(routineId: string): Promise<void> {
  void routineId;
  // await fetch(`/api/v1/routines/${routineId}/reactions`, { method: 'DELETE' });
}
