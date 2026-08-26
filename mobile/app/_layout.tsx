import { Stack, useRouter, useSegments } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { View } from 'react-native';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { PlayerProvider } from '../context/PlayerContext';

function Gate() {
  const { token, loading } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    const inLogin = segments[0] === 'login';
    if (!token && !inLogin) {
      router.replace('/login');
    } else if (token && inLogin) {
      router.replace('/(tabs)');
    }
  }, [token, loading, segments, router]);

  if (loading) {
    return <View style={{ flex: 1, backgroundColor: '#0F0A1F' }} />;
  }
  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="settings" options={{ headerShown: false, presentation: 'modal' }} />
    </Stack>
  );
}

export default function RootLayout() {
  return (
    <AuthProvider>
      <PlayerProvider>
        <StatusBar style="light" />
        <Gate />
      </PlayerProvider>
    </AuthProvider>
  );
}
