import AsyncStorage from '@react-native-async-storage/async-storage';
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { AppUser } from '../lib/types';
import * as api from '../lib/api';

interface AuthContextValue {
  token: string | null;
  user: AppUser | null;
  loading: boolean;
  signIn: (token: string, user: AppUser) => void;
  signOut: () => Promise<void>;
}

const USER_KEY = 'auth.session.user';

const AuthContext = createContext<AuthContextValue>({
  token: null,
  user: null,
  loading: true,
  signIn: () => {},
  signOut: async () => {},
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<AppUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [storedToken, storedUser] = await Promise.all([
          api.getStoredToken(),
          AsyncStorage.getItem(USER_KEY),
        ]);
        if (storedToken && storedUser) {
          setToken(storedToken);
          setUser(JSON.parse(storedUser) as AppUser);
        }
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      loading,
      signIn: (newToken, newUser) => {
        setToken(newToken);
        setUser(newUser);
        AsyncStorage.setItem(USER_KEY, JSON.stringify(newUser)).catch(() => {});
      },
      signOut: async () => {
        await api.logout();
        await AsyncStorage.removeItem(USER_KEY);
        setToken(null);
        setUser(null);
      },
    }),
    [token, user, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  return useContext(AuthContext);
}
