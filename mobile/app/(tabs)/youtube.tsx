import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { YouTubeWebTab } from '../../components/YouTubeWebTab';

const YOUTUBE_URL = 'https://m.youtube.com/?theme=dark';

export default function YouTubeScreen() {
  const insets = useSafeAreaInsets();
  return (
    <View style={{ flex: 1, backgroundColor: '#0F0F0F', paddingTop: insets.top, position: 'relative' }}>
      <YouTubeWebTab url={YOUTUBE_URL} />
    </View>
  );
}
