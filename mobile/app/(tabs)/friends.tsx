import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  ActivityIndicator,
  FlatList,
  Image,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';
import { usePlayer } from '../../context/PlayerContext';
import * as api from '../../lib/api';
import type { FriendActivity, NowPlaying } from '../../lib/types';
import { Colors, BorderRadius, Spacing, Glass, Gradients, Shadows } from '../../lib/theme';
import GradientView from '../../components/GradientView';
import FriendDetailModal from '../../components/FriendDetailModal';

const POLL_INTERVAL_MS = 30000;

const initials = (name: string) =>
  name
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

export default function FriendsScreen() {
  const router = useRouter();
  const { user, signOut } = useAuth();
  const { playSong } = usePlayer();
  const [friends, setFriends] = useState<FriendActivity[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedFriend, setSelectedFriend] = useState<FriendActivity | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchFriends = useCallback(async () => {
    try {
      const data = await api.fetchFriendsActivity();
      setFriends(data.friends);
    } catch {}
  }, []);

  useEffect(() => {
    fetchFriends().finally(() => setLoading(false));
    const interval = setInterval(fetchFriends, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [fetchFriends]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await fetchFriends();
    setRefreshing(false);
  }, [fetchFriends]);

  const handleSignOut = () => {
    Alert.alert('Sign Out', 'Are you sure you want to sign out?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Sign Out', style: 'destructive', onPress: signOut },
    ]);
  };

  const isOnline = (friend: FriendActivity) => {
    if (friend.nowPlaying && friend.nowPlaying.isPlaying) return true;
    if (friend.lastActive) {
      const diff = Date.now() - new Date(friend.lastActive).getTime();
      return diff < 2 * 60 * 1000;
    }
    return false;
  };

  const listeningCount = friends.filter((f) => f.nowPlaying?.isPlaying).length;

  const handlePlaySong = (song: { videoId: string; title: string; channel: string; thumbnailUrl: string }) => {
    setSelectedFriend(null);
    playSong(song, true);
  };

  const handlePlayCurrent = (song: NowPlaying) => {
    setSelectedFriend(null);
    playSong(song, true);
  };

  const renderAvatar = (name: string, avatarUrl: string | null | undefined, size = 'small') => {
    const dim = size === 'small' ? 36 : 48;
    return (
      <View style={[styles.avatarRing, { width: dim + 4, height: dim + 4, borderRadius: (dim + 4) / 2 }]}>
        <GradientView colors={Gradients.play} style={StyleSheet.absoluteFill} />
        <View style={[styles.avatarInner, { width: dim, height: dim, borderRadius: dim / 2 }]}>
          {avatarUrl ? (
            <Image source={{ uri: avatarUrl }} style={[styles.avatarImg, { width: dim, height: dim, borderRadius: dim / 2 }]} />
          ) : (
            <View style={[styles.avatarInitials, { width: dim, height: dim, borderRadius: dim / 2 }]}>
              <Text style={[styles.avatarInitialsText, { fontSize: dim / 2.6 }]}>{initials(name)}</Text>
            </View>
          )}
        </View>
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <GradientView colors={Gradients.brandMuted} style={styles.header}>
        <View style={styles.headerRow}>
          <View>
            <Text style={styles.brand}>VibeOn</Text>
            <Text style={styles.headerTitle}>Friends</Text>
          </View>
          <View style={styles.headerActions}>
            <Pressable
              onPress={() => router.push('/settings')}
              hitSlop={12}
              style={({ pressed }) => [styles.settingsBtn, pressed && { opacity: 0.7 }]}
            >
              <Ionicons name="settings-outline" color={Colors.text} size={20} />
            </Pressable>
            <Pressable
              onPress={handleSignOut}
              hitSlop={10}
              style={({ pressed }) => [styles.signOut, pressed && { opacity: 0.7 }]}
            >
              <Text style={[styles.signOutLabel, { color: Colors.primaryLight }]}>Sign out</Text>
            </Pressable>
          </View>
        </View>
        <Text style={[styles.summary, { color: Colors.textMuted }]}>
          {listeningCount > 0
            ? `${listeningCount} person${listeningCount === 1 ? '' : 's'} listening now`
            : 'Nobody is listening right now'}
        </Text>
      </GradientView>

      {loading ? (
        <ActivityIndicator style={{ marginTop: 60 }} color={Colors.primary} />
      ) : (
        <FlatList
          data={friends}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <Pressable
              style={({ pressed }) => [styles.card, pressed && { opacity: 0.92 }]}
              onPress={() => setSelectedFriend(item)}
            >
              <View style={styles.userRow}>
                {renderAvatar(item.name, item.avatarUrl)}
                {isOnline(item) && <View style={styles.onlineDot} />}
                <View style={{ flex: 1 }}>
                  <Text style={[styles.name, { color: Colors.text }]}>{item.name}</Text>
                  {item.code ? (
                    <Text style={[styles.userCode, { color: Colors.textMuted }]}>Code: {item.code}</Text>
                  ) : null}
                </View>
              </View>
              {item.nowPlaying ? (
                <View style={styles.songRow}>
                  {!!item.nowPlaying.thumbnailUrl && (
                    <Image source={{ uri: item.nowPlaying.thumbnailUrl }} style={styles.thumb} />
                  )}
                  <View style={{ flex: 1 }}>
                    <Text style={[styles.songTitle, { color: Colors.text }]} numberOfLines={1}>
                      {item.nowPlaying.title}
                    </Text>
                    <Text style={[styles.songChannel, { color: Colors.textMuted }]} numberOfLines={1}>
                      {item.nowPlaying.channel}
                    </Text>
                  </View>
                  <View
                    style={[
                      styles.statusPill,
                      item.nowPlaying.isPlaying ? styles.live : styles.paused,
                    ]}
                  >
                    <Text style={styles.statusText}>
                      {item.nowPlaying.isPlaying ? '● Live' : '❙❙ Paused'}
                    </Text>
                  </View>
                </View>
              ) : (
                <Text style={[styles.offline, { color: Colors.textSubtle }]}>Not listening</Text>
              )}
            </Pressable>
          )}
          ListEmptyComponent={<Text style={[styles.empty, { color: Colors.textMuted }]}>No one using the app yet</Text>}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={Colors.primary} />
          }
          contentContainerStyle={{ paddingBottom: 24, paddingHorizontal: Spacing.md, paddingTop: Spacing.sm }}
        />
      )}

      {selectedFriend && (
        <FriendDetailModal
          friendId={selectedFriend.id}
          friendName={selectedFriend.name}
          friendCode={selectedFriend.code}
          darkMode={false}
          onClose={() => setSelectedFriend(null)}
          onPlaySong={handlePlaySong}
          onPlayCurrent={handlePlayCurrent}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
    position: 'relative',
  },
  header: {
    paddingHorizontal: Spacing.lg,
    paddingTop: Spacing.md,
    paddingBottom: Spacing.lg,
    borderBottomLeftRadius: BorderRadius.xl,
    borderBottomRightRadius: BorderRadius.xl,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  brand: {
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 2,
    color: Colors.primaryLight,
    textTransform: 'uppercase',
    opacity: 0.9,
  },
  headerTitle: { fontSize: 30, fontWeight: '800', color: Colors.text, marginTop: 2 },
  summary: { fontSize: 13, fontWeight: '600', marginTop: Spacing.sm },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  settingsBtn: {
    width: 38,
    height: 38,
    borderRadius: 19,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255,255,255,0.08)',
    borderWidth: 1,
    borderColor: Glass.border,
  },
  signOut: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: BorderRadius.full,
    backgroundColor: 'rgba(255,255,255,0.08)',
    borderWidth: 1,
    borderColor: Glass.border,
  },
  signOutLabel: { fontSize: 13, fontWeight: '700' },
  card: {
    borderRadius: 20,
    padding: Spacing.md,
    marginBottom: Spacing.md,
    marginHorizontal: Spacing.xs,
    backgroundColor: 'rgba(21, 15, 42, 0.75)',
    borderWidth: 1,
    borderColor: 'rgba(167, 139, 250, 0.20)',
    ...Shadows.glow,
  },
  userRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: Spacing.sm,
  },
  avatarRing: {
    padding: 2,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarInner: {
    backgroundColor: Colors.bgCardLight,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  avatarImg: { resizeMode: 'cover' },
  avatarInitials: { backgroundColor: Colors.bgCardLight, alignItems: 'center', justifyContent: 'center' },
  avatarInitialsText: {
    color: '#fff',
    fontWeight: '800',
  },
  onlineDot: {
    position: 'absolute',
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: Colors.success,
    left: 30,
    top: 30,
    borderWidth: 2,
    borderColor: '#0A0718',
  },
  name: {
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: 0.2,
  },
  userCode: {
    fontSize: 11,
    marginTop: 2,
  },
  songRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: 'rgba(10, 7, 24, 0.60)',
    borderRadius: 14,
    padding: 8,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
  },
  thumb: {
    width: 60,
    height: 44,
    borderRadius: 10,
    backgroundColor: Colors.bgCardLight,
  },
  songTitle: {
    fontSize: 13,
    fontWeight: '700',
  },
  songChannel: {
    fontSize: 11,
    marginTop: 2,
  },
  statusPill: {
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: BorderRadius.full,
    borderWidth: 1,
  },
  live: {
    backgroundColor: 'rgba(16, 185, 129, 0.18)',
    borderColor: 'rgba(16, 185, 129, 0.40)',
  },
  paused: {
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    borderColor: 'rgba(255, 255, 255, 0.12)',
  },
  statusText: {
    fontSize: 10,
    fontWeight: '800',
    color: Colors.success,
  },
  offline: {
    fontSize: 12,
    fontWeight: '600',
    paddingVertical: 4,
    paddingHorizontal: 8,
  },
  empty: {
    textAlign: 'center',
    marginTop: 40,
    fontSize: 14,
  },
});
