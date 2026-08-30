import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { DeviceEventEmitter, NativeModules } from 'react-native';
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
  inPipMode: boolean;
  audioCurrentTime: number;
  audioDuration: number;
  isSeeking: boolean;
  queue: Song[];
  queueIndex: number;
  setCurrentSong: (song: Song | null) => void;
  setIsPlaying: (playing: boolean) => void;
  setShowVideo: (show: boolean) => void;
  playSong: (song: Song, audioMode?: boolean) => void;
  playQueue: (songs: Song[], startIndex?: number) => void;
  playNext: () => void;
  playPrevious: () => void;
  togglePlayPause: () => void;
  stopPlaying: () => void;
  seekTo: (seconds: number) => void;
  registerVideoPlayer: (handlers: VideoScreenHandlers) => void;
  unregisterVideoPlayer: () => void;
  prefetchAudio: (song: Song) => Promise<void>;
  youtubeRef: React.MutableRefObject<any>;
}

const LocalAudio = (NativeModules as any).LocalAudio;

const PlayerContext = createContext<PlayerContextValue>({
  currentSong: null,
  isPlaying: false,
  showVideo: false,
  videoScreenActive: false,
  inPipMode: false,
  audioCurrentTime: 0,
  audioDuration: 0,
  isSeeking: false,
  queue: [],
  queueIndex: -1,
  setCurrentSong: () => {},
  setIsPlaying: () => {},
  setShowVideo: () => {},
  playSong: () => {},
  playQueue: () => {},
  playNext: () => {},
  playPrevious: () => {},
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
  const [showVideo, setShowVideo] = useState(false);
  const [videoScreenActive, setVideoScreenActive] = useState(false);
  const [inPipMode, setInPipMode] = useState(false);
  const [audioCurrentTime, setAudioCurrentTime] = useState(0);
  const [audioDuration, setAudioDuration] = useState(0);
  const [isSeeking, setIsSeeking] = useState(false);
  const [queue, setQueue] = useState<Song[]>([]);
  const [queueIndex, setQueueIndex] = useState(-1);
  const youtubeRef = useRef<any>(null);

  const currentSongRef = useRef<Song | null>(null);
  currentSongRef.current = currentSong;
  const isPlayingRef = useRef(false);
  isPlayingRef.current = isPlaying;
  const audioCurrentTimeRef = useRef(0);
  const isSeekingRef = useRef(false);
  const videoScreenRef = useRef<VideoScreenHandlers | null>(null);
  const queueRef = useRef<Song[]>([]);
  queueRef.current = queue;
  const queueIndexRef = useRef(-1);
  queueIndexRef.current = queueIndex;

  // Mirror native playback service state into React state.
  useEffect(() => {
    const sub = DeviceEventEmitter.addListener('onStateChange', (e: any) => {
      if (!e) return;
      if (typeof e.playing === 'boolean') {
        const wasPlaying = isPlayingRef.current;
        setIsPlaying(e.playing);
        isPlayingRef.current = e.playing;

        if (wasPlaying && !e.playing && currentSongRef.current) {
          try { api.updateNowPlaying(currentSongRef.current, false); } catch {}
        }
      }
      if (typeof e.positionMs === 'number' && !isSeekingRef.current) {
        const secs = e.positionMs / 1000;
        setAudioCurrentTime(secs);
        audioCurrentTimeRef.current = secs;
      }
      if ( typeof e.durationMs === 'number' && e.durationMs > 0) {
        setAudioDuration(e.durationMs / 1000);
      }
    });
    return () => sub.remove();
  }, []);

  // Heartbeat: keep now_playing row alive while playing (bump every 4 min, TTL is 10 min).
  useEffect(() => {
    if (!isPlaying || !currentSong) return;
    const id = setInterval(() => {
      try { api.updateNowPlaying(currentSongRef.current!, true); } catch {}
    }, 4 * 60 * 1000);
    return () => clearInterval(id);
  }, [isPlaying, currentSong]);

  // Auto-advance when a song finishes
  useEffect(() => {
    const sub = DeviceEventEmitter.addListener('onPlaybackComplete', (_e: any) => {
      const q = queueRef.current;
      const idx = queueIndexRef.current;
      if (q.length === 0) return;
      const nextIdx = idx + 1;
      if (nextIdx < q.length) {
        const nextSong = q[nextIdx];
        setQueueIndex(nextIdx);
        setCurrentSong(nextSong);
        setIsPlaying(true);
        setAudioCurrentTime(0);
        setAudioDuration(0);
        audioCurrentTimeRef.current = 0;
        try { api.updateNowPlaying(nextSong, true); } catch {}
        try { LocalAudio?.play?.(nextSong.videoId, nextSong.title); } catch {}
      } else {
        setIsPlaying(false);
      }
    });
    return () => sub.remove();
  }, []);

  // Stop from the notification close / PiP stop button: mirror stopPlaying()
  // so the in-app mini-player doesn't stay stale (and a second stop doesn't
  // try to relaunch the service, which crashed the whole app before).
  useEffect(() => {
    const sub = DeviceEventEmitter.addListener('onPlaybackStopped', (e: any) => {
      const stoppedId = e?.videoId;
      const currentId = currentSongRef.current?.videoId;
      // If a new song already started (queue auto-advance after a complete),
      // the stopped event belongs to the old song - keep the player as-is.
      if (stoppedId && currentId && stoppedId !== currentId) return;
      setCurrentSong(null);
      setIsPlaying(false);
      setAudioCurrentTime(0);
      setAudioDuration(0);
      audioCurrentTimeRef.current = 0;
      setQueue([]);
      setQueueIndex(-1);
      try {
        api.clearNowPlaying();
      } catch {}
    });
    return () => sub.remove();
  }, []);

  // Keep native PiP auto-enter flag synced with playback state.
  useEffect(() => {
    try {
      LocalAudio?.setPipAutoEnter?.(isPlaying);
    } catch {}
  }, [isPlaying]);
  useEffect(() => {
    const sub = DeviceEventEmitter.addListener('onPipModeChanged', (e: any) => {
      setInPipMode(!!e?.isPip);
    });
    return () => sub.remove();
  }, []);

  const stopPlaying = useCallback(() => {
    try {
      LocalAudio?.stop?.();
    } catch {}
    setCurrentSong(null);
    setIsPlaying(false);
    setAudioCurrentTime(0);
    setAudioDuration(0);
    audioCurrentTimeRef.current = 0;
    setQueue([]);
    setQueueIndex(-1);
    try {
      api.clearNowPlaying();
    } catch {}
  }, []);

  const playSong = useCallback((song: Song, _audioMode = true) => {
    setCurrentSong(song);
    setIsPlaying(true);
    setAudioCurrentTime(0);
    setAudioDuration(0);
    audioCurrentTimeRef.current = 0;
    setQueue([song]);
    setQueueIndex(0);
    try {
      api.updateNowPlaying(song, true);
    } catch {}
    try {
      LocalAudio?.play?.(song.videoId, song.title);
    } catch {}
  }, []);

  const playQueueSongs = useCallback((songs: Song[], startIndex = 0) => {
    if (songs.length === 0) return;
    const song = songs[startIndex];
    setQueue(songs);
    setQueueIndex(startIndex);
    setCurrentSong(song);
    setIsPlaying(true);
    setAudioCurrentTime(0);
    setAudioDuration(0);
    audioCurrentTimeRef.current = 0;
    try { api.updateNowPlaying(song, true); } catch {}
    try { LocalAudio?.play?.(song.videoId, song.title); } catch {}
  }, []);

  const jumpToIndex = useCallback((idx: number) => {
    const q = queueRef.current;
    if (idx < 0 || idx >= q.length) return;
    const song = q[idx];
    setQueueIndex(idx);
    setCurrentSong(song);
    setIsPlaying(true);
    setAudioCurrentTime(0);
    setAudioDuration(0);
    audioCurrentTimeRef.current = 0;
    try { api.updateNowPlaying(song, true); } catch {}
    try { LocalAudio?.play?.(song.videoId, song.title); } catch {}
  }, []);

  const playNextSong = useCallback(() => {
    jumpToIndex(queueIndexRef.current + 1);
  }, [jumpToIndex]);

  const playPrevSong = useCallback(() => {
    jumpToIndex(queueIndexRef.current - 1);
  }, [jumpToIndex]);

  // Android PiP window previous/next buttons (route into the same queue logic).
  useEffect(() => {
    const sub = DeviceEventEmitter.addListener('onPipCommand', (e: any) => {
      const cmd = e?.command;
      if (cmd === 'next') jumpToIndex(queueIndexRef.current + 1);
      else if (cmd === 'prev') jumpToIndex(queueIndexRef.current - 1);
    });
    return () => sub.remove();
  }, [jumpToIndex]);

  const togglePlayPause = useCallback(() => {
    if (!currentSongRef.current) return;
    try {
      LocalAudio?.toggle?.();
    } catch {}
  }, []);

  const seekTo = useCallback((seconds: number) => {
    try {
      LocalAudio?.seek?.(Math.round(seconds * 1000));
    } catch {}
    setAudioCurrentTime(seconds);
  }, []);

  const prefetchAudio = useCallback(async () => {}, []);

  const registerVideoPlayer = useCallback((handlers: VideoScreenHandlers) => {
    videoScreenRef.current = handlers;
    setVideoScreenActive(true);
  }, []);

  const unregisterVideoPlayer = useCallback(() => {
    videoScreenRef.current = null;
    setVideoScreenActive(false);
  }, []);

  const value = useMemo<PlayerContextValue>(
    () => ({
      currentSong,
      isPlaying,
      showVideo,
      videoScreenActive,
      inPipMode,
      audioCurrentTime,
      audioDuration,
      isSeeking,
      queue,
      queueIndex,
      setCurrentSong,
      setIsPlaying,
      setShowVideo,
      playSong,
      playQueue: playQueueSongs,
      playNext: playNextSong,
      playPrevious: playPrevSong,
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
      inPipMode,
      audioCurrentTime,
      audioDuration,
      isSeeking,
      queue,
      queueIndex,
      playSong,
      playQueueSongs,
      playNextSong,
      playPrevSong,
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
