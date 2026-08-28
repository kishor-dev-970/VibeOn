import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { AdFreeVideoPlayer } from '../components/AdFreeVideoPlayer';
import { YouTubeWebPlayer } from '../components/YouTubeWebPlayer';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as api from '../lib/api';
import type { Song } from '../lib/types';
import { Colors, BorderRadius, Spacing } from '../lib/theme';
import { usePlayer } from '../context/PlayerContext';
import SongItem from '../components/SongItem';

export default function VideoPlayerScreen() {
  const params = useLocalSearchParams<{
    videoId: string;
    title?: string;
    channel?: string;
    thumbnailUrl?: string;
  }>();
  const router = useRouter();
  const {
    currentSong,
    isPlaying,
    setCurrentSong,
    setIsPlaying,
    stopPlaying,
    registerVideoPlayer,
    unregisterVideoPlayer,
    prefetchAudio,
  } = usePlayer();
  const youtubeRef = useRef<any>(null);

  // Hand over webview controls to the player context so screen-off can
  // switch to background audio and, on return, resume the video.
  useEffect(() => {
    registerVideoPlayer({
      getCurrentTime: () => youtubeRef.current?.getCurrentTime?.() ?? 0,
      pause: () => youtubeRef.current?.pauseVideo?.(),
      seekToPlay: (seconds: number) => youtubeRef.current?.seekTo?.(seconds, true),
    });
    return () => unregisterVideoPlayer();
  }, [registerVideoPlayer, unregisterVideoPlayer]);
  const [suggested, setSuggested] = useState<Song[]>([]);
  const [loadingSuggestions, setLoadingSuggestions] = useState(true);
  const [fallbackToStream, setFallbackToStream] = useState(false);

  const song: Song = {
    videoId: params.videoId,
    title: params.title ?? currentSong?.title ?? 'Now playing',
    channel: params.channel ?? currentSong?.channel ?? '',
    thumbnailUrl: params.thumbnailUrl ?? currentSong?.thumbnailUrl ?? '',
  };

  // Register in player context so now-playing updates for friends
  useEffect(() => {
    setCurrentSong(song);
    setIsPlaying(true);
    try { api.updateNowPlaying(song, true); } catch {}
    prefetchAudio(song);
    return () => {
      try { api.updateNowPlaying(song, false); } catch {}
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [song.videoId]);

  const loadSuggestions = useCallback(async () => {
    try {
      const data = await api.fetchTrendingSongs();
      const filtered = data.songs.filter((s) => s.videoId !== song.videoId);
      setSuggested(filtered);
    } catch {}
    finally {
      setLoadingSuggestions(false);
    }
  }, [song.videoId]);

  useEffect(() => {
    loadSuggestions();
  }, [loadSuggestions]);

  const playSuggested = (item: Song) => {
    setCurrentSong(item);
    setIsPlaying(true);
    try { api.updateNowPlaying(item, true); } catch {}
    router.setParams({
      videoId: item.videoId,
      title: item.title,
      channel: item.channel,
      thumbnailUrl: item.thumbnailUrl,
    });
  };

  const handleEnd = () => {
    setIsPlaying(false);
    stopPlaying();
    router.back();
  };

  return (
    <SafeAreaView style={styles.container} edges={['top', 'bottom']}>
      <View style={styles.topBar}>
        <Pressable onPress={() => router.back()} hitSlop={12} style={styles.backBtn}>
          <Text style={[styles.backLabel, { color: Colors.primary }]}>← Back</Text>
        </Pressable>
        <Text style={[styles.nowPlaying, { color: Colors.textMuted }]} numberOfLines={1}>
          Now Playing
        </Text>
        <View style={{ width: 60 }} />
      </View>

      <View style={styles.videoWrap}>
        {fallbackToStream ? (
          <AdFreeVideoPlayer
            ref={youtubeRef}
            height={220}
            videoId={song.videoId}
            play={isPlaying}
            onChangeState={(state: string) => {
              if (state === 'playing') setIsPlaying(true);
              else if (state === 'paused') setIsPlaying(false);
              else if (state === 'ended') handleEnd();
            }}
          />
        ) : (
          <YouTubeWebPlayer
            ref={youtubeRef}
            height={220}
            videoId={song.videoId}
            play={isPlaying}
            onChangeState={(state) => {
              if (state === 'playing') setIsPlaying(true);
              else if (state === 'paused') setIsPlaying(false);
              else if (state === 'ended') handleEnd();
            }}
            onFallbackToStream={() => setFallbackToStream(true)}
          />
        )}
      </View>

      <View style={styles.meta}>
        <Text style={[styles.title, { color: Colors.text }]} numberOfLines={2}>
          {song.title}
        </Text>
        <Text style={[styles.channel, { color: Colors.textMuted }]} numberOfLines={1}>
          {song.channel}
        </Text>
      </View>

      <Text style={[styles.sectionLabel, { color: Colors.textMuted }]}>SUGGESTED</Text>

      {loadingSuggestions ? (
        <ActivityIndicator style={{ marginTop: 30 }} color={Colors.primary} />
      ) : (
        <FlatList
          data={suggested}
          keyExtractor={(item) => item.videoId}
          renderItem={({ item }) => (
            <SongItem
              song={item}
              active={currentSong?.videoId === item.videoId}
              onPress={() => playSuggested(item)}
            />
          )}
          ListEmptyComponent={
            <Text style={[styles.empty, { color: Colors.textMuted }]}>No suggestions available</Text>
          }
          contentContainerStyle={{ paddingBottom: 24, paddingHorizontal: Spacing.md }}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
  },
  backBtn: {
    width: 60,
  },
  backLabel: {
    fontSize: 16,
    fontWeight: '600',
  },
  nowPlaying: {
    fontSize: 13,
    fontWeight: '600',
  },
  videoWrap: {
    marginHorizontal: Spacing.lg,
    borderRadius: BorderRadius.md,
    overflow: 'hidden',
    backgroundColor: '#000',
  },
  meta: {
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.md,
  },
  title: {
    fontSize: 17,
    fontWeight: '700',
  },
  channel: {
    fontSize: 13,
    marginTop: 4,
  },
  sectionLabel: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 1.2,
    paddingHorizontal: Spacing.lg,
    paddingBottom: Spacing.sm,
  },
  empty: {
    textAlign: 'center',
    marginTop: 20,
    fontSize: 14,
  },
});