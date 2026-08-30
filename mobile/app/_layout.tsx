import { Stack, usePathname, useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { PlayerProvider } from '../context/PlayerContext';
import * as api from '../lib/api';

function RootNavigator() {
  const { loading, user } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    api.fetchTrendingSongs().catch(() => {});
    api.fetchLiveStreams('hindi').catch(() => {});
  }, []);

  useEffect(() => {
    if (loading || user) return;
    if (pathname !== '/login') {
      router.replace('/login');
    }
  }, [loading, user, pathname, router]);

  if (loading) return null;
  return <Stack screenOptions={{ headerShown: false }} />;
}

export default function RootLayout() {
  return (
    <AuthProvider>
      <PlayerProvider>
        <StatusBar style="light" />
        <RootNavigator />
      </PlayerProvider>
    </AuthProvider>
  );
}
