interface Segment {
  color: string;
  filled: boolean;
}

interface Props {
  segments: Segment[];
  allComplete: boolean;
  size?: number;
}

export default function RoutineIcon({ segments, allComplete, size = 36 }: Props) {
  const cx = size / 2;
  const cy = size / 2;
  const outerR = size / 2 - 0.5;
  const innerR = outerR * 0.43;
  const gap = 1.5;
  const rayH = outerR - innerR - gap;
  const rayW = Math.max(2, size * 0.09);
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      {Array.from({ length: 8 }, (_, i) => {
        const seg = segments[i];
        return (
          <rect
            key={i}
            x={cx - rayW / 2}
            y={cy - outerR}
            width={rayW}
            height={rayH}
            rx={rayW / 2}
            fill={seg?.filled ? seg.color : '#E5E7EB'}
            transform={`rotate(${i * 45} ${cx} ${cy})`}
          />
        );
      })}
      <circle
        cx={cx}
        cy={cy}
        r={innerR}
        fill={allComplete ? '#a2bfff' : 'white'}
        stroke="#D1D5DB"
        strokeWidth="0.5"
      />
    </svg>
  );
}
