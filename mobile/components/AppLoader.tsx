import { Image, StyleSheet, View } from 'react-native';

export default function AppLoader({ onReady }: { onReady?: () => void }) {
  return (
    <View style={styles.container}>
      <Image
        source={require('../assets/vibeon-wallpaper.png')}
        style={StyleSheet.absoluteFill}
        resizeMode="cover"
        onLoad={onReady}
        onError={onReady}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'transparent',
  },
});
