import { useEffect, useRef } from 'react';
import {
  DeviceEventEmitter,
  requireNativeComponent,
  View,
  AppState,
  NativeModules,
  UIManager,
} from 'react-native';
import { useIsFocused } from 'expo-router';
import { YOUTUBE_TAB_GO_HOME } from './AppTabBar';

const NativePlayer: any = requireNativeComponent('BraveliteYouTubeView');
const LocalAudio: any = (NativeModules as any).LocalAudio;
const BraveliteFullscreen: any = (NativeModules as any).BraveliteFullscreen;
const CMD = { loadVideo: 1, loadWatch: 2, play: 3, pause: 4, seekTo: 5, stop: 6, loadUrl: 7 };

interface YouTubeWebTabProps {
  url: string;
}

export function YouTubeWebTab({ url }: YouTubeWebTabProps) {
  const isFocused = useIsFocused();
  const nativeRef = useRef<any>(null);
  const lastState = useRef<{
    videoId: string;
    title: string;
    paused: boolean;
  }>({ videoId: '', title: '', paused: true });

  const onTopPlaybackState = (e: any) => {
    const n = e?.nativeEvent;
    if (!n) return;
    lastState.current = {
      videoId: n.videoId || '',
      title: n.title || '',
      paused: n.paused,
    };
  };

  useEffect(() => {
    const sub = AppState.addEventListener('change', (state: string) => {
      if (state === 'background') {
        const st = lastState.current;
        if (st.videoId && !st.paused) {
          try {
            LocalAudio?.playCaptured?.(st.videoId, st.title);
          } catch {}
        }
      }
    });
    return () => sub.remove();
  }, []);

  // When leaving the YouTube tab (e.g. tapping Home), close fullscreen and stop the player.
  useEffect(() => {
    if (!isFocused) {
      try {
        BraveliteFullscreen?.closeYouTubePlayer?.();
      } catch {}
    }
  }, [isFocused]);

  // When the user taps the YouTube tab in the main app bar, go back to YouTube home.
  useEffect(() => {
    const sub = DeviceEventEmitter.addListener(YOUTUBE_TAB_GO_HOME, () => {
      try {
        if (nativeRef.current) {
          UIManager.dispatchViewManagerCommand(nativeRef.current, CMD.loadUrl, [url]);
        }
      } catch {}
    });
    return () => sub.remove();
  }, [url]);

  return (
    <View style={{ flex: 1, backgroundColor: '#0F0F0F' }}>
      <NativePlayer
        ref={nativeRef}
        style={{ flex: 1 }}
        url={url}
        onTopPlaybackState={onTopPlaybackState}
      />
    </View>
  );
}