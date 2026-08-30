import { useCallback, useEffect, useState } from 'react';
import {
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import MediaTabs from '../../components/MediaTabs';
import NowPlayingCard from '../../components/NowPlayingCard';
import GradientView from '../../components/GradientView';
import * as api from '../../lib/api';
import type { Song } from '../../lib/types';
import { Colors, BorderRadius, Spacing, Glass, Gradients, Shadows } from '../../lib/theme';
import { usePlayer } from '../../context/PlayerContext';

type Genre = 'hindi' | 'punjabi' | 'english';
type Mode = 'video' | 'audio';

const GENRES: { key: Genre; label: string }[] = [
  { key: 'hindi', label: 'Hindi' },
  { key: 'punjabi', label: 'Punjabi' },
  { key: 'english', label: 'English' },
];

export default function LiveScreen() {
  const { currentSong, playSong } = usePlayer();
  const [genre, setGenre] = useState<Genre>('hindi');
  const [songs, setSongs] = useState<Song[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async (g: Genre) => {
    try {
      const data = await api.fetchLiveStreams(g);
      setSongs(data.songs);
    } catch {
      setSongs([]);
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    load(genre).finally(() => setLoading(false));
  }, [genre, load]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await load(genre);
    setRefreshing(false);
  }, [genre, load]);

  const mode: Mode = 'audio';

  const displaySongs = songs;
  const activeSongId = currentSong?.videoId ?? null;

  const handlePlay = useCallback((song: Song) => {
    playSong(song, true);
  }, [playSong]);

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <GradientView colors={Gradients.brandMuted} style={styles.header}>
        <Text style={styles.brand}>VibeOn</Text>
        <Text style={styles.headerTitle}>Live</Text>
        <Text style={styles.headerSub}>Trending audio by genre</Text>
      </GradientView>

      <View style={styles.genreRow}>
        {GENRES.map((g) => {
          const active = genre === g.key;
          return (
            <Pressable
              key={g.key}
              onPress={() => setGenre(g.key)}
              style={({ pressed }) => [styles.genreChip, pressed && { opacity: 0.85 }]}
            >
              {active && <GradientView colors={Gradients.pink} style={StyleSheet.absoluteFill} />}
              <Text style={[styles.genreLabel, { color: active ? '#fff' : Colors.textMuted }]}>
                {g.label}
              </Text>
            </Pressable>
          );
        })}
      </View>

      <Text style={styles.sectionLabel}>LIVE · {genre.toUpperCase()}</Text>

      <MediaTabs
        mode={mode}
        showToggle={false}
        songs={displaySongs}
        loading={loading}
        refreshing={refreshing}
        onRefresh={onRefresh}
        onPlay={handlePlay}
        activeSongId={activeSongId}
        emptyLabel={loading ? undefined : `No live ${mode} found for ${genre} — pull to refresh`}
        loadingLabel="Finding live audio…"
      />

      <NowPlayingCard accent={Colors.accent} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.bg, position: 'relative' },
  header: {
    paddingHorizontal: Spacing.lg,
    paddingTop: Spacing.md,
    paddingBottom: Spacing.lg,
    borderBottomLeftRadius: BorderRadius.xl,
    borderBottomRightRadius: BorderRadius.xl,
  },
  brand: {
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 2,
    color: Colors.primaryLight,
    textTransform: 'uppercase',
    opacity: 0.9,
  },
  headerTitle: { fontSize: 30, fontWeight: '800', color: Colors.text, marginTop: 2 },
  headerSub: { fontSize: 13, color: Colors.textMuted, marginTop: 4, fontWeight: '500' },
  genreRow: {
    flexDirection: 'row',
    paddingHorizontal: Spacing.lg,
    paddingTop: Spacing.md,
    gap: 10,
  },
  genreChip: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 12,
    borderRadius: BorderRadius.lg,
    backgroundColor: Glass.bg,
    borderWidth: 1,
    borderColor: Glass.border,
    overflow: 'hidden',
  },
  genreLabel: { fontSize: 13, fontWeight: '800', letterSpacing: 0.3 },
  sectionLabel: {
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 1.4,
    marginHorizontal: Spacing.lg,
    marginTop: Spacing.lg,
    marginBottom: Spacing.sm,
    color: Colors.textMuted,
  },
});
