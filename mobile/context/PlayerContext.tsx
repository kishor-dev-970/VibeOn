import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { AppState } from 'react-native';
import { createAudioPlayer, setAudioModeAsync, type AudioPlayer } from 'expo-audio';
import type { Song } from '../lib/types';
import * as api from '../lib/api';

export interface VideoScreenHandlers {
  getCurrentTime: () => number | undefined | Promise<number | undefined>;
  pause: () => void;
  seekToPlay: (seconds: number) => void;
}

interface PlayerContextValue {
  currentSong: Song | null;
  isPlaying: boolean;
  showVideo: boolean;
  videoScreenActive: boolean;
  audioCurrentTime: number;
  audioDuration: number;
  isSeeking: boolean;
  setCurrentSong: (song: Song | null) => void;
  setIsPlaying: (playing: boolean) => void;
  setShowVideo: (show: boolean) => void;
  playSong: (song: Song, audioMode?: boolean) => void;
  togglePlayPause: () => void;
  stopPlaying: () => void;
  seekTo: (seconds: number) => void;
  registerVideoPlayer: (handlers: VideoScreenHandlers) => void;
  unregisterVideoPlayer: () => void;
  prefetchAudio: (song: Song) => Promise<void>;
  youtubeRef: React.MutableRefObject<any>;
}

const PlayerContext = createContext<PlayerContextValue>({
  currentSong: null,
  isPlaying: false,
  showVideo: true,
  videoScreenActive: false,
  audioCurrentTime: 0,
  audioDuration: 0,
  isSeeking: false,
  setCurrentSong: () => {},
  setIsPlaying: () => {},
  setShowVideo: () => {},
  playSong: () => {},
  togglePlayPause: () => {},
  stopPlaying: () => {},
  seekTo: () => {},
  registerVideoPlayer: () => {},
  unregisterVideoPlayer: () => {},
  prefetchAudio: async () => {},
  youtubeRef: { current: null },
});

export function PlayerProvider({ children }: { children: React.ReactNode }) {
  const [currentSong, setCurrentSong] = useState<Song | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [showVideo, setShowVideo] = useState(true);
  const [videoScreenActive, setVideoScreenActive] = useState(false);
  const [audioCurrentTime, setAudioCurrentTime] = useState(0);
  const [audioDuration, setAudioDuration] = useState(0);
  const [isSeeking, setIsSeeking] = useState(false);
  const youtubeRef = useRef<any>(null);

  const currentSongRef = useRef<Song | null>(null);
  currentSongRef.current = currentSong;
  const isAudioModeRef = useRef(false);
  const showVideoRef = useRef(true);
  showVideoRef.current = showVideo;
  const isPlayingRef = useRef(false);
  isPlayingRef.current = isPlaying;
  const audioCurrentTimeRef = useRef(0);
  const audioDurationRef = useRef(0);
  const backgroundPendingRef = useRef<{ song: Song; offset: number } | null>(null);
  const videoScreenRef = useRef<VideoScreenHandlers | null>(null);
  const audioUrlCacheRef = useRef<{ videoId: string; url: string } | null>(null);
  const lastVideoTimeRef = useRef(0);

  const playerRef = useRef<AudioPlayer | null>(null);
  const audioConfiguredRef = useRef(false);
  const isSeekingRef = useRef(false);

  const ensureAudioConfig = useCallback(async () => {
    if (audioConfiguredRef.current) return;
    try {
      await setAudioModeAsync({
        playsInSilentMode: true,
        shouldPlayInBackground: true,
        interruptionMode: 'doNotMix',
      });
      audioConfiguredRef.current = true;
    } catch {}
  }, []);

  const getPlayer = useCallback((): AudioPlayer | null => {
    if (!playerRef.current) {
      try {
        playerRef.current = createAudioPlayer(null);
        try {
          playerRef.current.addListener('playbackStatusUpdate', (status: any) => {
            if (status) {
              if (typeof status.currentTime === 'number' && !isSeekingRef.current) {
                setAudioCurrentTime(status.currentTime);
                audioCurrentTimeRef.current = status.currentTime;
              }
              if (typeof status.duration === 'number' && status.duration > 0) {
                setAudioDuration(status.duration);
                audioDurationRef.current = status.duration;
              }
              if (typeof status.playing === 'boolean') {
                setIsPlaying(status.playing);
              }
              if (status.didJustFinish) {
                backgroundPendingRef.current = null;
                setIsPlaying(false);
                setAudioCurrentTime(0);
                audioCurrentTimeRef.current = 0;
                const s = currentSongRef.current;
                if (s) {
                  try { api.updateNowPlaying(s, false); } catch {}
                }
              }
            }
          });
        } catch {}
      } catch {
        return null;
      }
    }
    return playerRef.current;
  }, []);

  const prefetchAudio = useCallback(async (song: Song) => {
    if (audioUrlCacheRef.current?.videoId === song.videoId) return;
    try {
      const data = await api.fetchAudioStream(song.videoId);
      audioUrlCacheRef.current = { videoId: song.videoId, url: data.audioUrl };
    } catch (e) {
      audioUrlCacheRef.current = null;
    }
  }, []);

  const loadAudio = useCallback(
    async (song: Song, offsetSeconds?: number) => {
      await ensureAudioConfig();
      let audioUrl: string;
      try {
        const data = await api.fetchAudioStream(song.videoId);
        audioUrl = data.audioUrl;
      } catch {
        setIsPlaying(false);
        return;
      }
      if (audioUrlCacheRef.current?.videoId !== song.videoId) {
        audioUrlCacheRef.current = { videoId: song.videoId, url: audioUrl };
      }
      const p = getPlayer();
      if (!p) return;
      try {
        setAudioCurrentTime(0);
        setAudioDuration(0);
        audioCurrentTimeRef.current = 0;
        p.replace({ uri: audioUrl, name: song.title });
        p.loop = false;
        p.play();
        if (typeof offsetSeconds === 'number' && offsetSeconds > 0) {
          try { p.seekTo(offsetSeconds).catch(() => {}); } catch {}
        }
        try {
          p.setActiveForLockScreen(true, {
            title: song.title,
            artist: song.channel,
            artworkUrl: song.thumbnailUrl || undefined,
          });
        } catch {}
        setIsPlaying(true);
      } catch {
        setIsPlaying(false);
      }
    },
    [ensureAudioConfig, getPlayer]
  );

  // Screen off while video is playing -> switch to background audio.
  // Screen on again -> resume the video from the audio position.
  useEffect(() => {
    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'background') {
        const song = currentSongRef.current;
        if (song && isPlayingRef.current && showVideoRef.current) {
          const cached = audioUrlCacheRef.current;
          const url = cached && cached.videoId === song.videoId ? cached.url : null;
          if (url) {
            let offset = lastVideoTimeRef.current > 0 ? lastVideoTimeRef.current : 0;
            try { videoScreenRef.current?.pause?.(); } catch {}
            try { youtubeRef.current?.pauseVideo?.(); } catch {}
            backgroundPendingRef.current = { song, offset };
            isAudioModeRef.current = true;
            setShowVideo(false);
            try {
              const p = getPlayer();
              if (p) {
                p.replace({ uri: url, name: song.title });
                p.loop = false;
                p.play();
                try {
                  p.setActiveForLockScreen(true, {
                    title: song.title,
                    artist: song.channel,
                    artworkUrl: song.thumbnailUrl || undefined,
                  });
                } catch {}
              }
            } catch {}
            setIsPlaying(true);
          } else {
            let offset = lastVideoTimeRef.current > 0 ? lastVideoTimeRef.current : 0;
            try { videoScreenRef.current?.pause?.(); } catch {}
            try { youtubeRef.current?.pauseVideo?.(); } catch {}
            backgroundPendingRef.current = { song, offset };
            isAudioModeRef.current = true;
            setShowVideo(false);
            setIsPlaying(true);
            api.fetchAudioStream(song.videoId)
              .then((data) => {
                audioUrlCacheRef.current = { videoId: song.videoId, url: data.audioUrl };
                if (backgroundPendingRef.current?.song.videoId !== song.videoId) return;
                const p = getPlayer();
                if (!p) return;
                p.replace({ uri: data.audioUrl, name: song.title });
                p.loop = false;
                p.play();
                try {
                  p.setActiveForLockScreen(true, {
                    title: song.title,
                    artist: song.channel,
                    artworkUrl: song.thumbnailUrl || undefined,
                  });
                } catch {}
              })
              .catch(() => {
                if (backgroundPendingRef.current?.song.videoId === song.videoId) {
                  backgroundPendingRef.current = null;
                  isAudioModeRef.current = false;
                  setIsPlaying(false);
                }
              });
          }
        }
      } else if (state === 'active') {
        const pending = backgroundPendingRef.current;
        if (pending) {
          backgroundPendingRef.current = null;
          const offset =
            audioCurrentTimeRef.current > 0 ? audioCurrentTimeRef.current : pending.offset;
          try {
            const p = getPlayer();
            p?.pause();
            p?.replace(null);
          } catch {}
          isAudioModeRef.current = false;
          setShowVideo(true);
          setIsPlaying(true);
          try { videoScreenRef.current?.seekToPlay?.(offset); } catch {}
          if (!videoScreenRef.current) {
            try { youtubeRef.current?.seekTo?.(offset, true); } catch {}
          }
        }
      }
    });
    return () => sub.remove();
  }, [loadAudio, getPlayer, youtubeRef]);

  const stopPlaying = useCallback(() => {
    backgroundPendingRef.current = null;
    if (youtubeRef.current) {
      try { youtubeRef.current.stopVideo?.(); } catch {}
    }
    try {
      const p = getPlayer();
      p?.pause();
      p?.replace(null);
      try { p?.clearLockScreenControls?.(); } catch {}
    } catch {}
    isAudioModeRef.current = false;
    setCurrentSong(null);
    setIsPlaying(false);
    setShowVideo(true);
    setAudioCurrentTime(0);
    audioCurrentTimeRef.current = 0;
    setAudioDuration(0);
    audioDurationRef.current = 0;
    try { api.clearNowPlaying(); } catch {}
  }, [getPlayer]);

  const playSong = useCallback(
    (song: Song, audioMode = false) => {
      backgroundPendingRef.current = null;
      if (youtubeRef.current) {
        try { youtubeRef.current.stopVideo?.(); } catch {}
      }
      try { getPlayer()?.pause(); } catch {}

      isAudioModeRef.current = audioMode;
      setCurrentSong(song);
      setShowVideo(!audioMode);
      setAudioCurrentTime(0);
      audioCurrentTimeRef.current = 0;
      setAudioDuration(0);
      audioDurationRef.current = 0;
      try { api.updateNowPlaying(song, true); } catch {}

      if (audioMode) {
        setIsPlaying(true);
        loadAudio(song);
      } else {
        setIsPlaying(true);
      }
    },
    [getPlayer, loadAudio]
  );

  const togglePlayPause = useCallback(() => {
    if (!currentSong) return;
    if (isAudioModeRef.current) {
      const p = getPlayer();
      if (!p) return;
      try {
        if (isPlaying) {
          p.pause();
          setIsPlaying(false);
        } else {
          p.play();
          setIsPlaying(true);
        }
      } catch {}
      return;
    }
    if (!youtubeRef.current) {
      setIsPlaying(!isPlaying);
      return;
    }
    try {
      if (isPlaying) {
        youtubeRef.current.pauseVideo?.();
      } else {
        youtubeRef.current.playVideo?.();
      }
    } catch {}
  }, [currentSong, isPlaying, getPlayer]);

  const seekTo = useCallback(
    (seconds: number) => {
      if (!isAudioModeRef.current) return;
      const p = getPlayer();
      if (!p) return;
      isSeekingRef.current = true;
      setIsSeeking(true);
      setAudioCurrentTime(seconds);
      p.seekTo(seconds).finally(() => {
        isSeekingRef.current = false;
        setIsSeeking(false);
      });
    },
    [getPlayer]
  );

  const registerVideoPlayer = useCallback((handlers: VideoScreenHandlers) => {
    videoScreenRef.current = handlers;
    setVideoScreenActive(true);
  }, []);

  const unregisterVideoPlayer = useCallback(() => {
    videoScreenRef.current = null;
    setVideoScreenActive(false);
  }, []);

  useEffect(() => {
    if (!videoScreenActive) return;
    const id = setInterval(() => {
      try {
        Promise.resolve(videoScreenRef.current?.getCurrentTime?.()).then((t) => {
          if (typeof t === 'number' && isFinite(t) && t > 0) lastVideoTimeRef.current = t;
        }).catch(() => {});
      } catch {}
    }, 2500);
    return () => clearInterval(id);
  }, [videoScreenActive]);

  const value = useMemo<PlayerContextValue>(
    () => ({
      currentSong,
      isPlaying,
      showVideo,
      videoScreenActive,
      audioCurrentTime,
      audioDuration,
      isSeeking,
      setCurrentSong,
      setIsPlaying,
      setShowVideo,
      playSong,
      togglePlayPause,
      stopPlaying,
      seekTo,
      registerVideoPlayer,
      unregisterVideoPlayer,
      prefetchAudio,
      youtubeRef,
    }),
    [
      currentSong,
      isPlaying,
      showVideo,
      videoScreenActive,
      audioCurrentTime,
      audioDuration,
      isSeeking,
      playSong,
      togglePlayPause,
      stopPlaying,
      seekTo,
      registerVideoPlayer,
      unregisterVideoPlayer,
      prefetchAudio,
    ]
  );

  return <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>;
}

export function usePlayer(): PlayerContextValue {
  return useContext(PlayerContext);
}