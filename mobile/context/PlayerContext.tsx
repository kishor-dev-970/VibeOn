import { createContext, useContext, useMemo, useState } from 'react';
import type { Song } from '../lib/types';

interface PlayerContextValue {
  currentSong: Song | null;
  isPlaying: boolean;
  showVideo: boolean;
  setCurrentSong: (song: Song | null) => void;
  setIsPlaying: (playing: boolean) => void;
  setShowVideo: (show: boolean) => void;
  playFriendSong: (song: Song) => void;
}

const PlayerContext = createContext<PlayerContextValue>({
  currentSong: null,
  isPlaying: false,
  showVideo: false,
  setCurrentSong: () => {},
  setIsPlaying: () => {},
  setShowVideo: () => {},
  playFriendSong: () => {},
});

export function PlayerProvider({ children }: { children: React.ReactNode }) {
  const [currentSong, setCurrentSong] = useState<Song | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [showVideo, setShowVideo] = useState(false);

  const playFriendSong = (song: Song) => {
    setCurrentSong(song);
    setIsPlaying(true);
    setShowVideo(false);
  };

  const value = useMemo<PlayerContextValue>(
    () => ({ currentSong, isPlaying, showVideo, setCurrentSong, setIsPlaying, setShowVideo, playFriendSong }),
    [currentSong, isPlaying, showVideo]
  );

  return <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>;
}

export function usePlayer(): PlayerContextValue {
  return useContext(PlayerContext);
}
