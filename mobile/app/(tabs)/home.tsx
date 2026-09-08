import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { YouTubeWebTab } from '../../components/YouTubeWebTab';
import NowPlayingCard from '../../components/NowPlayingCard';
import { Colors } from '../../lib/theme';

const YTMUSIC_URL = 'https://music.youtube.com';

export default function SongsScreen() {
  const insets = useSafeAreaInsets();

  return (
    <View style={{ flex: 1, backgroundColor: '#030303', paddingTop: insets.top, position: 'relative' }}>
      <YouTubeWebTab url={YTMUSIC_URL} />
      <NowPlayingCard accent={Colors.primary} />
    </View>
  );
}
