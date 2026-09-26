/**
 * Tiny bridge to the App Group's shared UserDefaults, so the app and the
 * WidgetKit extension see the same Notion token and database choices, plus a
 * hook to ask WidgetKit to re-render after settings or tasks change.
 *
 * Outside a native iOS build (Expo Go, web) the module is absent; an
 * in-memory fallback keeps the JS app runnable for UI work, but nothing
 * persists and there are no widgets.
 */
import { requireOptionalNativeModule } from 'expo-modules-core';

interface SweepSharedNative {
  get(key: string): string | null;
  set(key: string, value: string | null): void;
  reloadWidgets(): void;
}

const native = requireOptionalNativeModule<SweepSharedNative>('SweepShared');

const memory = new Map<string, string>();
const fallback: SweepSharedNative = {
  get: (key) => memory.get(key) ?? null,
  set: (key, value) => {
    if (value == null) memory.delete(key);
    else memory.set(key, value);
  },
  reloadWidgets: () => {},
};

const SweepShared: SweepSharedNative = native ?? fallback;
export const isNative = native != null;
export default SweepShared;
