export const Colors = {
  bg: '#0A0718',
  bgCard: '#130E26',
  bgCardLight: '#1F163D',
  primary: '#8B5CF6',
  primaryLight: '#C084FC',
  secondary: '#06B6D4',
  accent: '#EC4899',
  text: '#FFFFFF',
  textMuted: '#94A3B8',
  textSubtle: '#64748B',
  success: '#10B981',
  error: '#EF4444',
  border: '#261D45',
  borderLight: 'rgba(192, 132, 252, 0.22)',
  tabBarBg: '#0D091F',
  searchBg: '#161030',
  gradient: ['#8B5CF6', '#6366F1', '#06B6D4'] as const,
};

export const Spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
};

export const BorderRadius = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 28,
  full: 999,
};

export const Glass = {
  bg: 'rgba(22, 16, 44, 0.65)',
  bgStrong: 'rgba(14, 9, 32, 0.90)',
  border: 'rgba(167, 139, 250, 0.20)',
  borderStrong: 'rgba(192, 132, 252, 0.45)',
};

export const Shadows = {
  glow: {
    shadowColor: '#8B5CF6',
    shadowOpacity: 0.45,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 6 },
    elevation: 10,
  },
  glowCyan: {
    shadowColor: '#06B6D4',
    shadowOpacity: 0.45,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 6 },
    elevation: 10,
  },
  glowPink: {
    shadowColor: '#EC4899',
    shadowOpacity: 0.45,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 6 },
    elevation: 10,
  },
  soft: {
    shadowColor: '#000000',
    shadowOpacity: 0.40,
    shadowRadius: 20,
    shadowOffset: { width: 0, height: -6 },
    elevation: 14,
  },
};

export const Gradients = {
  brand: ['#8B5CF6', '#6366F1', '#06B6D4'],
  brandMuted: ['rgba(139, 92, 246, 0.50)', 'rgba(99, 102, 241, 0.28)', 'rgba(6, 182, 212, 0.18)'],
  play: ['#8B5CF6', '#EC4899'],
  violet: ['#8B5CF6', '#6366F1'],
  cyan: ['#6366F1', '#06B6D4'],
  pink: ['#EC4899', '#8B5CF6'],
  surface: ['rgba(139, 92, 246, 0.16)', 'rgba(6, 182, 212, 0.08)'],
};

