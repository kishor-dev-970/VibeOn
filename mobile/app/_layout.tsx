import { Redirect, Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { PlayerProvider } from '../context/PlayerContext';

function RootNavigator() {
  const { loading, user } = useAuth();
  if (loading) {
    return null;
  }
  if (!user) {
    return <Redirect href="/login" />;
  }
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