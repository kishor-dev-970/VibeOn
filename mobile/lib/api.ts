import AsyncStorage from '@react-native-async-storage/async-storage';
import { NativeModules } from 'react-native';
import { API_URL } from './config';
import type { AppUser, FriendActivity, Song } from './types';

const TrendingBridge = (NativeModules as any).TrendingBridge;

const TOKEN_KEY = 'auth.session.token';

let _trendingCache: Promise<{ songs: Song[] }> | null = null;
const _liveCache: Record<string, Promise<{ songs: Song[] }>> = {};

export async function getStoredToken(): Promise<string | null> {
  return AsyncStorage.getItem(TOKEN_KEY);
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = await getStoredToken();
  const res = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init.headers ?? {}),
    },
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error((json as { error?: string }).error ?? `Request failed (${res.status})`);
  }
  return json as T;
}

export async function signInWithName(firstName: string, lastName: string): Promise<{ token: string; user: AppUser }> {
  const res = await fetch(`${API_URL}/api/auth/signup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ firstName, lastName }),
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error((json as { error?: string }).error ?? 'Login failed');
  }
  const data = json as { token: string; user: AppUser };
  await AsyncStorage.setItem(TOKEN_KEY, data.token);
  return data;
}

export async function logout(): Promise<void> {
  await AsyncStorage.removeItem(TOKEN_KEY);
}

export function fetchMe(): Promise<{ user: AppUser }> {
  return request('/api/me');
}

export function searchSongs(q: string): Promise<{ songs: Song[] }> {
  if (TrendingBridge?.search) {
    return TrendingBridge.search(q).then((songs: Song[]) => ({ songs }));
  }
  return request(`/api/songs/search?q=${encodeURIComponent(q)}`);
}

export async function updateNowPlaying(song: Song, isPlaying: boolean): Promise<unknown> {
  if (!(await getStoredToken())) return undefined;
  return request('/api/activity', {
    method: 'PUT',
    body: JSON.stringify({ song, isPlaying }),
  });
}

export async function clearNowPlaying(): Promise<unknown> {
  if (!(await getStoredToken())) return undefined;
  return request('/api/activity', { method: 'DELETE' });
}

export function fetchFriendsActivity(): Promise<{ friends: FriendActivity[] }> {
  return request('/api/activity');
}

export function fetchTrendingSongs(): Promise<{ songs: Song[] }> {
  if (TrendingBridge?.getTrendingIndia) {
    if (!_trendingCache) {
      _trendingCache = TrendingBridge.getTrendingIndia()
        .then((songs: Song[]) => ({ songs }))
        .catch(() => ({ songs: [] as Song[] }));
    }
    return _trendingCache;
  }
  return request('/api/songs/trending');
}

export function fetchFriendStats(friendId: string): Promise<import('./types').FriendStats> {
  return request(`/api/friends/${friendId}/stats`);
}

export function fetchLiveStreams(genre: string): Promise<{ songs: Song[] }> {
  if (!_liveCache[genre]) {
    _liveCache[genre] = request(`/api/songs/live?genre=${encodeURIComponent(genre)}`)
      .catch(() => ({ songs: [] as Song[] }));
  }
  return _liveCache[genre];
}

export function fetchAudioStream(videoId: string): Promise<{ audioUrl: string }> {
  return request(`/api/songs/audio/stream?videoId=${encodeURIComponent(videoId)}`);
}

export function fetchVideoStream(videoId: string): Promise<{ videoUrl: string }> {
  return request(`/api/songs/video/stream?videoId=${encodeURIComponent(videoId)}`);
}
