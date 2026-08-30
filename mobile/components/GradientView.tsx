import { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

function parseColor(c: string) {
  const s = c.trim();
  if (s.startsWith('#')) {
    let h = s.slice(1);
    if (h.length === 3) h = h.split('').map((x) => x + x).join('');
    const num = parseInt(h, 16);
    return { r: (num >> 16) & 255, g: (num >> 8) & 255, b: num & 255, a: 1 };
  }
  const m = s.match(/rgba?\(([^)]+)\)/);
  if (m) {
    const p = m[1].split(',').map((x) => parseFloat(x));
    return { r: p[0], g: p[1], b: p[2], a: p[3] ?? 1 };
  }
  return { r: 0, g: 0, b: 0, a: 1 };
}

function mix(a: string, b: string, t: number) {
  const c1 = parseColor(a);
  const c2 = parseColor(b);
  const r = Math.round(c1.r + (c2.r - c1.r) * t);
  const g = Math.round(c1.g + (c2.g - c1.g) * t);
  const bl = Math.round(c1.b + (c2.b - c1.b) * t);
  const alpha = c1.a + (c2.a - c1.a) * t;
  return `rgba(${r}, ${g}, ${bl}, ${alpha})`;
}

function lerpColors(colors: readonly string[], t: number) {
  if (colors.length === 1) return colors[0];
  const seg = colors.length - 1;
  const scaled = Math.max(0, Math.min(1, t)) * seg;
  const i = Math.min(Math.floor(scaled), seg - 1);
  return mix(colors[i], colors[i + 1], scaled - i);
}

interface Props {
  colors: readonly string[];
  direction?: 'vertical' | 'horizontal';
  style?: any;
  children?: ReactNode;
}

const STEPS = 28;

export default function GradientView({ colors, direction = 'vertical', style, children }: Props) {
  const arr = [];
  for (let i = 0; i < STEPS; i++) {
    arr.push(
      <View key={i} style={{ flex: 1, backgroundColor: lerpColors([...colors], i / (STEPS - 1)) }} />
    );
  }
  return (
    <View style={[style, { overflow: 'hidden' }]}>
      <View
        pointerEvents="none"
        style={[StyleSheet.absoluteFill, { flexDirection: direction === 'horizontal' ? 'row' : 'column' }]}
      >
        {arr}
      </View>
      {children}
    </View>
  );
}
