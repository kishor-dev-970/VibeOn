import {
  Alert, Pressable, ScrollView, StyleSheet, Text, View, Share, Linking,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Colors, BorderRadius, Spacing } from '../lib/theme';

const GITHUB_LINK = 'https://github.com/kishor-dev-970/My_SocialMusic_Project/blob/main/mobile/social-music.apk';

export default function SettingsScreen() {
  const router = useRouter();

  const inviteFacebook = async () => {
    try {
      await Share.share({
        message: 'Join me on Social Music! Listen to music together with your friends. Download the app: ' + GITHUB_LINK,
        title: 'Invite to Social Music',
      });
    } catch {}
  };

  const inviteWhatsApp = () => {
    const msg = encodeURIComponent('Join me on Social Music! Listen to music together with your friends. Download the app: ' + GITHUB_LINK);
    Linking.openURL(`whatsapp://send?text=${msg}`).catch(() =>
      Alert.alert('WhatsApp not installed')
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Text style={styles.backBtn}>← Back</Text>
        </Pressable>
        <Text style={styles.headerTitle}>Settings</Text>
        <View style={{ width: 60 }} />
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.sectionTitle}>INVITE FRIENDS</Text>
        <View style={styles.card}>
          <Pressable style={styles.inviteBtn} onPress={inviteFacebook}>
            <Text style={styles.inviteIcon}>f</Text>
            <Text style={styles.inviteLabel}>Share on Facebook</Text>
          </Pressable>
          <View style={styles.divider} />
          <Pressable style={styles.inviteBtn} onPress={inviteWhatsApp}>
            <Text style={styles.inviteIcon}>💬</Text>
            <Text style={styles.inviteLabel}>Share on WhatsApp</Text>
          </Pressable>
        </View>

        <Text style={styles.sectionTitle}>ABOUT</Text>
        <View style={styles.card}>
          <Text style={styles.settingLabel}>Social Music</Text>
          <Text style={styles.version}>Version 1.0.0</Text>
          <View style={styles.divider} />
          <Text style={styles.about}>
            Design & Developed by KK{'\n'}
            Listen together with friends.
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.bg },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: Spacing.lg, paddingTop: 56, paddingBottom: Spacing.md,
    backgroundColor: Colors.tabBarBg, borderBottomWidth: 1, borderBottomColor: Colors.border,
  },
  backBtn: { fontSize: 16, fontWeight: '600', color: Colors.primary },
  headerTitle: { fontSize: 18, fontWeight: '700', color: Colors.text },
  content: { padding: Spacing.lg },
  sectionTitle: { fontSize: 12, fontWeight: '700', letterSpacing: 1, marginBottom: Spacing.sm, marginTop: Spacing.xl, color: Colors.textMuted },
  card: { backgroundColor: Colors.bgCard, borderRadius: BorderRadius.lg, padding: Spacing.lg },
  settingLabel: { fontSize: 16, fontWeight: '600', color: Colors.text },
  inviteBtn: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 14 },
  inviteIcon: { fontSize: 20, fontWeight: '800', color: Colors.primary, width: 28 },
  inviteLabel: { fontSize: 15, fontWeight: '500', color: Colors.text },
  divider: { height: 1, backgroundColor: Colors.border, marginVertical: 2 },
  version: { fontSize: 13, marginTop: 4, color: Colors.textMuted },
  about: { fontSize: 14, lineHeight: 22, marginTop: 8, color: Colors.textMuted },
});
