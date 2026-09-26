/**
 * App configuration — the port of android/.../Prefs.kt.
 *
 * On iOS the values live in the App Group's UserDefaults (via the local
 * `sweep-shared` native module) so the WidgetKit extension can read the same
 * token and database choices. Keys are identical to the Android app's
 * SharedPreferences keys, and the Swift widget code reads exactly these names.
 */
import SweepShared from '../modules/sweep-shared';

export const KEYS = {
  token: 'token',
  inboxDbId: 'inboxDbId',
  inboxTitleProp: 'inboxTitleProp',
  tasksDbId: 'tasksDbId',
  doneProp: 'doneProp',
  dueProp: 'dueProp',
} as const;

export interface Config {
  token: string;
  inboxDbId: string;
  inboxTitleProp: string;
  tasksDbId: string;
  doneProp: string;
  dueProp: string;
}

export function loadConfig(): Config {
  return {
    token: SweepShared.get(KEYS.token) ?? '',
    inboxDbId: SweepShared.get(KEYS.inboxDbId) ?? '',
    inboxTitleProp: SweepShared.get(KEYS.inboxTitleProp) ?? 'Name',
    tasksDbId: SweepShared.get(KEYS.tasksDbId) ?? '',
    doneProp: SweepShared.get(KEYS.doneProp) ?? 'Done',
    dueProp: SweepShared.get(KEYS.dueProp) ?? 'Due',
  };
}

export function saveConfig(c: Config): void {
  SweepShared.set(KEYS.token, c.token);
  SweepShared.set(KEYS.inboxDbId, c.inboxDbId);
  SweepShared.set(KEYS.inboxTitleProp, c.inboxTitleProp);
  SweepShared.set(KEYS.tasksDbId, c.tasksDbId);
  SweepShared.set(KEYS.doneProp, c.doneProp);
  SweepShared.set(KEYS.dueProp, c.dueProp);
  // Nudge any placed widgets to re-render with the new config.
  SweepShared.reloadWidgets();
}

export function captureReady(c: Config = loadConfig()): boolean {
  return c.token.trim().length > 0 && c.inboxDbId.trim().length > 0;
}

export function tasksReady(c: Config = loadConfig()): boolean {
  return c.token.trim().length > 0 && c.tasksDbId.trim().length > 0;
}

export function reloadWidgets(): void {
  SweepShared.reloadWidgets();
}
