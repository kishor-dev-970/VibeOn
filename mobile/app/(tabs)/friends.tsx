import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  ActivityIndicator,
  FlatList,
  Image,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../../context/AuthContext';
import * as api from '../../lib/api';
import { facebookLogin } from '../../lib/facebook';
import type { FriendActivity } from '../../lib/types';
import { Colors, BorderRadius, Spacing } from '../../lib/theme';
import FriendDetailModal from '../../components/FriendDetailModal';

const POLL_INTERVAL_MS = 30000;

export default function FriendsScreen() {
  const { token, user, signIn, signOut } = useAuth();
  const router = useRouter();
  const [friends, setFriends] = useState<FriendActivity[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedFriend, setSelectedFriend] = useState<FriendActivity | null>(null);
  const [loginBusy, setLoginBusy] = useState(false);

  const load = useCallback(
    async (showSpinner = false) => {
      if (!token) return;
      if (showSpinner) setRefreshing(true);
      try {
        const data = await api.fetchFriendsActivity();
        setFriends(data.friends);
      } catch (e) {
        const message = e instanceof Error ? e.message : String(e);
        if (message.includes('log in again') || message.includes('session')) {
          await signOut();
          return;
        }
        Alert.alert('Could not load friends', message);
      } finally {
        if (showSpinner) setRefreshing(false);
      }
    },
    [token, signOut]
  );

  useEffect(() => {
    load();
    const id = setInterval(() => load(), POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [load]);

  const handleFacebookLogin = async () => {
    setLoginBusy(true);
    try {
      const accessToken = await facebookLogin();
      const data = await api.loginWithFacebook(accessToken);
      signIn(data.token, data.user);
    } catch (e) {
      Alert.alert('Login failed', e instanceof Error ? e.message : String(e));
    } finally {
      setLoginBusy(false);
    }
  };

  const handleSignOut = () => {
    Alert.alert('Sign out', 'Are you sure you want to sign out?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Sign out',
        style: 'destructive',
        onPress: async () => {
          await signOut();
          setFriends([]);
        },
      },
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
    router.push({
      pathname: '/video-player',
      params: {
        videoId: song.videoId,
        title: song.title,
        channel: song.channel,
        thumbnailUrl: song.thumbnailUrl,
      },
    });
  };

  if (!token) {
    return (
      <ScrollView
        style={[styles.container, { backgroundColor: Colors.bg }]}
        contentContainerStyle={styles.loginContainer}
      >
        <Text style={styles.loginNote}>♫</Text>
        <Text style={styles.loginTitle}>Connect with friends</Text>
        <Text style={styles.loginSubtitle}>
          See what your friends are listening to and share your activity.
        </Text>
        {loginBusy ? (
          <ActivityIndicator size="large" color={Colors.primary} style={{ marginTop: 32 }} />
        ) : (
          <Pressable style={styles.fbButton} onPress={handleFacebookLogin}>
            <Text style={styles.fbIcon}>f</Text>
            <Text style={styles.fbButtonText}>Continue with Facebook</Text>
          </Pressable>
        )}
        <Text style={styles.loginFootnote}>You can still browse music without logging in.</Text>
      </ScrollView>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: Colors.bg }]}>
      <View style={styles.userRow}>
        {user?.avatar_url ? (
          <Image source={{ uri: user.avatar_url }} style={styles.avatar} />
        ) : (
          <View style={[styles.avatar, { backgroundColor: Colors.bgCardLight }]} />
        )}
        <Text style={[styles.userName, { color: Colors.text }]}>{user?.name ?? 'You'}</Text>
        <Pressable onPress={handleSignOut} hitSlop={10} style={styles.signOut}>
          <Text style={[styles.signOutLabel, { color: Colors.primary }]}>Sign out</Text>
        </Pressable>
      </View>

      <Text style={[styles.summary, { color: Colors.textMuted }]}>
        {listeningCount > 0
          ? `${listeningCount} friend${listeningCount === 1 ? '' : 's'} listening now`
          : 'Nobody is listening right now'}
      </Text>

      <FlatList
        data={friends}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <Pressable
            style={[styles.card, { backgroundColor: Colors.bgCard }]}
            onPress={() => setSelectedFriend(item)}
          >
            <View style={styles.userRow}>
              {item.avatarUrl ? (
                <Image source={{ uri: item.avatarUrl }} style={styles.avatar} />
              ) : (
                <View style={[styles.avatar, { backgroundColor: Colors.bgCardLight }]} />
              )}
              {isOnline(item) && <View style={styles.onlineDot} />}
              <Text style={[styles.name, { color: Colors.text }]}>{item.name}</Text>
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
                <Text
                  style={[
                    styles.status,
                    item.nowPlaying.isPlaying ? styles.live : styles.paused,
                  ]}
                >
                  {item.nowPlaying.isPlaying ? '● Live' : '❙❙ Paused'}
                </Text>
              </View>
            ) : (
              <Text style={[styles.offline, { color: Colors.textSubtle }]}>Not listening</Text>
            )}
          </Pressable>
        )}
        ListEmptyComponent={<Text style={[styles.empty, { color: Colors.textMuted }]}>No friends using the app yet</Text>}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={() => load(true)} tintColor={Colors.primary} />
        }
        contentContainerStyle={{ paddingBottom: 24, paddingHorizontal: Spacing.md, paddingTop: Spacing.sm }}
      />

      {selectedFriend && (
        <FriendDetailModal
          friendId={selectedFriend.id}
          friendName={selectedFriend.name}
          darkMode={false}
          onClose={() => setSelectedFriend(null)}
          onPlaySong={handlePlaySong}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  loginContainer: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },
  loginNote: {
    fontSize: 48,
    color: Colors.primary,
    marginBottom: 16,
  },
  loginTitle: {
    fontSize: 24,
    fontWeight: '800',
    color: Colors.text,
    textAlign: 'center',
    marginBottom: 8,
  },
  loginSubtitle: {
    fontSize: 15,
    color: Colors.textMuted,
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: 40,
  },
  fbButton: {
    backgroundColor: '#1877F2',
    borderRadius: BorderRadius.md,
    paddingVertical: 14,
    paddingHorizontal: 32,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  fbIcon: {
    fontSize: 20,
    fontWeight: '800',
    color: '#fff',
  },
  fbButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  loginFootnote: {
    fontSize: 12,
    color: Colors.textSubtle,
    marginTop: 20,
    textAlign: 'center',
  },
  userRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginBottom: Spacing.sm,
    paddingHorizontal: Spacing.lg,
    paddingTop: Spacing.sm,
  },
  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
  },
  userName: {
    fontSize: 15,
    fontWeight: '700',
    flex: 1,
  },
  signOut: {
    padding: 6,
  },
  signOutLabel: {
    fontSize: 13,
    fontWeight: '600',
  },
  summary: {
    fontSize: 13,
    fontWeight: '600',
    paddingVertical: Spacing.sm,
    paddingHorizontal: Spacing.lg,
  },
  card: {
    borderRadius: BorderRadius.lg,
    padding: Spacing.md,
    marginBottom: Spacing.sm,
    marginHorizontal: Spacing.sm,
  },
  onlineDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: Colors.success,
    position: 'absolute',
    left: 28,
    top: 26,
    borderWidth: 2,
    borderColor: Colors.bgCard,
  },
  name: {
    fontSize: 15,
    fontWeight: '700',
  },
  songRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  thumb: {
    width: 64,
    height: 40,
    borderRadius: BorderRadius.sm,
    backgroundColor: Colors.bgCardLight,
  },
  songTitle: {
    fontSize: 13,
    fontWeight: '600',
  },
  songChannel: {
    fontSize: 11,
    marginTop: 2,
  },
  status: {
    fontSize: 11,
    fontWeight: '700',
  },
  live: {
    color: Colors.success,
  },
  paused: {
    color: Colors.textMuted,
  },
  offline: {
    fontSize: 12,
  },
  empty: {
    textAlign: 'center',
    marginTop: 40,
    fontSize: 14,
  },
});