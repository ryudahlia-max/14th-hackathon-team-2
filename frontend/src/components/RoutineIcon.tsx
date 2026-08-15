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
  const n = segments.length;

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      {n === 0
        ? Array.from({ length: 8 }, (_, i) => (
            <rect
              key={i}
              x={cx - rayW / 2}
              y={cy - outerR}
              width={rayW}
              height={rayH}
              rx={rayW / 2}
              fill="#E5E7EB"
              transform={`rotate(${i * 45} ${cx} ${cy})`}
            />
          ))
        : segments.map((seg, i) => (
            <rect
              key={i}
              x={cx - rayW / 2}
              y={cy - outerR}
              width={rayW}
              height={rayH}
              rx={rayW / 2}
              fill={seg.filled ? seg.color : '#E5E7EB'}
              transform={`rotate(${i * (360 / n)} ${cx} ${cy})`}
            />
          ))}
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
