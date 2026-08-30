import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import type { Song } from '../lib/types';
import GradientView from './GradientView';
import { Colors, BorderRadius, Spacing, Glass, Gradients, Shadows } from '../lib/theme';

interface Props {
  song: Song;
  active?: boolean;
  onPress: () => void;
}

export default function SongItem({ song, active = false, onPress }: Props) {
  return (
    <Pressable
      style={({ pressed }) => [
        styles.row,
        { backgroundColor: active ? Glass.bg : 'transparent' },
        active && styles.active,
        pressed && styles.pressed,
      ]}
      onPress={onPress}
    >
      <View style={[styles.thumbWrap, active && { borderColor: Colors.primaryLight }]}>
        {!!song.thumbnailUrl && (
          <Image source={{ uri: song.thumbnailUrl }} style={styles.thumb} />
        )}
        {active && (
          <View style={styles.playOverlay}>
            <GradientView colors={Gradients.play} style={StyleSheet.absoluteFill} />
            <Text style={styles.playIcon}>❚❚</Text>
          </View>
        )}
      </View>

      <View style={styles.meta}>
        <Text
          style={[styles.title, { color: active ? Colors.primaryLight : Colors.text }]}
          numberOfLines={2}
        >
          {song.title}
        </Text>
        <Text style={[styles.channel, { color: Colors.textMuted }]} numberOfLines={1}>
          {song.channel}
        </Text>
      </View>

      {active ? (
        <View style={styles.eq}>
          <View style={[styles.eqBar, { backgroundColor: Colors.primaryLight }]} />
          <View style={[styles.eqBar, { backgroundColor: Colors.primaryLight, height: 14 }]} />
          <View style={[styles.eqBar, { backgroundColor: Colors.primaryLight }]} />
        </View>
      ) : (
        <View style={styles.playHint}>
          <Text style={styles.playHintIcon}>▶</Text>
        </View>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.md,
    gap: Spacing.md,
    borderRadius: BorderRadius.lg,
    marginHorizontal: Spacing.lg,
    marginBottom: Spacing.sm,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  pressed: { opacity: 0.7 },
  active: {
    borderColor: Glass.borderStrong,
    ...Shadows.glow,
  },
  thumbWrap: {
    width: 76,
    height: 52,
    borderRadius: BorderRadius.md,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    backgroundColor: Colors.bgCardLight,
  },
  thumb: {
    width: 76,
    height: 52,
  },
  playOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center',
  },
  playIcon: { color: '#fff', fontSize: 16, fontWeight: '800' },
  meta: {
    flex: 1,
    gap: 3,
  },
  title: {
    fontSize: 14,
    fontWeight: '700',
    lineHeight: 18,
  },
  channel: {
    fontSize: 12,
    fontWeight: '500',
  },
  playHint: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255,255,255,0.06)',
  },
  playHintIcon: { color: Colors.textMuted, fontSize: 12 },
  eq: { flexDirection: 'row', alignItems: 'center', gap: 3, width: 32, justifyContent: 'center' },
  eqBar: { width: 3, height: 8, borderRadius: 2 },
});
