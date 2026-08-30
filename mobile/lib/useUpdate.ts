import { useEffect, useRef, useState } from 'react';
import { Linking } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { checkForUpdate, installedVersion, isNewerVersion, type UpdateInfo } from './update';

const DISMISSED_KEY = 'update.dismissed';

interface UseUpdateResult {
  update: UpdateInfo | null;
  handled: boolean;
  dismiss: () => void;
  install: () => void;
}

export function useUpdate(): UseUpdateResult {
  const [update, setUpdate] = useState<UpdateInfo | null>(null);
  const [handled, setHandled] = useState(false);
  const checkedRef = useRef(false);

  useEffect(() => {
    if (checkedRef.current) return;
    checkedRef.current = true;

    (async () => {
      try {
        const [latest, dismissed] = await Promise.all([
          checkForUpdate(),
          AsyncStorage.getItem(DISMISSED_KEY),
        ]);
        if (!latest) return;
        if (dismissed === latest.latestVersion) return;
        if (!isNewerVersion(latest.latestVersion, installedVersion())) {
          await AsyncStorage.setItem(DISMISSED_KEY, latest.latestVersion).catch(() => {});
          return;
        }
        setUpdate(latest);
      } catch {}
    })();
  }, []);

  const dismiss = () => {
    if (update) {
      AsyncStorage.setItem(DISMISSED_KEY, update.latestVersion).catch(() => {});
    }
    setHandled(true);
  };

  const install = () => {
    setHandled(true);
    if (update?.downloadUrl) {
      Linking.openURL(update.downloadUrl).catch(() => {});
    }
  };

  return { update, handled, dismiss, install };
}