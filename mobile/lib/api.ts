import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_URL } from './config';
import type { AppUser, FriendActivity, Song } from './types';

const TOKEN_KEY = 'auth.session.token';

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

export function fetchFriendStats(friendId: string): Promise<import('./types').FriendStats> {
  return request(`/api/friends/${friendId}/stats`);
}

