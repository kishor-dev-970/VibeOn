import { useEffect, useState } from 'react';
import {
  ActivityIndicator, FlatList, Image, Pressable, StyleSheet, Text, View,
} from 'react-native';
import { Colors, BorderRadius, Spacing } from '../lib/theme';
import * as api from '../lib/api';
import type { FriendStats, NowPlaying } from '../lib/types';

interface Props {
  friendId: string;
  friendName: string;
  friendCode: string;
  darkMode: boolean;
  onClose: () => void;
  onPlaySong: (song: { videoId: string; title: string; channel: string; thumbnailUrl: string }) => void;
  onPlayCurrent?: (song: NowPlaying, audioMode?: boolean) => void;
}

const formatLastSeen = (iso?: string | null): string => {
  if (!iso) return 'Unknown';
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return 'Unknown';
  const d = new Date(t);
  return d.toLocaleString([], {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export default function FriendDetailModal({ friendId, friendName, friendCode, darkMode, onClose, onPlaySong, onPlayCurrent }: Props) {
  const [stats, setStats] = useState<FriendStats | null>(null);
  const [loading, setLoading] = useState(true);

  const bg = darkMode ? 'rgba(15,10,31,0.97)' : 'rgba(248,249,252,0.97)';
  const cardBg = darkMode ? Colors.bgCard : '#FFFFFF';
  const textColor = darkMode ? Colors.text : '#111827';
  const mutedColor = darkMode ? Colors.textMuted : '#6B7280';

  useEffect(() => {
    api.fetchFriendStats(friendId)
      .then(setStats)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [friendId]);

  const formatTime = (minutes: number) => {
    if (minutes < 60) return `${minutes}m`;
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return m > 0 ? `${h}h ${m}m` : `${h}h`;
  };

  return (
    <View style={[styles.overlay, { backgroundColor: bg }]}>
      <View style={styles.header}>
        <Pressable onPress={onClose} hitSlop={12}>
          <Text style={[styles.closeBtn, { color: Colors.primary }]}>✕ Close</Text>
        </Pressable>
        <View style={{ alignItems: 'center' }}>
          <Text style={[styles.title, { color: textColor }]} numberOfLines={1}>{friendName}</Text>
          {friendCode ? <Text style={[styles.code, { color: mutedColor }]}>Code: {friendCode}</Text> : null}
          {stats ? (
            stats.nowPlaying?.isPlaying ? (
              <Text style={[styles.listeningNow, { color: Colors.success }]}>
                ● Listening now · {formatLastSeen(stats.lastSeen)}
              </Text>
            ) : (
              <Text style={[styles.code, { color: mutedColor }]}>
                Last seen {formatLastSeen(stats.lastSeen)}
              </Text>
            )
          ) : null}
        </View>
        <View style={{ width: 70 }} />
      </View>

      {loading ? (
        <ActivityIndicator style={{ marginTop: 80 }} color={Colors.primary} />
      ) : stats ? (
        (() => {
          type ModalRow = {
            key: string;
            videoId: string;
            title: string;
            channel: string;
            thumbnailUrl: string;
            playCount?: number;
            recent: boolean;
          };
          const rows: ModalRow[] = [
            ...stats.recentPlays.map((p) => ({
              key: `r-${p.videoId}-${p.playedAt}`,
              videoId: p.videoId,
              title: p.title,
              channel: p.channel,
              thumbnailUrl: p.thumbnailUrl,
              recent: true,
            })),
            ...stats.songsListened.map((s) => ({
              key: `t-${s.videoId}`,
              videoId: s.videoId,
              title: s.title,
              channel: s.channel,
              thumbnailUrl: s.thumbnailUrl,
              playCount: s.playCount,
              recent: false,
            })),
          ];
          return (
            <View style={styles.content}>
              {stats.nowPlaying ? (
                <View style={[styles.nowPlayingCard, { backgroundColor: cardBg }]}>
                  <Text style={[styles.nowPlayingLabel, { color: mutedColor }]}>
                    {stats.nowPlaying.isPlaying ? 'LISTENING NOW' : 'LAST PLAYED'}
                  </Text>
                  <View style={styles.nowPlayingRow}>
                    {!!stats.nowPlaying.thumbnailUrl && (
                      <Image source={{ uri: stats.nowPlaying.thumbnailUrl }} style={styles.thumb} />
                    )}
                    <View style={styles.songMeta}>
                      <Text style={[styles.songTitle, { color: textColor }]} numberOfLines={1}>
                        {stats.nowPlaying.title}
                      </Text>
                      <Text style={[styles.songChannel, { color: mutedColor }]} numberOfLines={1}>
                        {stats.nowPlaying.channel}
                      </Text>
                    </View>
                  </View>
                  {onPlayCurrent ? (
                    <View style={styles.playRow}>
                      <Pressable
                        style={[styles.playBtn, { backgroundColor: Colors.primary }]}
                        onPress={() => onPlayCurrent(stats.nowPlaying!)}
                      >
                        <Text style={styles.playBtnText}>▶ Video</Text>
                      </Pressable>
                      <Pressable
                        style={[styles.playBtn, { backgroundColor: Colors.accent }]}
                        onPress={() => onPlayCurrent(stats.nowPlaying!, true)}
                      >
                        <Text style={styles.playBtnText}>🎵 Audio</Text>
                      </Pressable>
                    </View>
                  ) : null}
                </View>
              ) : null}

              <View style={styles.statsRow}>
                <View style={[styles.statCard, { backgroundColor: cardBg }]}>
                  <Text style={[styles.statValue, { color: Colors.primary }]}>{stats.totalSongs}</Text>
                  <Text style={[styles.statLabel, { color: mutedColor }]}>Songs played</Text>
                </View>
                <View style={[styles.statCard, { backgroundColor: cardBg }]}>
                  <Text style={[styles.statValue, { color: Colors.secondary }]}>{stats.totalPlays}</Text>
                  <Text style={[styles.statLabel, { color: mutedColor }]}>Plays</Text>
                </View>
                <View style={[styles.statCard, { backgroundColor: cardBg }]}>
                  <Text style={[styles.statValue, { color: Colors.accent }]}>{formatTime(stats.estimatedMinutes)}</Text>
                  <Text style={[styles.statLabel, { color: mutedColor }]}>Listened</Text>
                </View>
              </View>

              <FlatList
                data={rows}
                keyExtractor={(item) => item.key}
                ListHeaderComponent={
                  <>
                    <Text style={[styles.sectionTitle, { color: mutedColor }]}>RECENT PLAYS</Text>
                    {stats.recentPlays.length === 0 ? (
                      <Text style={[styles.none, { color: mutedColor }]}>No recent activity yet</Text>
                    ) : null}
                    <Text style={[styles.sectionTitle, { color: mutedColor }]}>TOP SONGS THIS WEEK</Text>
                  </>
                }
                renderItem={({ item }) => (
                  <Pressable style={[styles.songRow, { backgroundColor: cardBg }]} onPress={() => onPlaySong(item)}>
                    <Image source={{ uri: item.thumbnailUrl }} style={styles.thumb} />
                    <View style={styles.songMeta}>
                      <Text style={[styles.songTitle, { color: textColor }]} numberOfLines={1}>{item.title}</Text>
                      <Text style={[styles.songChannel, { color: mutedColor }]} numberOfLines={1}>{item.channel}</Text>
                    </View>
                    {!item.recent ? (
                      <Text style={[styles.playCount, { color: Colors.primaryLight }]}>{item.playCount}x</Text>
                    ) : null}
                  </Pressable>
                )}
                contentContainerStyle={{ paddingBottom: 40 }}
              />
            </View>
          );
        })()
      ) : (
        <Text style={[styles.empty, { color: mutedColor }]}>No listening history yet</Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 100,
  },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: Spacing.lg, paddingTop: 56, paddingBottom: Spacing.md,
  },
  closeBtn: { fontSize: 16, fontWeight: '600' },
  title: { fontSize: 18, fontWeight: '700' },
  code: { fontSize: 12, marginTop: 2 },
  listeningNow: { fontSize: 12, fontWeight: '700', marginTop: 2 },
  content: { flex: 1, paddingHorizontal: Spacing.lg },
  nowPlayingCard: { borderRadius: BorderRadius.lg, padding: Spacing.md, marginBottom: Spacing.md },
  nowPlayingLabel: { fontSize: 10, fontWeight: '700', letterSpacing: 1, marginBottom: Spacing.sm },
  nowPlayingRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  playRow: { flexDirection: 'row', gap: 10, marginTop: Spacing.md },
  playBtn: { flex: 1, alignItems: 'center', paddingVertical: 10, borderRadius: BorderRadius.md },
  playBtnText: { color: '#FFFFFF', fontSize: 13, fontWeight: '700' },
  statsRow: { flexDirection: 'row', gap: 10, marginBottom: Spacing.xl },
  statCard: { flex: 1, borderRadius: BorderRadius.lg, padding: Spacing.lg, alignItems: 'center' },
  statValue: { fontSize: 24, fontWeight: '800' },
  statLabel: { fontSize: 12, marginTop: 4, fontWeight: '500' },
  sectionTitle: { fontSize: 12, fontWeight: '700', letterSpacing: 1, marginBottom: Spacing.md, marginTop: Spacing.md },
  songRow: { flexDirection: 'row', alignItems: 'center', borderRadius: BorderRadius.md, padding: Spacing.md, marginBottom: Spacing.sm, gap: 12 },
  thumb: { width: 56, height: 56, borderRadius: BorderRadius.sm, backgroundColor: '#2D2550' },
  songMeta: { flex: 1 },
  songTitle: { fontSize: 14, fontWeight: '600' },
  songChannel: { fontSize: 12, marginTop: 2 },
  playCount: { fontSize: 14, fontWeight: '700' },
  none: { fontSize: 13, marginBottom: Spacing.md },
  empty: { textAlign: 'center', marginTop: 80, fontSize: 14 },
});
