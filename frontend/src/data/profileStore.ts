const PROFILE_PHOTO_KEY = 'routine-app:profilePhoto';

export function getProfilePhoto(): string | null {
  try {
    return localStorage.getItem(PROFILE_PHOTO_KEY);
  } catch {
    return null;
  }
}

export function setProfilePhoto(dataUrl: string) {
  localStorage.setItem(PROFILE_PHOTO_KEY, dataUrl);
}

export function clearProfilePhoto() {
  localStorage.removeItem(PROFILE_PHOTO_KEY);
}
