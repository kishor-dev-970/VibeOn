import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import type { Song } from '../lib/types';
import { Colors, BorderRadius, Spacing } from '../lib/theme';

interface Props {
  song: Song;
  active?: boolean;
  onPress: () => void;
}

export default function SongItem({ song, active = false, onPress }: Props) {
  return (
    <Pressable
      style={[
        styles.row,
        { backgroundColor: active ? Colors.bgCardLight : Colors.bgCard },
        active && styles.active,
      ]}
      onPress={onPress}
    >
      {!!song.thumbnailUrl && (
        <Image source={{ uri: song.thumbnailUrl }} style={styles.thumb} />
      )}
      <View style={styles.meta}>
        <Text style={[styles.title, { color: active ? Colors.primaryLight : Colors.text }]} numberOfLines={2}>
          {song.title}
        </Text>
        <Text style={[styles.channel, { color: Colors.textMuted }]} numberOfLines={1}>
          {song.channel}
        </Text>
      </View>
      {active && <Text style={[styles.badge, { color: Colors.primary }]}>Playing</Text>}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.md,
    gap: Spacing.md,
    borderRadius: BorderRadius.md,
    marginHorizontal: Spacing.lg,
    marginBottom: Spacing.sm,
  },
  active: {
    borderWidth: 1,
    borderColor: Colors.primary,
  },
  thumb: {
    width: 72,
    height: 48,
    borderRadius: BorderRadius.sm,
    backgroundColor: Colors.bgCardLight,
  },
  meta: {
    flex: 1,
    gap: 2,
  },
  title: {
    fontSize: 14,
    fontWeight: '600',
  },
  channel: {
    fontSize: 12,
  },
  badge: {
    fontSize: 11,
    fontWeight: '700',
  },
});
