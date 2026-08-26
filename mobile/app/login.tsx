import { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../context/AuthContext';
import * as api from '../lib/api';
import { facebookLogin } from '../lib/facebook';
import { Colors, BorderRadius, Spacing } from '../lib/theme';

export default function LoginScreen() {
  const { signIn } = useAuth();
  const router = useRouter();
  const [busy, setBusy] = useState(false);

  const handleLogin = async () => {
    setBusy(true);
    try {
      const accessToken = await facebookLogin();
      const { token, user } = await api.loginWithFacebook(accessToken);
      signIn(token, user);
      router.replace('/(tabs)');
    } catch (e) {
      Alert.alert('Login failed', e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.musicNote}>♫</Text>
      <Text style={styles.title}>Social Music</Text>
      <Text style={styles.subtitle}>
        Listen together.{'\n'}Your friends can see what you&apos;re playing.
      </Text>
      {busy ? (
        <ActivityIndicator size="large" color={Colors.primaryLight} />
      ) : (
        <Pressable style={styles.fbButton} onPress={handleLogin}>
          <Text style={styles.fbIcon}>f</Text>
          <Text style={styles.fbButtonText}>Continue with Facebook</Text>
        </Pressable>
      )}
      <Text style={styles.footer}>v1.0.0</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },
  musicNote: {
    fontSize: 48,
    color: Colors.primary,
    marginBottom: 16,
  },
  title: {
    fontSize: 34,
    fontWeight: '800',
    marginBottom: 12,
    color: Colors.text,
  },
  subtitle: {
    fontSize: 15,
    color: Colors.textMuted,
    textAlign: 'center',
    marginBottom: 48,
    lineHeight: 22,
  },
  fbButton: {
    backgroundColor: '#1877F2',
    borderRadius: BorderRadius.md,
    paddingVertical: 14,
    paddingHorizontal: 32,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  fbIcon: {
    fontSize: 20,
    fontWeight: '800',
    color: '#fff',
  },
  fbButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  footer: {
    position: 'absolute',
    bottom: 40,
    color: Colors.textSubtle,
    fontSize: 12,
  },
});
