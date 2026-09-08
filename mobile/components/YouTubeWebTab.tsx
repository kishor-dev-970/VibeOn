import { useEffect, useRef } from 'react';
import {
  DeviceEventEmitter,
  requireNativeComponent,
  View,
  AppState,
  NativeModules,
  UIManager,
  BackHandler,
} from 'react-native';
import { useIsFocused } from 'expo-router';
import { YOUTUBE_TAB_GO_HOME } from './AppTabBar';
import * as api from '../lib/api';
import type { Song } from '../lib/types';

const NativePlayer: any = requireNativeComponent('BraveliteYouTubeView');
const LocalAudio: any = (NativeModules as any).LocalAudio;
const BraveliteFullscreen: any = (NativeModules as any).BraveliteFullscreen;
const CMD = { loadVideo: 1, loadWatch: 2, play: 3, pause: 4, seekTo: 5, stop: 6, loadUrl: 7, nextTrack: 8, prevTrack: 9, goBack: 10 };

export const YTMUSIC_TAB_GO_HOME = 'ytMusicTabPressed';

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
  const lastReportedState = useRef<string>('');
  const currentSongRef = useRef<Song | null>(null);

  const onTopPlaybackState = (e: any) => {
    const n = e?.nativeEvent;
    if (!n) return;
    const videoId = (n.videoId || '').trim();
    const title = (n.title || '').trim();
    const artist = (n.artist || '').trim() || (url.includes('music.youtube.com') ? 'YouTube Music' : 'YouTube');
    const thumbnailUrl = n.thumbnailUrl || (videoId ? `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg` : '');
    const paused = !!n.paused;
    const isPlaying = !paused && !!videoId;

    lastState.current = {
      videoId,
      title,
      paused,
    };

    if (!videoId) return;

    // Detect state change: track switch or play/pause transition
    const stateKey = `${videoId}:${isPlaying}`;
    if (stateKey !== lastReportedState.current) {
      lastReportedState.current = stateKey;
      const song: Song = {
        videoId,
        title: title || 'Now Playing',
        channel: artist,
        thumbnailUrl,
        source: 'youtube',
      };
      currentSongRef.current = song;
      try {
        api.updateNowPlaying(song, isPlaying);
      } catch {}
    }
  };

  // Heartbeat while playing: keep now_playing row alive in Supabase (TTL is 15 min)
  useEffect(() => {
    const interval = setInterval(() => {
      if (!lastState.current.paused && currentSongRef.current) {
        try {
          api.updateNowPlaying(currentSongRef.current, true);
        } catch {}
      }
    }, 4 * 60 * 1000);
    return () => clearInterval(interval);
  }, []);

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

  // When leaving this tab, close fullscreen and stop video player
  useEffect(() => {
    if (!isFocused) {
      try {
        BraveliteFullscreen?.closeYouTubePlayer?.();
      } catch {}
    }
  }, [isFocused]);

  // When the user taps the tab icon in the main bar, reload home URL
  useEffect(() => {
    const eventName = url.includes('music.youtube.com') ? YTMUSIC_TAB_GO_HOME : YOUTUBE_TAB_GO_HOME;
    const sub = DeviceEventEmitter.addListener(eventName, () => {
      try {
        if (nativeRef.current) {
          UIManager.dispatchViewManagerCommand(nativeRef.current, CMD.loadUrl, [url]);
        }
      } catch {}
    });
    return () => sub.remove();
  }, [url]);

  // Forward Next/Previous button presses from PlayerContext to active web player
  useEffect(() => {
    const subNext = DeviceEventEmitter.addListener('onWebNextTrack', () => {
      if (isFocused && nativeRef.current) {
        try {
          UIManager.dispatchViewManagerCommand(nativeRef.current, CMD.nextTrack, []);
        } catch {}
      }
    });
    const subPrev = DeviceEventEmitter.addListener('onWebPrevTrack', () => {
      if (isFocused && nativeRef.current) {
        try {
          UIManager.dispatchViewManagerCommand(nativeRef.current, CMD.prevTrack, []);
        } catch {}
      }
    });
    return () => {
      subNext.remove();
      subPrev.remove();
    };
  }, [isFocused]);

  // Navigate back inside WebView on Android hardware back button
  useEffect(() => {
    if (!isFocused) return;
    const onBackPress = () => {
      if (nativeRef.current) {
        try {
          UIManager.dispatchViewManagerCommand(nativeRef.current, CMD.goBack, []);
          return true;
        } catch {}
      }
      return false;
    };
    const sub = BackHandler.addEventListener('hardwareBackPress', onBackPress);
    return () => sub.remove();
  }, [isFocused]);

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