import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  AppState,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import YoutubePlayer from 'react-native-youtube-iframe';
import { useRouter } from 'expo-router';
import SongItem from '../../components/SongItem';
import * as api from '../../lib/api';
import type { Song } from '../../lib/types';
import { Colors, BorderRadius, Spacing } from '../../lib/theme';
import { usePlayer } from '../../context/PlayerContext';

export default function HomeScreen() {
  const router = useRouter();
  const { currentSong, isPlaying, showVideo, setCurrentSong, setIsPlaying, setShowVideo, playFriendSong } = usePlayer();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Song[]>([]);
  const [trending, setTrending] = useState<Song[]>([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const playerRef = useRef<any>(null);

  const loadTrending = useCallback(async () => {
    try {
      const data = await api.fetchTrendingSongs();
      setTrending(data.songs);
    } catch {}
  }, []);

  useEffect(() => {
    loadTrending();
  }, [loadTrending]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadTrending();
    setRefreshing(false);
  }, [loadTrending]);

  const doSearch = useCallback(async (q: string) => {
    if (q.trim().length < 2) {
      setResults([]);
      setError(null);
      return;
    }
    setSearching(true);
    setError(null);
    try {
      const data = await api.searchSongs(q.trim());
      setResults(data.songs);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Search failed');
    } finally {
      setSearching(false);
    }
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => doSearch(query), 450);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query, doSearch]);

  // Resume playback when app comes back to foreground
  useEffect(() => {
    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active' && currentSong && isPlaying) {
        // YouTube iframe auto-resumes when app is active again
        try { playerRef.current?.playVideo(); } catch {}
      }
    });
    return () => sub.remove();
  }, [currentSong, isPlaying]);

  const playSong = async (song: Song) => {
    setCurrentSong(song);
    setIsPlaying(true);
    setShowVideo(false);
    try {
      await api.updateNowPlaying(song, true);
    } catch {}
  };

  const stopPlaying = async () => {
    setCurrentSong(null);
    setIsPlaying(false);
    setShowVideo(false);
    try {
      await api.clearNowPlaying();
    } catch {}
  };

  const onPlayerStateChange = async (state: string) => {
    if (!currentSong) return;
    if (state === 'playing') {
      setIsPlaying(true);
      try {
        await api.updateNowPlaying(currentSong, true);
      } catch {}
    } else if (state === 'paused') {
      setIsPlaying(false);
      try {
        await api.updateNowPlaying(currentSong, false);
      } catch {}
    } else if (state === 'ended') {
      await stopPlaying();
    }
  };

  const togglePlayPause = async () => {
    if (!currentSong) return;
    const next = !isPlaying;
    setIsPlaying(next);
    try {
      await api.updateNowPlaying(currentSong, next);
    } catch {}
  };

  const isSearching = query.trim().length >= 2;
  const displayData = isSearching ? results : trending;

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={styles.headerRow}>
        <Text style={styles.headerTitle}>Home</Text>
        <Pressable onPress={() => router.push('/settings')} hitSlop={12}>
          <Text style={styles.settingsIcon}>⚙️</Text>
        </Pressable>
      </View>

      <View style={[styles.searchBar, { backgroundColor: Colors.searchBg }]}>
        <Text style={styles.searchIcon}>🔍</Text>
        <TextInput
          style={[styles.searchInput, { color: Colors.text }]}
          placeholder="Search songs on YouTube..."
          placeholderTextColor={Colors.textSubtle}
          value={query}
          onChangeText={setQuery}
          autoCapitalize="none"
          returnKeyType="search"
          onSubmitEditing={() => doSearch(query)}
        />
      </View>

      {error && <Text style={[styles.error, { color: Colors.error }]}>{error}</Text>}

      <Text style={[styles.sectionLabel, { color: Colors.textMuted }]}>
        {isSearching ? 'SEARCH RESULTS' : 'TRENDING NOW'}
      </Text>

      <FlatList
        data={displayData}
        keyExtractor={(item) => item.videoId}
        renderItem={({ item }) => (
          <SongItem song={item} active={item.videoId === currentSong?.videoId} onPress={() => playSong(item)} />
        )}
        ListEmptyComponent={
          searching ? (
            <ActivityIndicator style={{ marginTop: 32 }} color={Colors.primary} />
          ) : isSearching ? (
            <Text style={[styles.empty, { color: Colors.textMuted }]}>No songs found</Text>
          ) : (
            <Text style={[styles.empty, { color: Colors.textMuted }]}>No trending songs yet</Text>
          )
        }
        contentContainerStyle={{ paddingBottom: currentSong ? 320 : 24 }}
        refreshControl={
          !isSearching ? (
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={Colors.primary} />
          ) : undefined
        }
      />

      {currentSong && (
        <View style={[styles.playerCard, { backgroundColor: Colors.bgCard, borderTopColor: Colors.border }]}>
          <View style={styles.playerAccent} />
          <View style={styles.playerHeader}>
            <View style={styles.playerMeta}>
              <Text style={[styles.nowPlayingLabel, { color: Colors.primary }]}>NOW PLAYING</Text>
              <Text style={[styles.playerTitle, { color: Colors.text }]} numberOfLines={1}>
                {currentSong.title}
              </Text>
              <Text style={[styles.playerChannel, { color: Colors.textMuted }]} numberOfLines={1}>
                {currentSong.channel}
              </Text>
            </View>
            <Pressable onPress={stopPlaying} hitSlop={12} style={styles.closeBtn}>
              <Text style={[styles.close, { color: Colors.textMuted }]}>✕</Text>
            </Pressable>
          </View>

          {showVideo && (
            <YoutubePlayer
              ref={playerRef}
              height={200}
              videoId={currentSong.videoId}
              play={isPlaying}
              onChangeState={onPlayerStateChange}
            />
          )}

          {!showVideo && (
            <View style={styles.audioOnly}>
              <Text style={styles.albumArt}>♫</Text>
              <Text style={[styles.audioLabel, { color: Colors.textMuted }]}>Audio Only</Text>
            </View>
          )}

          {/* Hidden player for audio-only mode - renders at 1px so audio still plays */}
          {!showVideo && (
            <View style={{ height: 1, overflow: 'hidden' }}>
              <YoutubePlayer
                ref={playerRef}
                height={1}
                videoId={currentSong.videoId}
                play={isPlaying}
                onChangeState={onPlayerStateChange}
              />
            </View>
          )}

          <View style={styles.controlsRow}>
            <Pressable onPress={togglePlayPause} style={[styles.playPauseBtn, { backgroundColor: Colors.primary }]}>
              <Text style={styles.playPauseText}>{isPlaying ? '❚❚' : '▶'}</Text>
            </Pressable>

            <Pressable
              onPress={() => setShowVideo(!showVideo)}
              style={[styles.videoToggle, { backgroundColor: showVideo ? Colors.secondary : Colors.bgCardLight }]}
            >
              <Text style={[styles.videoToggleText, { color: showVideo ? Colors.text : Colors.textMuted }]}>
                {showVideo ? 'Video ON' : 'Show Video'}
              </Text>
            </Pressable>
          </View>

          <Text style={[styles.listeningNote, { color: Colors.textSubtle }]}>
            Your friends can see you listening to this
          </Text>
        </View>
      )}
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.lg,
    paddingTop: Spacing.sm,
    paddingBottom: Spacing.md,
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: '800',
    color: Colors.text,
  },
  settingsIcon: {
    fontSize: 24,
  },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: BorderRadius.md,
    marginHorizontal: Spacing.lg,
    marginBottom: Spacing.md,
    paddingHorizontal: Spacing.md,
    gap: 8,
  },
  searchIcon: {
    fontSize: 16,
  },
  searchInput: {
    flex: 1,
    paddingVertical: 12,
    fontSize: 15,
  },
  error: {
    textAlign: 'center',
    marginBottom: 8,
    fontSize: 13,
  },
  sectionLabel: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1.2,
    marginHorizontal: Spacing.lg,
    marginBottom: Spacing.sm,
  },
  empty: {
    textAlign: 'center',
    marginTop: 40,
    fontSize: 14,
  },
  playerCard: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    borderTopLeftRadius: BorderRadius.xl,
    borderTopRightRadius: BorderRadius.xl,
    paddingTop: Spacing.xs,
    borderTopWidth: 1,
    shadowColor: '#000',
    shadowOpacity: 0.4,
    shadowRadius: 20,
    shadowOffset: { width: 0, height: -6 },
    elevation: 12,
  },
  playerAccent: {
    width: 40,
    height: 4,
    borderRadius: 2,
    backgroundColor: Colors.primary,
    alignSelf: 'center',
    marginTop: Spacing.sm,
    marginBottom: Spacing.xs,
  },
  playerHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.lg,
    paddingBottom: Spacing.sm,
  },
  playerMeta: {
    flex: 1,
  },
  nowPlayingLabel: {
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 1,
    marginBottom: 2,
  },
  playerTitle: {
    fontSize: 15,
    fontWeight: '700',
  },
  playerChannel: {
    fontSize: 12,
    marginTop: 1,
  },
  closeBtn: {
    padding: Spacing.sm,
  },
  close: {
    fontSize: 18,
  },
  audioOnly: {
    alignItems: 'center',
    justifyContent: 'center',
    height: 120,
  },
  albumArt: {
    fontSize: 48,
    color: Colors.primary,
  },
  audioLabel: {
    fontSize: 12,
    fontWeight: '600',
    marginTop: 8,
    letterSpacing: 0.5,
  },
  controlsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
  },
  playPauseBtn: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  playPauseText: {
    fontSize: 18,
    color: Colors.text,
  },
  videoToggle: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: BorderRadius.full,
  },
  videoToggleText: {
    fontSize: 13,
    fontWeight: '600',
  },
  listeningNote: {
    fontSize: 11,
    textAlign: 'center',
    paddingBottom: Spacing.lg,
  },
});
