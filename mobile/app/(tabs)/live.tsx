import { useCallback, useEffect, useState } from 'react';
import {
  Image,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { AdFreeVideoPlayer } from '../../components/AdFreeVideoPlayer';
import { useRouter } from 'expo-router';
import MediaTabs from '../../components/MediaTabs';
import AudioSeekBar from '../../components/AudioSeekBar';
import * as api from '../../lib/api';
import type { Song } from '../../lib/types';
import { Colors, BorderRadius, Spacing } from '../../lib/theme';
import { usePlayer } from '../../context/PlayerContext';

type Genre = 'hindi' | 'punjabi' | 'english';
type Mode = 'video' | 'audio';

const GENRES: { key: Genre; label: string; emoji: string }[] = [
  { key: 'hindi', label: 'Hindi', emoji: '🇮🇳' },
  { key: 'punjabi', label: 'Punjabi', emoji: '🎵' },
  { key: 'english', label: 'English', emoji: '🎸' },
];

export default function LiveScreen() {
  const router = useRouter();
  const { currentSong, isPlaying, showVideo, videoScreenActive, setShowVideo, playSong, togglePlayPause, stopPlaying, setIsPlaying, youtubeRef } = usePlayer();
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

  const isAudioMode = !showVideo;
  const mode: Mode = showVideo ? 'video' : 'audio';

  const displaySongs = songs;
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

      <Text style={styles.sectionLabel}>LIVE / TRENDING BY GENRE</Text>

      <MediaTabs
        mode={mode}
        onModeChange={(m) => setShowVideo(m === 'video')}
        songs={displaySongs}
        loading={loading}
        refreshing={refreshing}
        onRefresh={onRefresh}
        onPlay={handlePlay}
        activeSongId={activeSongId}
        emptyLabel={`No ${mode} found for ${genre}`}
      />

      {currentSong && (
        <View style={[styles.playerCard, { backgroundColor: Colors.bgCard, borderTopColor: Colors.border }]}>
          <View style={styles.playerAccent} />
          <View style={styles.playerHeader}>
            <View style={styles.playerMeta}>
              <Text style={[styles.nowPlayingLabel, { color: Colors.accent }]}>
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
            <AdFreeVideoPlayer
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

          {isAudioMode && <AudioSeekBar trackColor={Colors.accent} />}

          <View style={styles.controlsRow}>
            <Pressable onPress={togglePlayPause} style={[styles.playPauseBtn, { backgroundColor: Colors.accent }]}>
              <Text style={styles.playPauseText}>{isPlaying ? '❚❚' : '▶'}</Text>
            </Pressable>
          </View>

          <Text style={[styles.listeningNote, { color: Colors.textSubtle }]}>
            Your friends can see you listening to this
          </Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.bg },
  genreRow: {
    flexDirection: 'row', paddingHorizontal: Spacing.lg, paddingTop: Spacing.sm, gap: 10,
  },
  genreChip: {
    flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 14, paddingVertical: 8,
    borderRadius: BorderRadius.full, backgroundColor: Colors.bgCard,
  },
  genreChipActive: { backgroundColor: Colors.accent },
  genreEmoji: { fontSize: 14 },
  genreLabel: { fontSize: 13, fontWeight: '600', color: Colors.textMuted },
  genreLabelActive: { color: Colors.text },
  sectionLabel: {
    fontSize: 11, fontWeight: '700', letterSpacing: 1.2,
    marginHorizontal: Spacing.lg, marginTop: Spacing.lg, marginBottom: Spacing.sm, color: Colors.textMuted,
  },
  playerCard: {
    position: 'absolute', left: 0, right: 0, bottom: 0,
    borderTopLeftRadius: BorderRadius.xl, borderTopRightRadius: BorderRadius.xl,
    paddingTop: Spacing.xs, borderTopWidth: 1,
    shadowColor: '#000', shadowOpacity: 0.4, shadowRadius: 20,
    shadowOffset: { width: 0, height: -6 }, elevation: 12,
  },
  playerAccent: {
    width: 40, height: 4, borderRadius: 2, backgroundColor: Colors.accent,
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
