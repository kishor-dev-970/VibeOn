import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { usePlayer } from '../context/PlayerContext';
import AudioSeekBar from './AudioSeekBar';
import GradientView from './GradientView';
import { Colors, BorderRadius, Spacing, Glass, Gradients, Shadows } from '../lib/theme';

export default function NowPlayingCard({ accent = Colors.primary }: { accent?: string }) {
  const { currentSong, isPlaying, togglePlayPause, stopPlaying } = usePlayer();

  if (!currentSong) return null;

  return (
    <View style={styles.card}>
      <GradientView colors={Gradients.brandMuted} style={StyleSheet.absoluteFill} />
      <View style={[styles.accent, { backgroundColor: accent }]} />

      <View style={styles.header}>
        <View style={styles.meta}>
          <Text style={[styles.label, { color: accent }]}>NOW PLAYING · AUDIO</Text>
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

      <View style={styles.body}>
        <View style={[styles.artRing, { shadowColor: accent }]}>
          {!!currentSong.thumbnailUrl && (
            <Image source={{ uri: currentSong.thumbnailUrl }} style={styles.art} />
          )}
        </View>
        <View style={styles.audioInfo}>
          <Text style={[styles.audioLabel, { color: Colors.text }]}>Ad-free audio</Text>
          <Text style={[styles.audioSub, { color: Colors.textMuted }]}>plays in background</Text>
        </View>
        <View style={[styles.liveDot, { backgroundColor: accent }]} />
      </View>

      <AudioSeekBar trackColor={accent} />

      <View style={styles.controls}>
        <Pressable
          onPress={togglePlayPause}
          style={({ pressed }) => [styles.playBtn, pressed && { opacity: 0.92 }]}
        >
          <GradientView colors={Gradients.play} style={styles.playGrad}>
            <Text style={styles.playIcon}>{isPlaying ? '❚❚' : '▶'}</Text>
          </GradientView>
        </Pressable>
      </View>

      <Text style={[styles.note, { color: Colors.textSubtle }]}>
        Your friends can see you listening to this
      </Text>
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
    marginTop: Spacing.sm,
    marginBottom: Spacing.xs,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.lg,
    paddingBottom: Spacing.sm,
  },
  meta: { flex: 1 },
  label: { fontSize: 10, fontWeight: '800', letterSpacing: 1.2, marginBottom: 3 },
  title: { fontSize: 15, fontWeight: '800' },
  channel: { fontSize: 12, marginTop: 2 },
  closeBtn: { padding: Spacing.sm },
  close: { fontSize: 18, fontWeight: '700' },
  body: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    paddingHorizontal: Spacing.lg,
    marginBottom: Spacing.sm,
  },
  artRing: {
    width: 70,
    height: 70,
    borderRadius: 18,
    padding: 2,
    backgroundColor: Glass.border,
    shadowOpacity: 0.5,
    shadowRadius: 14,
    shadowOffset: { width: 0, height: 4 },
    elevation: 8,
  },
  art: { width: 66, height: 66, borderRadius: 16, backgroundColor: Colors.bgCardLight },
  audioInfo: { flex: 1 },
  audioLabel: { fontSize: 14, fontWeight: '700' },
  audioSub: { fontSize: 12, marginTop: 2 },
  liveDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    shadowColor: '#000',
    shadowOpacity: 0.4,
    shadowRadius: 6,
    elevation: 6,
  },
  controls: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
  },
  playBtn: {
    width: 56,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
    ...Shadows.glow,
  },
  playGrad: { width: 56, height: 56, borderRadius: 28, alignItems: 'center', justifyContent: 'center' },
  playIcon: { fontSize: 20, color: '#fff', fontWeight: '800' },
  note: { fontSize: 11, textAlign: 'center', paddingBottom: Spacing.lg },
});
