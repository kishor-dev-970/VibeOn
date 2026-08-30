export const Colors = {
  bg: '#0F0A1F',
  bgCard: '#1A1333',
  bgCardLight: '#231B42',
  primary: '#7C3AED',
  primaryLight: '#A78BFA',
  secondary: '#06B6D4',
  accent: '#EC4899',
  text: '#FFFFFF',
  textMuted: '#9CA3AF',
  textSubtle: '#6B7280',
  success: '#10B981',
  error: '#EF4444',
  border: '#2D2550',
  tabBarBg: '#130D28',
  searchBg: '#1A1333',
  gradient: ['#7C3AED', '#6366F1', '#06B6D4'] as const,
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
  bg: 'rgba(26, 19, 51, 0.55)',
  bgStrong: 'rgba(18, 13, 38, 0.82)',
  border: 'rgba(167, 139, 250, 0.22)',
  borderStrong: 'rgba(167, 139, 250, 0.45)',
};

export const Shadows = {
  glow: {
    shadowColor: '#7C3AED',
    shadowOpacity: 0.45,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 8 },
    elevation: 12,
  },
  glowCyan: {
    shadowColor: '#06B6D4',
    shadowOpacity: 0.45,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 8 },
    elevation: 12,
  },
  soft: {
    shadowColor: '#000000',
    shadowOpacity: 0.35,
    shadowRadius: 24,
    shadowOffset: { width: 0, height: -10 },
    elevation: 16,
  },
};

export const Gradients = {
  brand: ['#7C3AED', '#6366F1', '#06B6D4'],
  brandMuted: ['rgba(124, 58, 237, 0.55)', 'rgba(99, 102, 241, 0.32)', 'rgba(6, 182, 212, 0.20)'],
  play: ['#7C3AED', '#EC4899'],
  violet: ['#7C3AED', '#6366F1'],
  cyan: ['#6366F1', '#06B6D4'],
  pink: ['#EC4899', '#7C3AED'],
  surface: ['rgba(124, 58, 237, 0.18)', 'rgba(6, 182, 212, 0.10)'],
};
