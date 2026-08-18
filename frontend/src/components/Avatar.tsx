interface Props {
  friendId: string;
  src?: string | null;
  className?: string;
}

export default function Avatar({ friendId, src, className = 'w-10 h-10' }: Props) {
  if (src) {
    return <img src={src} alt="" data-user-id={friendId} className={`${className} rounded-full object-cover shrink-0`} />;
  }

  return <div className={`${className} rounded-full bg-gray-300 shrink-0`} />;
}
