import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
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
import type { FriendActivity } from '../../lib/types';
import { Colors, BorderRadius, Spacing } from '../../lib/theme';
import FriendDetailModal from '../../components/FriendDetailModal';

const POLL_INTERVAL_MS = 5000;

export default function FriendsScreen() {
  const { signOut, darkMode } = useAuth();
  const { playFriendSong } = usePlayer();
  const router = useRouter();
  const [friends, setFriends] = useState<FriendActivity[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedFriend, setSelectedFriend] = useState<FriendActivity | null>(null);

  const load = useCallback(
    async (showSpinner = false) => {
      if (showSpinner) setRefreshing(true);
      try {
        const data = await api.fetchFriendsActivity();
        setFriends(data.friends);
      } catch (e) {
        const message = e instanceof Error ? e.message : String(e);
        if (message.includes('log in again') || message.includes('session')) {
          await signOut();
          router.replace('/login');
          return;
        }
        Alert.alert('Could not load friends', message);
      } finally {
        if (showSpinner) setRefreshing(false);
      }
    },
    [signOut, router]
  );

  useEffect(() => {
    load();
    const id = setInterval(() => load(), POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [load]);

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
    playFriendSong(song);
    setSelectedFriend(null);
    router.navigate('/(tabs)');
  };

  return (
    <View style={[styles.container, { backgroundColor: Colors.bg }]}>
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
          darkMode={darkMode}
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
  userRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginBottom: Spacing.sm,
  },
  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
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
