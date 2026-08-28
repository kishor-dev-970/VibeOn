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
import { useRouter } from 'expo-router';
import { useAuth } from '../../context/AuthContext';
import { usePlayer } from '../../context/PlayerContext';
import * as api from '../../lib/api';
import type { FriendActivity, NowPlaying } from '../../lib/types';
import { Colors, BorderRadius, Spacing } from '../../lib/theme';
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
  const { user, signOut } = useAuth();
  const { playSong } = usePlayer();
  const router = useRouter();
  const [friends, setFriends] = useState<FriendActivity[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedFriend, setSelectedFriend] = useState<FriendActivity | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(
    async (showSpinner = false) => {
      if (!user) return;
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
        setLoading(false);
        if (showSpinner) setRefreshing(false);
      }
    },
    [user, signOut]
  );

  useEffect(() => {
    load();
    const id = setInterval(() => load(), POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [load]);

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

  const handlePlayCurrent = (song: NowPlaying, audioMode = false) => {
    setSelectedFriend(null);
    if (audioMode) {
      playSong(song, true);
    } else {
      router.push({
        pathname: '/video-player',
        params: {
          videoId: song.videoId,
          title: song.title,
          channel: song.channel,
          thumbnailUrl: song.thumbnailUrl,
        },
      });
    }
  };

  const renderAvatar = (name: string, avatarUrl: string | null | undefined, size = 'small') => (
    avatarUrl ? (
      <Image source={{ uri: avatarUrl }} style={size === 'small' ? styles.avatar : styles.avatarLarge} />
    ) : (
      <View style={[size === 'small' ? styles.avatar : styles.avatarLarge, styles.avatarInitials]}>
        <Text style={styles.avatarInitialsText}>{initials(name)}</Text>
      </View>
    )
  );

  return (
    <View style={[styles.container, { backgroundColor: Colors.bg }]}>
      <View style={styles.userRow}>
        {renderAvatar(user?.name ?? 'You', user?.avatar_url)}
        <View style={{ flex: 1 }}>
          <Text style={[styles.userName, { color: Colors.text }]}>{user?.name ?? 'You'}</Text>
          {user?.code ? <Text style={[styles.userCode, { color: Colors.textMuted }]}>Code: {user.code}</Text> : null}
        </View>
        <Pressable onPress={handleSignOut} hitSlop={10} style={styles.signOut}>
          <Text style={[styles.signOutLabel, { color: Colors.primary }]}>Sign out</Text>
        </Pressable>
      </View>

      <Text style={[styles.summary, { color: Colors.textMuted }]}>
        {listeningCount > 0
          ? `${listeningCount} person${listeningCount === 1 ? '' : 's'} listening now`
          : 'Nobody is listening right now'}
      </Text>

      {loading ? (
        <ActivityIndicator style={{ marginTop: 60 }} color={Colors.primary} />
      ) : (
        <FlatList
          data={friends}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <Pressable
              style={[styles.card, { backgroundColor: Colors.bgCard }]}
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
          ListEmptyComponent={<Text style={[styles.empty, { color: Colors.textMuted }]}>No one using the app yet</Text>}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={() => load(true)} tintColor={Colors.primary} />
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
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
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
  avatarLarge: {
    width: 48,
    height: 48,
    borderRadius: 24,
  },
  avatarInitials: {
    backgroundColor: Colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarInitialsText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '800',
  },
  userName: {
    fontSize: 15,
    fontWeight: '700',
  },
  userCode: {
    fontSize: 11,
    marginTop: 1,
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