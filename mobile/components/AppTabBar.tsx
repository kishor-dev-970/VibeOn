import { DeviceEventEmitter, Pressable, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

export const YOUTUBE_TAB_GO_HOME = 'youtubeTabPressed';

interface TabItemConfig {
  title: string;
  renderIcon: (color: string) => React.ReactNode;
}

const TAB_CONFIG: Record<string, TabItemConfig> = {
  youtube: {
    title: 'YouTube',
    renderIcon: (color: string) => <Ionicons name="logo-youtube" color={color} size={22} />,
  },
  home: {
    title: 'YT Music',
    renderIcon: (color: string) => <Ionicons name="musical-notes" color={color} size={22} />,
  },
  friends: {
    title: 'Friends',
    renderIcon: (color: string) => <Ionicons name="people" color={color} size={22} />,
  },
};

export default function AppTabBar(props: any) {
  const { state, navigation, insets } = props;
  const bottomInset = insets?.bottom ?? 0;

  return (
    <View
      style={[
        styles.container,
        { paddingBottom: Math.max(bottomInset, 8) },
      ]}
    >
      <View style={styles.dock}>
        {state.routes.map((route: any, index: number) => {
          const isFocused = state.index === index;
          const conf = TAB_CONFIG[route.name] || {
            title: route.name,
            renderIcon: (color: string) => <Ionicons name="apps" color={color} size={22} />,
          };
          const label = conf.title;
          const iconColor = isFocused ? '#FFFFFF' : '#94A3B8';
          const icon = conf.renderIcon(iconColor);

          const onPress = () => {
            const event = navigation.emit({
              type: 'tabPress',
              target: route.key,
              canPreventDefault: true,
            });
            if (!isFocused && !event.defaultPrevented) navigation.navigate(route.name);
            if (route.name === 'youtube') {
              DeviceEventEmitter.emit(YOUTUBE_TAB_GO_HOME);
            } else if (route.name === 'home') {
              DeviceEventEmitter.emit('ytMusicTabPressed');
            }
          };

          return (
            <Pressable
              key={route.key}
              onPress={onPress}
              style={styles.tab}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <View style={[styles.tabInner, isFocused ? styles.tabInnerActive : styles.tabInnerInactive]}>
                <View style={styles.iconWrap}>{icon}</View>
                <Text
                  numberOfLines={1}
                  style={[
                    styles.label,
                    { color: isFocused ? '#FFFFFF' : '#94A3B8' },
                    isFocused && styles.labelActive,
                  ]}
                >
                  {label}
                </Text>
              </View>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#090615',
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.08)',
    paddingTop: 6,
    paddingHorizontal: 12,
  },
  dock: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: 'rgba(22, 16, 44, 0.85)',
    borderRadius: 24,
    paddingVertical: 5,
    paddingHorizontal: 6,
    borderWidth: 1,
    borderColor: 'rgba(167, 139, 250, 0.18)',
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tabInner: {
    width: '100%',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 6,
    borderRadius: 18,
    borderWidth: 1,
  },
  tabInnerActive: {
    backgroundColor: '#8B5CF6',
    borderColor: 'rgba(255, 255, 255, 0.30)',
  },
  tabInnerInactive: {
    backgroundColor: 'transparent',
    borderColor: 'transparent',
  },
  iconWrap: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 2,
  },
  label: {
    fontSize: 11,
    fontWeight: '600',
    letterSpacing: 0.1,
  },
  labelActive: {
    fontWeight: '700',
  },
});

