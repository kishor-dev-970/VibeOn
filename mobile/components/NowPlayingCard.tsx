import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { usePlayer } from '../context/PlayerContext';
import AudioSeekBar from './AudioSeekBar';
import GradientView from './GradientView';
import { Colors, BorderRadius, Spacing, Glass, Gradients, Shadows } from '../lib/theme';

export default function NowPlayingCard({ accent = Colors.primary }: { accent?: string }) {
  const { currentSong, isPlaying, togglePlayPause, playNext, playPrevious, stopPlaying } = usePlayer();

  if (!currentSong) return null;

  return (
    <View style={styles.card}>
      <GradientView colors={Gradients.brandMuted} style={StyleSheet.absoluteFill} />
      <View style={[styles.accent, { backgroundColor: accent }]} />

      <View style={styles.header}>
        <View style={[styles.artRing, { shadowColor: accent }]}>
          {!!currentSong.thumbnailUrl && (
            <Image source={{ uri: currentSong.thumbnailUrl }} style={styles.art} />
          )}
        </View>
        <View style={styles.meta}>
          <Text style={[styles.title, { color: Colors.text }]} numberOfLines={1}>
            {currentSong.title}
          </Text>
          <Text style={[styles.channel, { color: Colors.textMuted }]} numberOfLines={1}>
            {currentSong.channel}
          </Text>
        </View>
        <Pressable onPress={stopPlaying} hitSlop={12} style={styles.closeBtn}>
          <Text style={[styles.close, { color: Colors.textMuted }]}>✕</Text>
        </Pressable>
      </View>

      <AudioSeekBar trackColor={accent} />

      <View style={styles.controls}>
        <Pressable onPress={playPrevious} style={styles.controlBtn} hitSlop={10}>
          <Text style={[styles.controlIcon, { color: Colors.text }]}>⏮</Text>
        </Pressable>

        <Pressable
          onPress={togglePlayPause}
          style={({ pressed }) => [styles.playBtn, pressed && { opacity: 0.92 }]}
        >
          <GradientView colors={Gradients.play} style={styles.playGrad}>
            <Text style={styles.playIcon}>{isPlaying ? '❚❚' : '▶'}</Text>
          </GradientView>
        </Pressable>

        <Pressable onPress={playNext} style={styles.controlBtn} hitSlop={10}>
          <Text style={[styles.controlIcon, { color: Colors.text }]}>⏭</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    position: 'absolute',
    left: Spacing.sm,
    right: Spacing.sm,
    bottom: Spacing.sm,
    borderRadius: BorderRadius.xxl,
    paddingTop: Spacing.xs,
    paddingBottom: Spacing.md,
    borderWidth: 1,
    borderColor: Glass.border,
    backgroundColor: Glass.bgStrong,
    ...Shadows.soft,
    overflow: 'hidden',
  },
  accent: {
    width: 44,
    height: 4,
    borderRadius: 2,
    alignSelf: 'center',
    marginTop: Spacing.xs,
    marginBottom: Spacing.xs,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
    gap: Spacing.md,
  },
  artRing: {
    width: 52,
    height: 52,
    borderRadius: 14,
    padding: 2,
    backgroundColor: Glass.border,
    shadowOpacity: 0.4,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
    elevation: 4,
  },
  art: { width: 48, height: 48, borderRadius: 12, backgroundColor: Colors.bgCardLight },
  meta: { flex: 1 },
  title: { fontSize: 14, fontWeight: '800' },
  channel: { fontSize: 12, marginTop: 2 },
  closeBtn: { padding: Spacing.sm },
  close: { fontSize: 16, fontWeight: '700' },
  controls: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.xxl,
    paddingHorizontal: Spacing.lg,
    paddingTop: Spacing.xs,
  },
  controlBtn: {
    padding: Spacing.sm,
    alignItems: 'center',
    justifyContent: 'center',
  },
  controlIcon: {
    fontSize: 20,
    fontWeight: '700',
  },
  playBtn: {
    width: 50,
    height: 50,
    borderRadius: 25,
    alignItems: 'center',
    justifyContent: 'center',
    ...Shadows.glow,
  },
  playGrad: { width: 50, height: 50, borderRadius: 25, alignItems: 'center', justifyContent: 'center' },
  playIcon: { fontSize: 18, color: '#fff', fontWeight: '800' },
});
