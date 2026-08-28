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
  lastActive?: string;
}

export interface SongStat {
  videoId: string;
  title: string;
  channel: string;
  thumbnailUrl: string;
  playCount: number;
}

export interface RecentPlay {
  videoId: string;
  title: string;
  channel: string;
  thumbnailUrl: string;
  playedAt: string;
}

export interface FriendStats {
  totalSongs: number;
  totalPlays: number;
  estimatedMinutes: number;
  songsListened: SongStat[];
  recentPlays: RecentPlay[];
}