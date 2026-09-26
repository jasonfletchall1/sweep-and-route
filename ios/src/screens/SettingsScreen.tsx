import React, { useEffect, useMemo, useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import { colors } from '../theme';
import { Choice, Label, PrimaryButton, SectionHeader } from '../components';
import { Database, listDatabases, propertiesOfType, titleProperty } from '../notion';
import { captureReady, loadConfig, saveConfig, tasksReady } from '../store';
import { isNative } from '../../modules/sweep-shared';

const TOKEN_HELP =
  'In Notion (web or desktop): Settings → Developer tools → Connections → New connection. ' +
  'Name it Sweep, allow read/insert/update content, then in its Content access tab add your ' +
  'Inbox and Tasks databases. Copy the secret here. Only content you add there is visible to this app.';

export default function SettingsScreen({
  onOpenCapture,
  onOpenTasks,
}: {
  onOpenCapture: () => void;
  onOpenTasks: () => void;
}) {
  const saved = useMemo(() => loadConfig(), []);
  const [token, setToken] = useState(saved.token);
  const [status, setStatus] = useState<{ text: string; error?: boolean } | null>(null);
  const [busy, setBusy] = useState(false);
  const [databases, setDatabases] = useState<Database[]>([]);
  const [inboxTitle, setInboxTitle] = useState<string | null>(null);
  const [tasksTitle, setTasksTitle] = useState<string | null>(null);
  const [doneProp, setDoneProp] = useState<string | null>(null);
  const [dueProp, setDueProp] = useState<string | null>(null);

  const names = databases.map((d) => d.title);
  const tasksDb = databases.find((d) => d.title === tasksTitle) ?? null;
  const checkboxes = tasksDb ? propertiesOfType(tasksDb.properties, 'checkbox') : [];
  const dates = tasksDb ? propertiesOfType(tasksDb.properties, 'date') : [];

  // Same preselection rules as SettingsActivity.preselect(): saved id first,
  // then a name hint ("inbox" / "task").
  function preselect(dbs: Database[], savedId: string, hint: string): string | null {
    const byId = dbs.find((d) => d.id === savedId);
    if (byId) return byId.title;
    const byHint = dbs.find((d) => d.title.toLowerCase().includes(hint));
    return byHint?.title ?? null;
  }

  useEffect(() => {
    if (!tasksDb) return;
    const cb = propertiesOfType(tasksDb.properties, 'checkbox');
    const dt = propertiesOfType(tasksDb.properties, 'date');
    setDoneProp(cb.find((p) => p === saved.doneProp) ?? cb.find((p) => p.toLowerCase() === 'done') ?? cb[0] ?? null);
    setDueProp(dt.find((p) => p === saved.dueProp) ?? dt.find((p) => p.toLowerCase() === 'due') ?? dt[0] ?? null);
  }, [tasksDb?.id]);

  async function load() {
    const t = token.trim();
    if (!t) {
      setStatus({ text: 'Paste your Notion integration token first', error: true });
      return;
    }
    setBusy(true);
    setStatus({ text: 'Connecting to Notion…' });
    try {
      const dbs = await listDatabases(t);
      if (dbs.length === 0) {
        setStatus({
          text:
            'No databases visible to this token. In Notion, open the Sweep connection → Content access → add your Inbox and Tasks databases.',
          error: true,
        });
        return;
      }
      setDatabases(dbs);
      setInboxTitle(preselect(dbs, saved.inboxDbId, 'inbox'));
      setTasksTitle(preselect(dbs, saved.tasksDbId, 'task'));
      setStatus({ text: `Found ${dbs.length} databases. Pick which is which:` });
    } catch (e: any) {
      setStatus({ text: `Couldn't connect: ${e?.message ?? ''}`, error: true });
    } finally {
      setBusy(false);
    }
  }

  function save() {
    const inboxDb = databases.find((d) => d.title === inboxTitle);
    if (!inboxDb || !tasksDb || !doneProp || !dueProp) {
      setStatus({ text: 'Pick both databases and the Done/Due properties', error: true });
      return;
    }
    saveConfig({
      token: token.trim(),
      inboxDbId: inboxDb.id,
      inboxTitleProp: titleProperty(inboxDb.properties),
      tasksDbId: tasksDb.id,
      doneProp,
      dueProp,
    });
    setStatus({ text: 'Saved — now add the Sweep widgets to your home screen (long-press it → + → Sweep)' });
  }

  // Auto-connect when a token is already stored, like the Android app does.
  useEffect(() => {
    if (saved.token.trim()) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const ready = captureReady() || tasksReady();

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Sweep</Text>
        <Text style={styles.tagline}>Quick capture & tasks for Notion</Text>

        {ready ? (
          <View style={styles.quickRow}>
            <Pressable style={styles.quick} onPress={onOpenCapture}>
              <Text style={styles.quickText}>＋ Add to Inbox</Text>
            </Pressable>
            <Pressable style={styles.quick} onPress={onOpenTasks}>
              <Text style={styles.quickText}>Open tasks ›</Text>
            </Pressable>
          </View>
        ) : null}

        {!isNative ? (
          <Text style={styles.warn}>
            Running without the native module (Expo Go / web): settings won't persist and there are no widgets.
          </Text>
        ) : null}

        <SectionHeader>CONNECT NOTION</SectionHeader>
        <Label>Notion integration token</Label>
        <TextInput
          value={token}
          onChangeText={setToken}
          placeholder="ntn_…"
          placeholderTextColor={colors.subtext}
          autoCapitalize="none"
          autoCorrect={false}
          secureTextEntry
          textContentType="password"
          style={styles.input}
        />
        <Text style={styles.help}>{TOKEN_HELP}</Text>
        <PrimaryButton title="Load databases" onPress={load} disabled={busy} />
        {status ? <Text style={[styles.status, status.error && styles.statusError]}>{status.text}</Text> : null}

        <SectionHeader>YOUR DATABASES</SectionHeader>
        <Choice
          label="Inbox database (quick captures land here)"
          options={names}
          value={inboxTitle}
          onChange={setInboxTitle}
          disabled={databases.length === 0}
        />
        <Choice
          label="Tasks database"
          options={names}
          value={tasksTitle}
          onChange={setTasksTitle}
          disabled={databases.length === 0}
        />
        <Choice
          label="Which checkbox marks a task done?"
          options={checkboxes}
          value={doneProp}
          onChange={setDoneProp}
          disabled={!tasksDb}
        />
        <Choice
          label="Which date property is the due date?"
          options={dates}
          value={dueProp}
          onChange={setDueProp}
          disabled={!tasksDb}
        />
        <PrimaryButton title="Save" onPress={save} disabled={databases.length === 0} />

        <Text style={styles.footer}>Sweep & Route · North Lafayette</Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  content: { padding: 20, paddingBottom: 48 },
  title: { color: colors.text, fontSize: 28, fontWeight: '700' },
  tagline: { color: colors.subtext, fontSize: 15, marginTop: 2 },
  quickRow: { flexDirection: 'row', gap: 10, marginTop: 18 },
  quick: {
    flex: 1,
    backgroundColor: colors.cardBg,
    borderColor: colors.fieldStroke,
    borderWidth: 1,
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
  },
  quickText: { color: colors.accent, fontSize: 15, fontWeight: '600' },
  warn: { color: colors.overdue, marginTop: 14, fontSize: 13 },
  input: {
    backgroundColor: colors.fieldBg,
    borderColor: colors.fieldStroke,
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 12,
    fontSize: 16,
    color: colors.text,
  },
  help: { color: colors.subtext, fontSize: 12, lineHeight: 17, marginTop: 8, marginBottom: 12 },
  status: { color: colors.subtext, marginTop: 12, fontSize: 14 },
  statusError: { color: colors.overdue },
  footer: { color: colors.subtext, fontSize: 12, textAlign: 'center', marginTop: 32 },
});
