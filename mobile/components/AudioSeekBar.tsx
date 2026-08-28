import { useRef, useState } from 'react';
import {
  LayoutChangeEvent,
  PanResponder,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Colors } from '../lib/theme';
import { usePlayer } from '../context/PlayerContext';

function formatTime(sec: number): string {
  if (!isFinite(sec) || sec < 0) sec = 0;
  const m = Math.floor(sec / 60);
  const s = Math.floor(sec % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export default function AudioSeekBar({ trackColor }: { trackColor?: string }) {
  const { audioCurrentTime, audioDuration, seekTo } = usePlayer();
  const widthRef = useRef(0);
  const [dragging, setDragging] = useState(false);
  const [dragValue, setDragValue] = useState(0);

  const duration = audioDuration > 0 ? audioDuration : 0;
  const progress = dragging ? dragValue : audioCurrentTime;
  const ratio = duration > 0 ? Math.min(Math.max(progress / duration, 0), 1) : 0;

  const durationRef = useRef(duration);
  durationRef.current = duration;
  const seekToRef = useRef(seekTo);
  seekToRef.current = seekTo;
  const dragValueRef = useRef(0);

  const valueFromX = (x: number) => {
    const d = durationRef.current;
    if (widthRef.current <= 0 || d <= 0) return 0;
    return Math.max(0, Math.min((x / widthRef.current) * d, d));
  };

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: (evt) => {
        setDragging(true);
        const v = valueFromX(evt.nativeEvent.locationX);
        dragValueRef.current = v;
        setDragValue(v);
      },
      onPanResponderMove: (evt) => {
        const v = valueFromX(evt.nativeEvent.locationX);
        dragValueRef.current = v;
        setDragValue(v);
      },
      onPanResponderRelease: () => {
        if (durationRef.current > 0) {
          seekToRef.current?.(dragValueRef.current);
        }
        setDragging(false);
      },
      onPanResponderTerminate: () => {
        setDragging(false);
      },
    })
  ).current;

  const onLayout = (e: LayoutChangeEvent) => {
    widthRef.current = e.nativeEvent.layout.width;
  };

  return (
    <View style={styles.container}>
      {duration > 0 && (
        <View
          style={styles.barArea}
          onLayout={onLayout}
          {...panResponder.panHandlers}
        >
          <View style={styles.track}>
            <View
              style={[
                styles.fill,
                { width: `${ratio * 100}%`, backgroundColor: trackColor ?? Colors.primary },
              ]}
            />
            <View
              style={[
                styles.thumb,
                { left: `${ratio * 100}%`, backgroundColor: trackColor ?? Colors.primary },
              ]}
            />
          </View>
        </View>
      )}
      <View style={styles.timeRow}>
        <Text style={styles.time}>{formatTime(progress)}</Text>
        <Text style={styles.time}>{formatTime(duration)}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 20,
    marginTop: 8,
  },
  barArea: {
    height: 30,
    justifyContent: 'center',
  },
  track: {
    height: 4,
    borderRadius: 2,
    backgroundColor: Colors.bgCardLight,
    justifyContent: 'center',
  },
  fill: {
    height: 4,
    borderRadius: 2,
  },
  thumb: {
    position: 'absolute',
    width: 14,
    height: 14,
    borderRadius: 7,
    marginLeft: -7,
    top: -5,
  },
  timeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 2,
  },
  time: {
    fontSize: 11,
    color: Colors.textMuted,
    fontVariant: ['tabular-nums'],
  },
});
