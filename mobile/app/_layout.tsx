import { Stack, usePathname, useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { PlayerProvider, usePlayer } from '../context/PlayerContext';
import UpdatePrompt from '../components/UpdatePrompt';
import GradientView from '../components/GradientView';
import * as api from '../lib/api';
import { Colors, Gradients } from '../lib/theme';

// While the whole activity is shown in the Android Picture-in-Picture window,
// cover the app with a compact audio player so the PiP window looks like a
// mini player instead of a shrunk-down app.
function PipOverlay() {
  const { inPipMode, currentSong, isPlaying, togglePlayPause, audioCurrentTime, audioDuration } =
    usePlayer();
  if (!inPipMode || !currentSong) return null;
  const progress = audioDuration > 0 ? Math.min(1, audioCurrentTime / audioDuration) : 0;
  return (
    <View style={StyleSheet.absoluteFill}>
      <GradientView colors={Gradients.brandMuted} style={StyleSheet.absoluteFill} />
      <View style={styles.pipBody}>
        {!!currentSong.thumbnailUrl && (
          <Image source={{ uri: currentSong.thumbnailUrl }} style={styles.pipArt} />
        )}
        <Text style={styles.pipTitle} numberOfLines={2}>
          {currentSong.title}
        </Text>
        <Text style={styles.pipChannel} numberOfLines={1}>
          {currentSong.channel}
        </Text>
        <View style={styles.pipProgressTrack}>
          <View style={[styles.pipProgressFill, { width: `${Math.round(progress * 100)}%` }]} />
        </View>
        <Pressable onPress={togglePlayPause} style={styles.pipPlayBtn}>
          <GradientView colors={Gradients.play} style={styles.pipPlayGrad}>
            <Text style={styles.pipPlayIcon}>{isPlaying ? '❚❚' : '▶'}</Text>
          </GradientView>
        </Pressable>
      </View>
    </View>
  );
}

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
  return (
    <>
      <Stack screenOptions={{ headerShown: false }} />
      <UpdatePrompt />
      <PipOverlay />
    </>
  );
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

const styles = StyleSheet.create({
  pipBody: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  pipArt: {
    width: 72,
    height: 72,
    borderRadius: 16,
    marginBottom: 10,
    backgroundColor: Colors.bgCardLight,
  },
  pipTitle: {
    color: Colors.text,
    fontSize: 13,
    fontWeight: '800',
    textAlign: 'center',
  },
  pipChannel: {
    color: Colors.textMuted,
    fontSize: 10,
    marginTop: 2,
    textAlign: 'center',
  },
  pipProgressTrack: {
    width: '80%',
    height: 3,
    borderRadius: 2,
    backgroundColor: 'rgba(255,255,255,0.18)',
    marginTop: 10,
    overflow: 'hidden',
  },
  pipProgressFill: {
    height: 3,
    borderRadius: 2,
    backgroundColor: Colors.primaryLight,
  },
  pipPlayBtn: {
    marginTop: 12,
    width: 48,
    height: 48,
    borderRadius: 24,
    overflow: 'hidden',
  },
  pipPlayGrad: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pipPlayIcon: {
    fontSize: 18,
    color: '#fff',
    fontWeight: '800',
  },
});