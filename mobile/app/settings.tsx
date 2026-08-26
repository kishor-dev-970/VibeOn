import { useState } from 'react';
import {
  Alert, Pressable, ScrollView, StyleSheet, Switch, Text, View, Share, Linking,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Colors, BorderRadius, Spacing } from '../lib/theme';
import { useAuth } from '../context/AuthContext';

export default function SettingsScreen() {
  const router = useRouter();
  const { darkMode, toggleDarkMode } = useAuth();
  const bg = darkMode ? Colors.bg : '#F8F9FC';
  const cardBg = darkMode ? Colors.bgCard : '#FFFFFF';
  const textColor = darkMode ? Colors.text : '#111827';
  const mutedColor = darkMode ? Colors.textMuted : '#6B7280';

  const inviteFacebook = async () => {
    try {
      await Share.share({
        message: 'Join me on Social Music! Listen to music together with your friends. Download now: https://play.google.com/store/apps/details?id=com.example.socialmusic',
        title: 'Invite to Social Music',
      });
    } catch {}
  };

  const inviteWhatsApp = () => {
    const msg = encodeURIComponent('Join me on Social Music! Listen to music together with your friends. Download now: https://play.google.com/store/apps/details?id=com.example.socialmusic');
    Linking.openURL(`whatsapp://send?text=${msg}`).catch(() =>
      Alert.alert('WhatsApp not installed')
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: bg }]}>
      <View style={[styles.header, { backgroundColor: cardBg, borderBottomColor: Colors.border }]}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Text style={[styles.backBtn, { color: Colors.primary }]}>← Back</Text>
        </Pressable>
        <Text style={[styles.headerTitle, { color: textColor }]}>Settings</Text>
        <View style={{ width: 60 }} />
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={[styles.sectionTitle, { color: mutedColor }]}>APPEARANCE</Text>
        <View style={[styles.card, { backgroundColor: cardBg }]}>
          <View style={styles.settingRow}>
            <Text style={[styles.settingLabel, { color: textColor }]}>Dark Mode</Text>
            <Switch
              value={darkMode}
              onValueChange={toggleDarkMode}
              trackColor={{ false: '#393459', true: Colors.primaryLight }}
              thumbColor={darkMode ? Colors.primary : '#f4f3f4'}
            />
          </View>
        </View>

        <Text style={[styles.sectionTitle, { color: mutedColor }]}>INVITE FRIENDS</Text>
        <View style={[styles.card, { backgroundColor: cardBg }]}>
          <Pressable style={styles.inviteBtn} onPress={inviteFacebook}>
            <Text style={styles.inviteIcon}>f</Text>
            <Text style={[styles.inviteLabel, { color: textColor }]}>Share on Facebook</Text>
          </Pressable>
          <View style={[styles.divider, { backgroundColor: Colors.border }]} />
          <Pressable style={styles.inviteBtn} onPress={inviteWhatsApp}>
            <Text style={styles.inviteIcon}>💬</Text>
            <Text style={[styles.inviteLabel, { color: textColor }]}>Share on WhatsApp</Text>
          </Pressable>
        </View>

        <Text style={[styles.sectionTitle, { color: mutedColor }]}>ABOUT</Text>
        <View style={[styles.card, { backgroundColor: cardBg }]}>
          <Text style={[styles.settingLabel, { color: textColor }]}>Social Music</Text>
          <Text style={[styles.version, { color: mutedColor }]}>Version 1.0.0</Text>
          <View style={[styles.divider, { backgroundColor: Colors.border }]} />
          <Text style={[styles.about, { color: mutedColor }]}>
            Design & Developed by KK{'\n'}
            Listen together with friends.
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: Spacing.lg, paddingTop: 56, paddingBottom: Spacing.md,
    borderBottomWidth: 1,
  },
  backBtn: { fontSize: 16, fontWeight: '600' },
  headerTitle: { fontSize: 18, fontWeight: '700' },
  content: { padding: Spacing.lg },
  sectionTitle: { fontSize: 12, fontWeight: '700', letterSpacing: 1, marginBottom: Spacing.sm, marginTop: Spacing.xl },
  card: { borderRadius: BorderRadius.lg, padding: Spacing.lg },
  settingRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  settingLabel: { fontSize: 16, fontWeight: '600' },
  inviteBtn: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 14 },
  inviteIcon: { fontSize: 20, fontWeight: '800', color: Colors.primary, width: 28 },
  inviteLabel: { fontSize: 15, fontWeight: '500' },
  divider: { height: 1, marginVertical: 2 },
  version: { fontSize: 13, marginTop: 4 },
  about: { fontSize: 14, lineHeight: 22, marginTop: 8 },
});
