import { Pressable, StyleSheet, Text, View } from 'react-native';
import GradientView from './GradientView';
import { Colors, Glass, Gradients, BorderRadius, Shadows } from '../lib/theme';

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
    backgroundColor: Glass.bgStrong,
    borderTopWidth: StyleSheet.hairlineWidth,
    paddingTop: 2,
    paddingHorizontal: 4,
  },
  row: { flex: 1, flexDirection: 'row' },
  tab: { flex: 1, alignItems: 'center' },
  tabInner: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: 1,
    paddingVertical: 3,
    paddingHorizontal: 6,
    borderRadius: BorderRadius.sm,
    overflow: 'hidden',
  },
  tabInnerActive: {
    ...Shadows.glow,
  },
  iconWrap: { width: 22, height: 22, alignItems: 'center', justifyContent: 'center' },
  label: { fontSize: 10, fontWeight: '600' },
});
