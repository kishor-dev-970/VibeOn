import { Linking, Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import GradientView from './GradientView';
import { useUpdate } from '../lib/useUpdate';
import { Colors, BorderRadius, Spacing, Glass, Gradients, Shadows } from '../lib/theme';

export default function UpdatePrompt() {
  const { update, handled, dismiss, install } = useUpdate();
  if (!update || handled) return null;

  return (
    <Modal transparent visible animationType="fade" onRequestClose={dismiss}>
      <View style={styles.backdrop}>
        <GradientView colors={Gradients.brandMuted} style={StyleSheet.absoluteFill} />
        <View style={styles.sheet}>
          <View style={styles.badgeWrap}>
            <GradientView colors={Gradients.play} style={styles.badge}>
              <Text style={styles.badgeText}>NEW</Text>
            </GradientView>
          </View>
          <Text style={styles.title}>Update available</Text>
          <Text style={styles.subtitle}>
            Version <Text style={styles.version}>{update.latestVersion}</Text> of VibeOn is ready.
            Download and install it to keep listening with the latest fixes.
          </Text>
          {!!update.notes && (
            <ScrollView style={styles.notesBox} contentContainerStyle={styles.notesInner}>
              <Text style={styles.notesText}>{update.notes}</Text>
            </ScrollView>
          )}
          <Pressable onPress={install} style={({ pressed }) => [styles.primary, pressed && { opacity: 0.9 }]}>
            <GradientView colors={Gradients.play} style={styles.primaryGrad}>
              <Text style={styles.primaryText}>Download & Install</Text>
            </GradientView>
          </Pressable>
          <Pressable onPress={() => Linking.openURL('https://github.com/kishor-dev-970/VibeOn/releases/latest').catch(() => {})} hitSlop={8}>
            <Text style={styles.link}>See release notes on GitHub</Text>
          </Pressable>
          <Pressable onPress={dismiss} hitSlop={8}>
            <Text style={styles.later}>Later</Text>
          </Pressable>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(6, 4, 16, 0.88)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.xl,
  },
  sheet: {
    width: '100%',
    maxWidth: 400,
    borderRadius: BorderRadius.xxl,
    backgroundColor: Glass.bgStrong,
    borderWidth: 1,
    borderColor: Glass.border,
    padding: Spacing.xl,
    alignItems: 'center',
    ...Shadows.soft,
  },
  badgeWrap: { marginBottom: Spacing.md },
  badge: {
    paddingHorizontal: 16,
    paddingVertical: 5,
    borderRadius: BorderRadius.full,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: { color: '#fff', fontSize: 11, fontWeight: '800', letterSpacing: 1.5 },
  title: { color: Colors.text, fontSize: 20, fontWeight: '800', marginBottom: Spacing.sm },
  subtitle: { color: Colors.textMuted, fontSize: 14, lineHeight: 21, textAlign: 'center', marginBottom: Spacing.lg },
  version: { color: Colors.primaryLight, fontWeight: '800' },
  notesBox: {
    maxHeight: 160,
    width: '100%',
    backgroundColor: Colors.bg,
    borderRadius: BorderRadius.lg,
    marginBottom: Spacing.lg,
    borderWidth: 1,
    borderColor: Glass.border,
  },
  notesInner: { padding: Spacing.md },
  notesText: { color: Colors.textMuted, fontSize: 13, lineHeight: 19 },
  primary: {
    width: '100%',
    borderRadius: BorderRadius.lg,
    overflow: 'hidden',
    ...Shadows.glow,
  },
  primaryGrad: { paddingVertical: 14, alignItems: 'center' },
  primaryText: { color: '#fff', fontSize: 15, fontWeight: '800' },
  link: { color: Colors.primaryLight, fontSize: 13, marginTop: Spacing.md },
  later: { color: Colors.textSubtle, fontSize: 14, marginTop: Spacing.md },
});