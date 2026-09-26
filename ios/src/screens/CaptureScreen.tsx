import React, { useEffect, useRef, useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { colors } from '../theme';
import { PrimaryButton } from '../components';
import { createItem } from '../notion';
import { captureReady, loadConfig } from '../store';

/**
 * Port of CaptureActivity: a small sheet that types straight into the Notion
 * Inbox and closes. Voice on iOS uses the keyboard's own dictation key — the
 * `voice` flag just makes sure the keyboard is up and hints at the mic.
 */
export default function CaptureScreen({
  voice,
  initialText,
  onDone,
  onNeedsSetup,
}: {
  voice: boolean;
  initialText?: string;
  onDone: () => void;
  onNeedsSetup: () => void;
}) {
  const [text, setText] = useState(initialText ?? '');
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState<{ text: string; error?: boolean } | null>(null);
  const input = useRef<TextInput>(null);

  useEffect(() => {
    if (!captureReady()) {
      onNeedsSetup();
      return;
    }
    // autoFocus alone is unreliable right after a deep-link launch.
    const t = setTimeout(() => input.current?.focus(), 250);
    return () => clearTimeout(t);
  }, []);

  async function save() {
    const trimmed = text.trim();
    if (!trimmed || busy) return;
    setBusy(true);
    setStatus({ text: 'Saving…' });
    const c = loadConfig();
    try {
      await createItem(c.token, c.inboxDbId, c.inboxTitleProp, trimmed);
      setStatus({ text: '✓ Saved to Inbox' });
      setTimeout(onDone, 500);
    } catch (e: any) {
      setBusy(false);
      setStatus({ text: `Couldn't save: ${e?.message ?? ''}`, error: true });
    }
  }

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={styles.header}>
        <Pressable onPress={onDone} hitSlop={12}>
          <Text style={styles.cancel}>Cancel</Text>
        </Pressable>
        <Text style={styles.title}>Add to Inbox</Text>
        <View style={styles.headerSpacer} />
      </View>
      <View style={styles.body}>
        <TextInput
          ref={input}
          value={text}
          onChangeText={setText}
          placeholder="Add to Inbox…"
          placeholderTextColor={colors.subtext}
          multiline
          autoFocus
          editable={!busy}
          returnKeyType="done"
          blurOnSubmit
          onSubmitEditing={save}
          style={styles.input}
        />
        <Text style={styles.hint}>
          {voice ? '🎤 Tap the mic on your keyboard to dictate, then Save.' : 'Tip: the mic key on your keyboard dictates.'}
        </Text>
        {status ? <Text style={[styles.status, status.error && styles.statusError]}>{status.text}</Text> : null}
        <PrimaryButton title="Save" onPress={save} disabled={busy || !text.trim()} />
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 12,
  },
  cancel: { color: colors.accent, fontSize: 16, width: 64 },
  title: { flex: 1, textAlign: 'center', color: colors.text, fontSize: 17, fontWeight: '600' },
  headerSpacer: { width: 64 },
  body: { padding: 20, gap: 12 },
  input: {
    backgroundColor: colors.cardBg,
    borderColor: colors.fieldStroke,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    minHeight: 120,
    fontSize: 18,
    color: colors.text,
    textAlignVertical: 'top',
  },
  hint: { color: colors.subtext, fontSize: 13 },
  status: { color: colors.subtext, fontSize: 14 },
  statusError: { color: colors.overdue },
});
