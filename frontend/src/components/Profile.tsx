interface Props {
  src?: string | null;
}

export default function Profile({ src }: Props) {
  if (src) {
    return (
      <img
        src={src}
        alt="프로필 사진"
        className="h-20 w-20 shrink-0 rounded-full object-cover"
      />
    );
  }

  return (
    <svg
      className="h-20 w-20 shrink-0"
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 200 200"
      fill="none"
    >
      <circle cx="100" cy="100" r="100" fill="#D9D9D9" />
    </svg>
  );
}
