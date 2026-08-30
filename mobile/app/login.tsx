import { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../context/AuthContext';
import * as api from '../lib/api';
import { Colors, BorderRadius, Spacing } from '../lib/theme';

export default function LoginScreen() {
  const { signIn } = useAuth();
  const router = useRouter();
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [busy, setBusy] = useState(false);

  const handleLogin = async () => {
    if (!firstName.trim() || !lastName.trim()) {
      Alert.alert('Enter your name', 'Please enter your first and last name.');
      return;
    }
    setBusy(true);
    try {
      const { token, user } = await api.signInWithName(firstName.trim(), lastName.trim());
      signIn(token, user);
      router.replace('/(tabs)/youtube');
    } catch (e) {
      Alert.alert('Login failed', e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <Text style={styles.musicNote}>♫</Text>
      <Text style={styles.title}>VibeOn</Text>
      <Text style={styles.subtitle}>
        Enter your name to join.{'\n'}Everyone in the app can see what you&apos;re playing.
      </Text>

      <TextInput
        style={styles.input}
        placeholder="First name"
        placeholderTextColor={Colors.textSubtle}
        value={firstName}
        onChangeText={setFirstName}
        autoCapitalize="words"
        autoCorrect={false}
        returnKeyType="next"
      />
      <TextInput
        style={styles.input}
        placeholder="Last name"
        placeholderTextColor={Colors.textSubtle}
        value={lastName}
        onChangeText={setLastName}
        autoCapitalize="words"
        autoCorrect={false}
        returnKeyType="done"
        onSubmitEditing={handleLogin}
      />

      {busy ? (
        <ActivityIndicator size="large" color={Colors.primaryLight} style={styles.busy} />
      ) : (
        <Pressable style={styles.enterButton} onPress={handleLogin}>
          <Text style={styles.enterButtonText}>Enter</Text>
        </Pressable>
      )}
      <Text style={styles.footer}>v1.5.4</Text>
    </KeyboardAvoidingView>
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
    marginBottom: 36,
    lineHeight: 22,
  },
  input: {
    width: '100%',
    maxWidth: 360,
    backgroundColor: Colors.searchBg,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    borderColor: Colors.border,
    color: Colors.text,
    fontSize: 16,
    paddingHorizontal: Spacing.lg,
    paddingVertical: 14,
    marginBottom: Spacing.md,
  },
  enterButton: {
    backgroundColor: Colors.primary,
    borderRadius: BorderRadius.md,
    paddingVertical: 14,
    paddingHorizontal: 48,
    marginTop: Spacing.sm,
  },
  enterButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  busy: {
    marginTop: 28,
  },
  footer: {
    position: 'absolute',
    bottom: 40,
    color: Colors.textSubtle,
    fontSize: 12,
  },
});