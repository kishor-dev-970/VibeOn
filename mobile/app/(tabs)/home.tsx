import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { YouTubeWebTab } from '../../components/YouTubeWebTab';

const YTMUSIC_URL = 'https://music.youtube.com';

export default function SongsScreen() {
  const insets = useSafeAreaInsets();

  return (
    <View style={{ flex: 1, backgroundColor: '#030303', paddingTop: insets.top, position: 'relative' }}>
      <YouTubeWebTab url={YTMUSIC_URL} />
    </View>
  );
}
