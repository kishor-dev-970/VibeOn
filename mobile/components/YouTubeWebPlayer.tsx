import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef } from 'react';
import { requireNativeComponent, UIManager, View } from 'react-native';

const NativePlayer: any = requireNativeComponent('BraveliteYouTubeView');

const CMD = { loadVideo: 1, loadWatch: 2, play: 3, pause: 4, seekTo: 5, stop: 6 };

export interface YouTubeWebPlayerHandle {
  pauseVideo: () => void;
  playVideo: () => void;
  stopVideo: () => void;
  seekTo: (seconds: number) => void;
  getCurrentTime: () => number;
}

interface YouTubeWebPlayerProps {
  videoId: string;
  height: number;
  play: boolean;
  onChangeState?: (state: 'playing' | 'paused' | 'ended' | 'error' | 'loading') => void;
  onFallbackToStream?: () => void;
}

interface PlaybackState {
  videoId: string;
  currentTime: number;
  paused: boolean;
  title: string;
  ended: boolean;
  error: boolean;
}

export const YouTubeWebPlayer = forwardRef<YouTubeWebPlayerHandle, YouTubeWebPlayerProps>(
  ({ videoId, height, play, onChangeState, onFallbackToStream }, ref) => {
    const nativeRef = useRef<any>(null);
    const lastStateRef = useRef<PlaybackState | null>(null);
    const watchTriedRef = useRef(false);
    const stoppedRef = useRef(false);

    const onChangeStateRef = useRef(onChangeState);
    onChangeStateRef.current = onChangeState;
    const onFallbackRef = useRef(onFallbackToStream);
    onFallbackRef.current = onFallbackToStream;
    const videoIdRef = useRef(videoId);
    videoIdRef.current = videoId;
    const playRef = useRef(play);
    playRef.current = play;

    const send = useCallback((cmd: number, args: any[] = []) => {
      const node = nativeRef.current;
      if (node) {
        try {
          UIManager.dispatchViewManagerCommand(node, cmd, args);
        } catch {}
      }
    }, []);

    const onPlaybackState = useCallback(
      (s: PlaybackState) => {
        lastStateRef.current = s;
        if (s.error) {
          if (!watchTriedRef.current && !stoppedRef.current) {
            watchTriedRef.current = true;
            onChangeStateRef.current?.('loading');
            send(CMD.loadWatch);
            return;
          }
          onChangeStateRef.current?.('error');
          onFallbackRef.current?.();
          return;
        }
        if (s.videoId && s.videoId !== videoIdRef.current) return;
        if (s.ended && !s.paused) {
          onChangeStateRef.current?.('ended');
          return;
        }
        onChangeStateRef.current?.(s.paused ? 'paused' : 'playing');
      },
      [send]
    );

    // Load the video into the native player whenever the id changes.
    useEffect(() => {
      watchTriedRef.current = false;
      stoppedRef.current = false;
      lastStateRef.current = null;
      send(CMD.loadVideo, [videoId, true]);
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [videoId]);

    // Toggle play/pause from outside (mini-player buttons, screen-off handoff).
    useEffect(() => {
      if (!lastStateRef.current) return;
      if (play) send(CMD.play);
      else send(CMD.pause);
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [play]);

    useImperativeHandle(
      ref,
      () => ({
        pauseVideo: () => send(CMD.pause),
        playVideo: () => send(CMD.play),
        stopVideo: () => {
          stoppedRef.current = true;
          lastStateRef.current = null;
          send(CMD.stop);
        },
        seekTo: (seconds: number) => {
          if (typeof seconds === 'number' && Number.isFinite(seconds) && seconds > 0) {
            send(CMD.seekTo, [seconds]);
          }
        },
        getCurrentTime: () => lastStateRef.current?.currentTime ?? 0,
      }),
      [send]
    );

    return (
      <View style={{ width: '100%', height, backgroundColor: '#000', overflow: 'hidden' }}>
        <NativePlayer
          ref={nativeRef}
          style={{ width: '100%', height: '100%' }}
          videoId={videoId}
          onPlaybackState={onPlaybackState}
        />
      </View>
    );
  }
);