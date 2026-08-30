import { useEffect } from 'react';
import { requireNativeComponent, View, AppState, NativeModules } from 'react-native';
import { useIsFocused } from 'expo-router';

const NativePlayer: any = requireNativeComponent('BraveliteYouTubeView');
const LocalAudio: any = (NativeModules as any).LocalAudio;
const BraveliteFullscreen: any = (NativeModules as any).BraveliteFullscreen;

interface YouTubeWebTabProps {
  url: string;
}

export function YouTubeWebTab({ url }: YouTubeWebTabProps) {
  const isFocused = useIsFocused();
  const lastState = (require('react') as any).useRef<{
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

  return (
    <View style={{ flex: 1, backgroundColor: '#0F0F0F' }}>
      <NativePlayer
        style={{ flex: 1 }}
        url={url}
        onTopPlaybackState={onTopPlaybackState}
      />
    </View>
  );
}
