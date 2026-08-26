import { useEffect, useState } from 'react';
import {
  ActivityIndicator, FlatList, Image, Pressable, StyleSheet, Text, View,
} from 'react-native';
import { Colors, BorderRadius, Spacing } from '../lib/theme';
import * as api from '../lib/api';

interface SongStat {
  videoId: string;
  title: string;
  channel: string;
  thumbnailUrl: string;
  playCount: number;
}

interface FriendStats {
  totalSongs: number;
  totalPlays: number;
  estimatedMinutes: number;
  songsListened: SongStat[];
}

interface Props {
  friendId: string;
  friendName: string;
  darkMode: boolean;
  onClose: () => void;
  onPlaySong: (song: { videoId: string; title: string; channel: string; thumbnailUrl: string }) => void;
}

export default function FriendDetailModal({ friendId, friendName, darkMode, onClose, onPlaySong }: Props) {
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
        <Text style={[styles.title, { color: textColor }]} numberOfLines={1}>{friendName}</Text>
        <View style={{ width: 70 }} />
      </View>

      {loading ? (
        <ActivityIndicator style={{ marginTop: 80 }} color={Colors.primary} />
      ) : stats ? (
        <View style={styles.content}>
          <View style={styles.statsRow}>
            <View style={[styles.statCard, { backgroundColor: cardBg }]}>
              <Text style={[styles.statValue, { color: Colors.primary }]}>{stats.totalSongs}</Text>
              <Text style={[styles.statLabel, { color: mutedColor }]}>Songs</Text>
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
          <Text style={[styles.sectionTitle, { color: mutedColor }]}>TOP SONGS THIS WEEK</Text>
          <FlatList
            data={stats.songsListened}
            keyExtractor={(item) => item.videoId}
            renderItem={({ item }) => (
              <Pressable style={[styles.songRow, { backgroundColor: cardBg }]} onPress={() => onPlaySong(item)}>
                <Image source={{ uri: item.thumbnailUrl }} style={styles.thumb} />
                <View style={styles.songMeta}>
                  <Text style={[styles.songTitle, { color: textColor }]} numberOfLines={1}>{item.title}</Text>
                  <Text style={[styles.songChannel, { color: mutedColor }]} numberOfLines={1}>{item.channel}</Text>
                </View>
                <Text style={[styles.playCount, { color: Colors.primaryLight }]}>{item.playCount}x</Text>
              </Pressable>
            )}
            contentContainerStyle={{ paddingBottom: 40 }}
          />
        </View>
      ) : (
        <Text style={[styles.empty, { color: mutedColor }]}>No listening history yet</Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: { flex: 1 },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: Spacing.lg, paddingTop: 56, paddingBottom: Spacing.md,
  },
  closeBtn: { fontSize: 16, fontWeight: '600' },
  title: { fontSize: 18, fontWeight: '700' },
  content: { flex: 1, paddingHorizontal: Spacing.lg },
  statsRow: { flexDirection: 'row', gap: 10, marginBottom: Spacing.xl },
  statCard: { flex: 1, borderRadius: BorderRadius.lg, padding: Spacing.lg, alignItems: 'center' },
  statValue: { fontSize: 24, fontWeight: '800' },
  statLabel: { fontSize: 12, marginTop: 4, fontWeight: '500' },
  sectionTitle: { fontSize: 12, fontWeight: '700', letterSpacing: 1, marginBottom: Spacing.md },
  songRow: { flexDirection: 'row', alignItems: 'center', borderRadius: BorderRadius.md, padding: Spacing.md, marginBottom: Spacing.sm, gap: 12 },
  thumb: { width: 56, height: 56, borderRadius: BorderRadius.sm, backgroundColor: '#2D2550' },
  songMeta: { flex: 1 },
  songTitle: { fontSize: 14, fontWeight: '600' },
  songChannel: { fontSize: 12, marginTop: 2 },
  playCount: { fontSize: 14, fontWeight: '700' },
  empty: { textAlign: 'center', marginTop: 80, fontSize: 14 },
});
