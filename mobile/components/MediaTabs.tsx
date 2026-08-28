import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  AppState,
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import type { Song } from '../lib/types';
import { Colors, BorderRadius, Spacing } from '../lib/theme';
import SongItem from './SongItem';

type Mode = 'video' | 'audio';

interface Props {
  mode: Mode;
  onModeChange: (mode: Mode) => void;
  songs: Song[];
  loading: boolean;
  refreshing: boolean;
  onRefresh: () => void;
  onPlay: (song: Song, index?: number) => void;
  activeSongId?: string | null;
  emptyLabel?: string;
}

export default function MediaTabs({
  mode,
  onModeChange,
  songs,
  loading,
  refreshing,
  onRefresh,
  onPlay,
  activeSongId,
  emptyLabel,
}: Props) {
  return (
    <View style={styles.container}>
      <View style={styles.toggleRow}>
        <Pressable
          style={[styles.toggle, mode === 'video' && styles.toggleActive]}
          onPress={() => onModeChange('video')}
        >
          <Text style={[styles.toggleLabel, mode === 'video' && styles.toggleLabelActive]}>🎬 Videos</Text>
        </Pressable>
        <Pressable
          style={[styles.toggle, mode === 'audio' && styles.toggleActive]}
          onPress={() => onModeChange('audio')}
        >
          <Text style={[styles.toggleLabel, mode === 'audio' && styles.toggleLabelActive]}>🎵 Audio</Text>
        </Pressable>
      </View>

      {loading ? (
        <ActivityIndicator style={{ marginTop: 60 }} color={Colors.primary} />
      ) : (
        <FlatList
          data={songs}
          keyExtractor={(item) => `${mode}-${item.videoId}`}
          renderItem={({ item, index }) => (
            <SongItem
              song={item}
              active={activeSongId === item.videoId}
              onPress={() => onPlay(item, index)}
            />
          )}
          ListEmptyComponent={
            <Text style={styles.empty}>{emptyLabel ?? 'No songs found'}</Text>
          }
          contentContainerStyle={{ paddingBottom: 160 }}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={Colors.primary} />
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  toggleRow: {
    flexDirection: 'row',
    marginHorizontal: Spacing.lg,
    marginBottom: Spacing.md,
    backgroundColor: Colors.bgCard,
    borderRadius: BorderRadius.full,
    padding: 4,
  },
  toggle: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 8,
    borderRadius: BorderRadius.full,
  },
  toggleActive: {
    backgroundColor: Colors.primary,
  },
  toggleLabel: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.textMuted,
  },
  toggleLabelActive: {
    color: '#fff',
  },
  empty: {
    textAlign: 'center',
    marginTop: 60,
    fontSize: 14,
    color: Colors.textMuted,
  },
});
