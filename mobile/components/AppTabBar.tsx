import { DeviceEventEmitter, Pressable, StyleSheet, Text, View } from 'react-native';
import GradientView from './GradientView';
import { Colors, Glass, Gradients, BorderRadius, Shadows } from '../lib/theme';

export const YOUTUBE_TAB_GO_HOME = 'youtubeTabPressed';

export default function AppTabBar(props: any) {
  const { state, descriptors, navigation, insets } = props;
  const bottomInset = insets?.bottom ?? 0;

  return (
    <View
      style={[
        styles.container,
        { paddingBottom: bottomInset, borderTopColor: Glass.border },
      ]}
    >
      <GradientView colors={Gradients.brandMuted} style={StyleSheet.absoluteFill} />
      <View style={styles.row}>
        {state.routes.map((route: any, index: number) => {
          const { options } = descriptors[route.key];
          const label = options.title ?? route.name;
          const isFocused = state.index === index;
          const icon = options.tabBarIcon
            ? options.tabBarIcon({ color: isFocused ? '#fff' : Colors.textMuted, size: 20, focused: isFocused })
            : null;
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
            <Pressable key={route.key} onPress={onPress} style={styles.tab}>
              <View style={[styles.tabInner, isFocused && styles.tabInnerActive]}>
                {isFocused && (
                  <GradientView colors={Gradients.play} style={StyleSheet.absoluteFill} />
                )}
                <View style={styles.iconWrap}>{icon}</View>
                <Text style={[styles.label, { color: isFocused ? '#fff' : Colors.textMuted }]}>
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
    flexDirection: 'row',
    backgroundColor: '#0c0a18',
    borderTopWidth: 1,
    borderTopColor: 'rgba(255,255,255,0.08)',
    paddingTop: 6,
    paddingHorizontal: 8,
  },
  row: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-around' },
  tab: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  tabInner: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: 3,
    paddingVertical: 5,
    paddingHorizontal: 12,
    borderRadius: BorderRadius.full,
    overflow: 'hidden',
  },
  tabInnerActive: {
    ...Shadows.glow,
  },
  iconWrap: { width: 22, height: 22, alignItems: 'center', justifyContent: 'center' },
  label: { fontSize: 11, fontWeight: '700', letterSpacing: 0.2 },
});
