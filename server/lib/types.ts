export interface AppUser {
  id: string;
  fb_id: string;
  name: string;
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
  avatarUrl: string | null;
  nowPlaying: NowPlaying | null;
}
