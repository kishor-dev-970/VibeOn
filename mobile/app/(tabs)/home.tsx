import { useCallback, useEffect, useRef, useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import MediaTabs from '../../components/MediaTabs';
import NowPlayingCard from '../../components/NowPlayingCard';
import GradientView from '../../components/GradientView';
import * as api from '../../lib/api';
import type { Song } from '../../lib/types';
import { Colors, BorderRadius, Spacing, Glass, Gradients, Shadows } from '../../lib/theme';
import { usePlayer } from '../../context/PlayerContext';

type Mode = 'video' | 'audio';

export default function HomeScreen() {
  const router = useRouter();
  const { currentSong, playSong } = usePlayer();
  const [query, setQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Song[]>([]);
  const [trending, setTrending] = useState<Song[]>([]);
  const [searching, setSearching] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [focused, setFocused] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadTrending = useCallback(async () => {
    try {
      const data = await api.fetchTrendingSongs();
      setTrending(data.songs);
    } catch {}
  }, []);

  useEffect(() => {
    loadTrending();
  }, [loadTrending]);

  const doSearch = useCallback(async (q: string) => {
    if (q.trim().length < 2) {
      setSearchResults([]);
      return;
    }
    setSearching(true);
    try {
      const data = await api.searchSongs(q.trim());
      setSearchResults(data.songs);
    } catch {
    } finally {
      setSearching(false);
    }
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => doSearch(query), 450);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query, doSearch]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadTrending();
    setRefreshing(false);
  }, [loadTrending]);

  const mode: Mode = 'audio';

  const isSearching = query.trim().length >= 2;
  const displaySongs = isSearching ? searchResults : trending;
  const activeSongId = currentSong?.videoId ?? null;

  const handlePlay = useCallback((song: Song) => {
    playSong(song, true);
  }, [playSong]);

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <GradientView colors={Gradients.brandMuted} style={styles.header}>
          <View style={styles.headerRow}>
            <View>
              <Text style={styles.brand}>VibeOn</Text>
              <Text style={styles.headerTitle}>Songs</Text>
            </View>
            <Pressable
              onPress={() => router.push('/settings')}
              hitSlop={12}
              style={({ pressed }) => [styles.settingsBtn, pressed && { opacity: 0.7 }]}
            >
              <Ionicons name="settings-outline" color={Colors.text} size={20} />
            </Pressable>
          </View>
        </GradientView>

        <View
          style={[
            styles.searchBar,
            { backgroundColor: Glass.bg, borderColor: focused ? Colors.primaryLight : Glass.border },
          ]}
        >
          <Ionicons name="search" color={Colors.textMuted} size={18} />
          <TextInput
            style={[styles.searchInput, { color: Colors.text }]}
            placeholder="Search songs, artists..."
            placeholderTextColor={Colors.textSubtle}
            value={query}
            onChangeText={setQuery}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            autoCapitalize="none"
            returnKeyType="search"
            onSubmitEditing={() => doSearch(query)}
          />
          {query.length > 0 && (
            <Pressable onPress={() => setQuery('')} hitSlop={8}>
              <Ionicons name="close-circle" color={Colors.textMuted} size={18} />
            </Pressable>
          )}
        </View>

        <MediaTabs
          mode={mode}
          showToggle={false}
          songs={displaySongs}
          loading={searching && isSearching}
          refreshing={refreshing}
          onRefresh={onRefresh}
          onPlay={handlePlay}
          activeSongId={activeSongId}
          emptyLabel={isSearching ? 'No songs found' : 'No trending songs yet'}
        />

        <NowPlayingCard accent={Colors.primary} />
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.bg, position: 'relative' },
  flex: { flex: 1 },
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
  settingsBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255,255,255,0.08)',
    borderWidth: 1,
    borderColor: Glass.border,
  },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: BorderRadius.full,
    marginHorizontal: Spacing.lg,
    marginVertical: Spacing.md,
    paddingHorizontal: Spacing.md,
    gap: 10,
    borderWidth: 1,
    ...Shadows.glow,
  },
  searchInput: { flex: 1, paddingVertical: 12, fontSize: 15 },
});
