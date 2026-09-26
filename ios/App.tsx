import React, { useEffect, useState } from 'react';
import { SafeAreaView, StyleSheet } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import * as Linking from 'expo-linking';

import { colors } from './src/theme';
import { captureReady, tasksReady } from './src/store';
import SettingsScreen from './src/screens/SettingsScreen';
import CaptureScreen from './src/screens/CaptureScreen';
import AllTasksScreen from './src/screens/AllTasksScreen';

type Screen = { name: 'settings' } | { name: 'capture'; voice: boolean; text?: string } | { name: 'tasks' };

/**
 * Three screens, no navigation library. The home-screen widgets deep-link in:
 *   sweep://capture            — quick capture (text)
 *   sweep://capture?voice=1    — quick capture, keyboard up for dictation
 *   sweep://tasks              — all open tasks
 * Anything else (or a cold launch from the icon) lands on Settings, which is
 * also where Capture/Tasks bounce to until Notion is connected — mirroring
 * CaptureActivity / AllTasksActivity on Android.
 */
function screenForUrl(url: string | null): Screen {
  if (!url) return { name: 'settings' };
  const { hostname, path, queryParams } = Linking.parse(url);
  const route = (hostname || path || '').replace(/^\/+|\/+$/g, '');
  if (route === 'capture') {
    if (!captureReady()) return { name: 'settings' };
    const voice = String(queryParams?.voice ?? '') === '1';
    const text = typeof queryParams?.text === 'string' ? queryParams.text : undefined;
    return { name: 'capture', voice, text };
  }
  if (route === 'tasks') {
    return tasksReady() ? { name: 'tasks' } : { name: 'settings' };
  }
  return { name: 'settings' };
}

export default function App() {
  const url = Linking.useURL();
  const [screen, setScreen] = useState<Screen>(() => screenForUrl(url));

  // Each new deep link (widget tap while the app is already open) re-routes.
  useEffect(() => {
    setScreen(screenForUrl(url));
  }, [url]);

  const goSettings = () => setScreen({ name: 'settings' });

  let body: React.ReactNode;
  switch (screen.name) {
    case 'capture':
      body = (
        <CaptureScreen
          voice={screen.voice}
          initialText={screen.text}
          onDone={goSettings}
          onNeedsSetup={goSettings}
        />
      );
      break;
    case 'tasks':
      body = <AllTasksScreen onNeedsSetup={goSettings} onAdd={() => setScreen({ name: 'capture', voice: false })} />;
      break;
    default:
      body = (
        <SettingsScreen
          onOpenCapture={() => setScreen({ name: 'capture', voice: false })}
          onOpenTasks={() => setScreen({ name: 'tasks' })}
        />
      );
  }

  return (
    <SafeAreaView style={styles.root}>
      <StatusBar style="auto" />
      {body}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.screenBg },
});
