import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { useEventListener } from 'expo';
import { useVideoPlayer, VideoView } from 'expo-video';
import * as api from '../lib/api';
import { Colors } from '../lib/theme';

export interface AdFreeVideoPlayerHandle {
  pauseVideo: () => void;
  playVideo: () => void;
  stopVideo: () => void;
  seekTo: (seconds: number, allowSeekAhead?: boolean) => void;
  getCurrentTime: () => number;
}

interface AdFreeVideoPlayerProps {
  videoId: string;
  height: number;
  play: boolean;
  onChangeState?: (state: string) => void;
}

const streamUrlCache: Record<string, string> = {};

export const AdFreeVideoPlayer = forwardRef<AdFreeVideoPlayerHandle, AdFreeVideoPlayerProps>(
  ({ videoId, height, play, onChangeState }, ref) => {
    const [source, setSource] = useState<{ uri: string } | null>(null);
    const [error, setError] = useState(false);
    const [loading, setLoading] = useState(false);

    const player = useVideoPlayer(null);
    const onChangeStateRef = useRef(onChangeState);
    onChangeStateRef.current = onChangeState;
    const didPlayRef = useRef(false);

    useEventListener(player, 'playingChange', ({ isPlaying }) => {
      if (isPlaying) didPlayRef.current = true;
      onChangeStateRef.current?.(isPlaying ? 'playing' : 'paused');
    });
    useEventListener(player, 'playToEnd', () => {
      if (!didPlayRef.current) return;
      onChangeStateRef.current?.('ended');
    });

    useEffect(() => {
      let cancelled = false;
      setSource(null);
      setError(false);
      setLoading(true);
      const load = async () => {
        try {
          const url = streamUrlCache[videoId] ?? (await api.fetchVideoStream(videoId)).videoUrl;
          if (cancelled) return;
          streamUrlCache[videoId] = url;
          setSource({ uri: url });
        } catch {
          if (!cancelled) setError(true);
        } finally {
          if (!cancelled) setLoading(false);
        }
      };
      load();
      return () => {
        cancelled = true;
      };
    }, [videoId]);

    useEffect(() => {
      if (source) {
        try { player.replace(source, true); } catch {}
      }
    }, [source, player]);

    useEffect(() => {
      if (!source || error) return;
      if (play) {
        try { player.play(); } catch {}
      } else {
        try { player.pause(); } catch {}
      }
    }, [play, source, error, player]);

    useImperativeHandle(
      ref,
      () => ({
        pauseVideo: () => player.pause(),
        playVideo: () => player.play(),
        stopVideo: () => {
          try {
            player.pause();
            player.replace(null, true);
          } catch {}
        },
        seekTo: (seconds: number) => {
          try { player.currentTime = seconds; } catch {}
        },
        getCurrentTime: () => player.currentTime,
      }),
      [player]
    );

    return (
      <View style={[styles.wrap, { height }]}>
        {source && !error ? (
          <VideoView player={player} style={StyleSheet.absoluteFill} contentFit="contain" />
        ) : null}
        {loading ? (
          <View style={[StyleSheet.absoluteFill, styles.overlay]}>
            <ActivityIndicator color={Colors.primary} />
          </View>
        ) : null}
        {error ? (
          <View style={[StyleSheet.absoluteFill, styles.overlay]}>
            <Text style={styles.errorText}>Video unavailable</Text>
          </View>
        ) : null}
      </View>
    );
  }
);

const styles = StyleSheet.create({
  wrap: {
    width: '100%',
    backgroundColor: '#000',
    overflow: 'hidden',
  },
  overlay: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#000',
  },
  errorText: {
    color: Colors.textMuted,
    fontSize: 13,
  },
});