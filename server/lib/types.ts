export interface AppUser {
  id: string;
  first_name: string;
  last_name: string;
  name: string;
  code: string;
  avatar_url: string | null;
}

export interface Song {
  videoId: string;
  title: string;
  channel: string;
  thumbnailUrl: string;
  source?: 'youtube' | 'spotify';
  trackId?: string;
  previewUrl?: string;
  albumName?: string;
}

export interface NowPlaying extends Song {
  isPlaying: boolean;
  updatedAt: string;
}

export interface FriendActivity {
  id: string;
  name: string;
  code: string;
  avatarUrl: string | null;
  nowPlaying: NowPlaying | null;
}
