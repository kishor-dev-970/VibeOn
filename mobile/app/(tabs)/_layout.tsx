import { Tabs } from 'expo-router';
import { Colors } from '../../lib/theme';
import { Ionicons } from '@expo/vector-icons';
import AppTabBar from '../../components/AppTabBar';

export default function TabsLayout() {
  return (
    <Tabs
      initialRouteName="youtube"
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: Colors.primary,
        tabBarInactiveTintColor: Colors.textMuted,
      }}
      tabBar={(props) => <AppTabBar {...props} />}
    >
      <Tabs.Screen
        name="youtube"
        options={{
          title: 'YouTube',
          tabBarIcon: ({ color }) => <Ionicons name="logo-youtube" color={color} size={22} />,
        }}
      />
      <Tabs.Screen
        name="home"
        options={{
          title: 'Songs',
          tabBarIcon: ({ color }) => <Ionicons name="musical-notes" color={color} size={22} />,
        }}
      />
      <Tabs.Screen
        name="live"
        options={{
          title: 'Live',
          tabBarIcon: ({ color }) => <Ionicons name="radio" color={color} size={22} />,
        }}
      />
      <Tabs.Screen
        name="friends"
        options={{
          title: 'Friends',
          tabBarIcon: ({ color }) => <Ionicons name="people" color={color} size={22} />,
        }}
      />
    </Tabs>
  );
}
