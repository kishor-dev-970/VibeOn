import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  AppState,
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import YoutubePlayer from 'react-native-youtube-iframe';
import SongItem from '../../components/SongItem';
import * as api from '../../lib/api';
import type { Song } from '../../lib/types';
import { Colors, BorderRadius, Spacing } from '../../lib/theme';
import { usePlayer } from '../../context/PlayerContext';

type Genre = 'hindi' | 'punjabi' | 'english';

const GENRES: { key: Genre; label: string; emoji: string }[] = [
  { key: 'hindi', label: 'Hindi', emoji: '🇮🇳' },
  { key: 'punjabi', label: 'Punjabi', emoji: '🎵' },
  { key: 'english', label: 'English', emoji: '🎸' },
];

export default function LiveScreen() {
  const { currentSong, isPlaying, showVideo, setCurrentSong, setIsPlaying, setShowVideo } = usePlayer();
  const [genre, setGenre] = useState<Genre>('hindi');
  const [songs, setSongs] = useState<Song[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const playerRef = useRef<any>(null);
  const currentIndex = useRef(0);

  const loadStreams = useCallback(async (g: Genre) => {
    try {
      const data = await api.fetchLiveStreams(g);
      setSongs(data.songs);
    } catch {
      setSongs([]);
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    loadStreams(genre).finally(() => setLoading(false));
  }, [genre, loadStreams]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadStreams(genre);
    setRefreshing(false);
  }, [genre, loadStreams]);

  // Resume when coming to foreground
  useEffect(() => {
    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active' && currentSong && isPlaying) {
        try { playerRef.current?.playVideo(); } catch {}
      }
    });
    return () => sub.remove();
  }, [currentSong, isPlaying]);

  const playSong = async (song: Song, index: number) => {
    currentIndex.current = index;
    setCurrentSong(song);
    setIsPlaying(true);
    setShowVideo(true);
    try { await api.updateNowPlaying(song, true); } catch {}
  };

  const onPlayerStateChange = async (state: string) => {
    if (!currentSong) return;
    if (state === 'playing') {
      setIsPlaying(true);
      try { await api.updateNowPlaying(currentSong, true); } catch {}
    } else if (state === 'paused') {
      setIsPlaying(false);
      try { await api.updateNowPlaying(currentSong, false); } catch {}
    } else if (state === 'ended') {
      // Auto-play next song in the list
      const nextIndex = currentIndex.current + 1;
      if (nextIndex < songs.length) {
        playSong(songs[nextIndex], nextIndex);
      }
    }
  };

  const stopPlaying = async () => {
    setCurrentSong(null);
    setIsPlaying(false);
    setShowVideo(false);
    try { await api.clearNowPlaying(); } catch {}
  };

  const isLive = (s: Song) => s.videoId === currentSong?.videoId;

  return (
    <View style={styles.container}>
      <View style={styles.genreRow}>
        {GENRES.map((g) => (
          <Pressable
            key={g.key}
            style={[styles.genreChip, genre === g.key && styles.genreChipActive]}
            onPress={() => setGenre(g.key)}
          >
            <Text style={styles.genreEmoji}>{g.emoji}</Text>
            <Text style={[styles.genreLabel, genre === g.key && styles.genreLabelActive]}>
              {g.label}
            </Text>
          </Pressable>
        ))}
      </View>

      <Text style={styles.sectionLabel}>LIVE NOW</Text>

      {loading ? (
        <ActivityIndicator style={{ marginTop: 60 }} color={Colors.primary} />
      ) : (
        <FlatList
          data={songs}
          keyExtractor={(item) => item.videoId}
          renderItem={({ item, index }) => (
            <SongItem
              song={item}
              active={isLive(item)}
              onPress={() => playSong(item, index)}
            />
          )}
          ListEmptyComponent={
            <Text style={styles.empty}>No live streams found for {genre}</Text>
          }
          contentContainerStyle={{ paddingBottom: currentSong ? 320 : 24 }}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={Colors.primary} />
          }
        />
      )}

      {currentSong && (
        <View style={[styles.playerCard, { backgroundColor: Colors.bgCard, borderTopColor: Colors.border }]}>
          <View style={styles.playerAccent} />
          <View style={styles.playerHeader}>
            <View style={styles.playerMeta}>
              <Text style={[styles.nowPlayingLabel, { color: Colors.accent }]}>NOW PLAYING</Text>
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

          <YoutubePlayer
            ref={playerRef}
            height={200}
            videoId={currentSong.videoId}
            play={isPlaying}
            onChangeState={onPlayerStateChange}
          />

          <Text style={[styles.autoLabel, { color: Colors.textSubtle }]}>
            Auto-playing next stream
          </Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.bg },
  genreRow: {
    flexDirection: 'row',
    paddingHorizontal: Spacing.lg,
    paddingTop: Spacing.sm,
    gap: 10,
  },
  genreChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: BorderRadius.full,
    backgroundColor: Colors.bgCard,
  },
  genreChipActive: {
    backgroundColor: Colors.accent,
  },
  genreEmoji: { fontSize: 14 },
  genreLabel: { fontSize: 13, fontWeight: '600', color: Colors.textMuted },
  genreLabelActive: { color: Colors.text },
  sectionLabel: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1.2,
    marginHorizontal: Spacing.lg,
    marginTop: Spacing.lg,
    marginBottom: Spacing.sm,
    color: Colors.textMuted,
  },
  empty: { textAlign: 'center', marginTop: 60, fontSize: 14, color: Colors.textMuted },
  playerCard: {
    position: 'absolute', left: 0, right: 0, bottom: 0,
    borderTopLeftRadius: BorderRadius.xl, borderTopRightRadius: BorderRadius.xl,
    paddingTop: Spacing.xs, borderTopWidth: 1,
    shadowColor: '#000', shadowOpacity: 0.4, shadowRadius: 20,
    shadowOffset: { width: 0, height: -6 }, elevation: 12,
  },
  playerAccent: {
    width: 40, height: 4, borderRadius: 2,
    backgroundColor: Colors.accent, alignSelf: 'center',
    marginTop: Spacing.sm, marginBottom: Spacing.xs,
  },
  playerHeader: {
    flexDirection: 'row', alignItems: 'center',
    paddingHorizontal: Spacing.lg, paddingBottom: Spacing.sm,
  },
  playerMeta: { flex: 1 },
  nowPlayingLabel: { fontSize: 10, fontWeight: '700', letterSpacing: 1, marginBottom: 2 },
  playerTitle: { fontSize: 15, fontWeight: '700' },
  playerChannel: { fontSize: 12, marginTop: 1 },
  closeBtn: { padding: Spacing.sm },
  close: { fontSize: 18 },
  autoLabel: { fontSize: 11, textAlign: 'center', paddingBottom: Spacing.lg },
});
