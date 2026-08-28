import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import YoutubePlayer from 'react-native-youtube-iframe';
import { useRouter } from 'expo-router';
import MediaTabs from '../../components/MediaTabs';
import AudioSeekBar from '../../components/AudioSeekBar';
import * as api from '../../lib/api';
import type { Song } from '../../lib/types';
import { Colors, BorderRadius, Spacing } from '../../lib/theme';
import { usePlayer } from '../../context/PlayerContext';

type Mode = 'video' | 'audio';

export default function HomeScreen() {
  const router = useRouter();
  const {
    currentSong,
    isPlaying,
    showVideo,
    videoScreenActive,
    setShowVideo,
    playSong,
    togglePlayPause,
    stopPlaying,
    setIsPlaying,
    youtubeRef,
  } = usePlayer();
  const [query, setQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Song[]>([]);
  const [trending, setTrending] = useState<Song[]>([]);
  const [searching, setSearching] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadTrending = useCallback(async () => {
    try {
      const data = await api.fetchTrendingSongs();
      setTrending(data.songs);
    } catch {}
  }, []);

  useEffect(() => {
    loadTrending();
  }, [loadTrending]);

  const doSearch = useCallback(async (q: string) => {
    if (q.trim().length < 2) {
      setSearchResults([]);
      return;
    }
    setSearching(true);
    try {
      const data = await api.searchSongs(q.trim());
      setSearchResults(data.songs);
    } catch {
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

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadTrending();
    setRefreshing(false);
  }, [loadTrending]);

  const isAudioMode = !showVideo;
  const mode: Mode = showVideo ? 'video' : 'audio';

  const isSearching = query.trim().length >= 2;
  const displaySongs = isSearching ? searchResults : trending;
  const activeSongId = currentSong?.videoId ?? null;

  const handlePlay = useCallback((song: Song) => {
    if (showVideo) {
      router.push({
        pathname: '/video-player',
        params: {
          videoId: song.videoId,
          title: song.title,
          channel: song.channel,
          thumbnailUrl: song.thumbnailUrl,
        },
      });
    } else {
      playSong(song, true);
    }
  }, [showVideo, router, playSong]);

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
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
          placeholder="Search songs..."
          placeholderTextColor={Colors.textSubtle}
          value={query}
          onChangeText={setQuery}
          autoCapitalize="none"
          returnKeyType="search"
          onSubmitEditing={() => doSearch(query)}
        />
      </View>

      <MediaTabs
        mode={mode}
        onModeChange={(m) => setShowVideo(m === 'video')}
        songs={displaySongs}
        loading={searching && isSearching}
        refreshing={refreshing}
        onRefresh={onRefresh}
        onPlay={handlePlay}
        activeSongId={activeSongId}
        emptyLabel={isSearching ? 'No songs found' : 'No trending songs yet'}
      />

      {currentSong && (
        <View style={[styles.playerCard, { backgroundColor: Colors.bgCard, borderTopColor: Colors.border }]}>
          <View style={styles.playerAccent} />
          <View style={styles.playerHeader}>
            <View style={styles.playerMeta}>
              <Text style={[styles.nowPlayingLabel, { color: Colors.primary }]}>
                {isAudioMode ? 'NOW PLAYING · AUDIO' : 'NOW PLAYING'}
              </Text>
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

          {showVideo ? (
            <YoutubePlayer
              ref={youtubeRef}
              height={200}
              videoId={currentSong.videoId}
              play={isPlaying && !videoScreenActive}
              onChangeState={(state: string) => {
                if (state === 'playing') setIsPlaying(true);
                else if (state === 'paused') setIsPlaying(false);
                else if (state === 'ended') stopPlaying();
              }}
            />
          ) : (
            <View style={styles.audioOnly}>
              {!!currentSong.thumbnailUrl && (
                <Image source={{ uri: currentSong.thumbnailUrl }} style={styles.artwork} />
              )}
              <Text style={styles.audioLabel}>
                {isAudioMode ? 'Ad-free audio · plays in background' : 'Playing audio'}
              </Text>
            </View>
          )}

          {isAudioMode && <AudioSeekBar />}

          <View style={styles.controlsRow}>
            <Pressable onPress={togglePlayPause} style={[styles.playPauseBtn, { backgroundColor: Colors.primary }]}>
              <Text style={styles.playPauseText}>{isPlaying ? '❚❚' : '▶'}</Text>
            </Pressable>
          </View>

          <Text style={[styles.listeningNote, { color: Colors.textSubtle }]}>
            Your friends can see you listening to this
          </Text>
        </View>
      )}
    </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.bg },
  flex: { flex: 1 },
  headerRow: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: Spacing.lg, paddingTop: Spacing.sm, paddingBottom: Spacing.md,
  },
  headerTitle: { fontSize: 28, fontWeight: '800', color: Colors.text },
  settingsIcon: { fontSize: 24 },
  searchBar: {
    flexDirection: 'row', alignItems: 'center', borderRadius: BorderRadius.md,
    marginHorizontal: Spacing.lg, marginBottom: Spacing.md, paddingHorizontal: Spacing.md, gap: 8,
  },
  searchIcon: { fontSize: 16 },
  searchInput: { flex: 1, paddingVertical: 12, fontSize: 15 },
  playerCard: {
    position: 'absolute', left: 0, right: 0, bottom: 0,
    borderTopLeftRadius: BorderRadius.xl, borderTopRightRadius: BorderRadius.xl,
    paddingTop: Spacing.xs, borderTopWidth: 1,
    shadowColor: '#000', shadowOpacity: 0.4, shadowRadius: 20,
    shadowOffset: { width: 0, height: -6 }, elevation: 12,
  },
  playerAccent: {
    width: 40, height: 4, borderRadius: 2, backgroundColor: Colors.primary,
    alignSelf: 'center', marginTop: Spacing.sm, marginBottom: Spacing.xs,
  },
  playerHeader: {
    flexDirection: 'row', alignItems: 'center', paddingHorizontal: Spacing.lg, paddingBottom: Spacing.sm,
  },
  playerMeta: { flex: 1 },
  nowPlayingLabel: { fontSize: 10, fontWeight: '700', letterSpacing: 1, marginBottom: 2 },
  playerTitle: { fontSize: 15, fontWeight: '700' },
  playerChannel: { fontSize: 12, marginTop: 1 },
  closeBtn: { padding: Spacing.sm },
  close: { fontSize: 18 },
  audioOnly: {
    alignItems: 'center', justifyContent: 'center', flexDirection: 'row', gap: 12, height: 90,
  },
  artwork: { width: 64, height: 64, borderRadius: BorderRadius.md, backgroundColor: Colors.bgCardLight },
  audioLabel: { fontSize: 12, fontWeight: '600', color: Colors.textMuted },
  controlsRow: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center',
    paddingHorizontal: Spacing.lg, paddingVertical: Spacing.sm,
  },
  playPauseBtn: {
    width: 48, height: 48, borderRadius: 24, alignItems: 'center', justifyContent: 'center',
  },
  playPauseText: { fontSize: 18, color: Colors.text },
  listeningNote: { fontSize: 11, textAlign: 'center', paddingBottom: Spacing.lg },
});
