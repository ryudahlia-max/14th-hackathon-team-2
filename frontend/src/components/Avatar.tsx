import { ME_ID } from '../data/mockData';
import { getProfilePhoto } from '../data/profileStore';

interface Props {
  friendId: string;
  className?: string;
}

export default function Avatar({ friendId, className = 'w-10 h-10' }: Props) {
  const photo = friendId === ME_ID ? getProfilePhoto() : null;

  if (photo) {
    return <img src={photo} alt="" className={`${className} rounded-full object-cover shrink-0`} />;
  }

  return <div className={`${className} rounded-full bg-gray-300 shrink-0`} />;
}
