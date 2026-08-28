import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
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
const inFlight: Record<string, Promise<string>> = {};

async function fetchVideoStreamUrl(videoId: string, attempt = 0): Promise<string> {
  const cachedUrl = streamUrlCache[videoId];
  if (cachedUrl) return cachedUrl;
  const pending = inFlight[videoId];
  if (pending) return pending;
  const p = api
    .fetchVideoStream(videoId)
    .then((r) => {
      streamUrlCache[videoId] = r.videoUrl;
      return r.videoUrl;
    })
    .catch(async (e) => {
      if (attempt >= 2) throw e;
      await new Promise((resolve) => setTimeout(resolve, 1200));
      return fetchVideoStreamUrl(videoId, attempt + 1);
    })
    .finally(() => {
      delete inFlight[videoId];
    });
  inFlight[videoId] = p;
  return p;
}

export const AdFreeVideoPlayer = forwardRef<AdFreeVideoPlayerHandle, AdFreeVideoPlayerProps>(
  ({ videoId, height, play, onChangeState }, ref) => {
    const [source, setSource] = useState<{ uri: string } | null>(null);
    const [error, setError] = useState(false);
    const [loading, setLoading] = useState(false);

    const player = useVideoPlayer(null);
    const onChangeStateRef = useRef(onChangeState);
    onChangeStateRef.current = onChangeState;
    const didPlayRef = useRef(false);
    const replacedVideoIdRef = useRef<string | null>(null);
    const loadRef = useRef<(() => Promise<void>) | null>(null);

    useEventListener(player, 'playingChange', ({ isPlaying }) => {
      if (isPlaying) didPlayRef.current = true;
      onChangeStateRef.current?.(isPlaying ? 'playing' : 'paused');
    });
    useEventListener(player, 'playToEnd', () => {
      if (!didPlayRef.current) return;
      onChangeStateRef.current?.('ended');
    });
    useEventListener(player, 'statusChange', ({ status }) => {
      if (status === 'error') onChangeStateRef.current?.('error');
    });

    useEffect(() => {
      let cancelled = false;
      setSource(null);
      setError(false);
      setLoading(true);
      replacedVideoIdRef.current = null;
      const load = async () => {
        try {
          const url = await fetchVideoStreamUrl(videoId);
          if (cancelled) return;
          streamUrlCache[videoId] = url;
          setSource({ uri: url });
        } catch (e) {
          console.error('stream-fetch-error', videoId, String(e));
          if (!cancelled) setError(true);
        } finally {
          if (!cancelled) setLoading(false);
        }
      };
      loadRef.current = load;
      load();
      return () => {
        cancelled = true;
      };
    }, [videoId]);

    const retry = useCallback(() => {
      delete streamUrlCache[videoId];
      setError(false);
      setLoading(true);
      loadRef.current?.();
    }, [videoId]);

    useEffect(() => {
      if (!source || replacedVideoIdRef.current === videoId) return;
      replacedVideoIdRef.current = videoId;
      try {
        player.replace(source);
      } catch {}
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [source, videoId]);

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
            player.replace(null);
          } catch {}
        },
        seekTo: (seconds: number) => {
          try {
            if (Number.isFinite(seconds) && seconds > 0) player.currentTime = seconds;
          } catch {}
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
          <Pressable style={[StyleSheet.absoluteFill, styles.overlay]} onPress={retry}>
            <Text style={styles.errorText}>Video unavailable · Tap to retry</Text>
          </Pressable>
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