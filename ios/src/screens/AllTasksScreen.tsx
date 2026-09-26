import React, { useCallback, useEffect, useState } from 'react';
import { FlatList, Linking, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';

import { colors } from '../theme';
import { completeTask, openTasks, Task } from '../notion';
import { loadConfig, reloadWidgets, tasksReady } from '../store';

/** Port of AllTasksActivity: every open task, no due-date filter. */
export default function AllTasksScreen({ onNeedsSetup, onAdd }: { onNeedsSetup: () => void; onAdd: () => void }) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [status, setStatus] = useState<string | null>('Loading…');
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    const c = loadConfig();
    try {
      const result = await openTasks(c.token, c.tasksDbId, c.doneProp, c.dueProp, null);
      setTasks(result);
      setStatus(result.length === 0 ? 'No open tasks' : null);
    } catch (e: any) {
      setStatus(`Couldn't load: ${e?.message ?? ''}`);
    } finally {
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    if (!tasksReady()) {
      onNeedsSetup();
      return;
    }
    load();
  }, [load]);

  async function complete(task: Task) {
    const c = loadConfig();
    setTasks((prev) => prev.filter((t) => t.id !== task.id));
    try {
      await completeTask(c.token, task.id, c.doneProp);
      reloadWidgets();
    } catch (e: any) {
      setStatus(`Couldn't complete task: ${e?.message ?? ''}`);
      setTasks((prev) => [task, ...prev]);
    }
  }

  return (
    <View style={styles.flex}>
      <View style={styles.header}>
        <Pressable onPress={onNeedsSetup} hitSlop={12}>
          <Text style={styles.link}>‹ Settings</Text>
        </Pressable>
        <Text style={styles.title}>Open tasks</Text>
        <Pressable onPress={onAdd} hitSlop={12}>
          <Text style={styles.link}>＋ Add</Text>
        </Pressable>
      </View>
      {status ? <Text style={styles.status}>{status}</Text> : null}
      <FlatList
        data={tasks}
        keyExtractor={(t) => t.id}
        contentContainerStyle={styles.list}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => {
              setRefreshing(true);
              load();
            }}
          />
        }
        renderItem={({ item }) => (
          <View style={styles.row}>
            <Pressable onPress={() => complete(item)} hitSlop={8} accessibilityLabel="Mark done">
              <View style={styles.circle} />
            </Pressable>
            <Pressable style={styles.rowBody} onPress={() => item.url && Linking.openURL(item.url).catch(() => {})}>
              <Text style={styles.name}>{item.name}</Text>
              <Text style={[styles.due, isOverdue(item.due) && styles.overdue]}>{formatDue(item.due)}</Text>
            </Pressable>
          </View>
        )}
      />
    </View>
  );
}

function localDate(due: string): Date {
  const [y, m, d] = due.slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}

function startOfToday(): Date {
  const n = new Date();
  return new Date(n.getFullYear(), n.getMonth(), n.getDate());
}

function isOverdue(due: string | null): boolean {
  if (!due) return false;
  const d = localDate(due);
  return !isNaN(d.getTime()) && d < startOfToday();
}

function formatDue(due: string | null): string {
  if (!due) return 'No due date';
  const d = localDate(due);
  if (isNaN(d.getTime())) return due;
  if (d.getTime() === startOfToday().getTime()) return 'Today';
  return d.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 12,
  },
  link: { color: colors.accent, fontSize: 16 },
  title: { color: colors.text, fontSize: 17, fontWeight: '600' },
  status: { color: colors.subtext, paddingHorizontal: 20, paddingBottom: 8 },
  list: { paddingHorizontal: 12, paddingBottom: 32 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.cardBg,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    marginBottom: 8,
    gap: 12,
  },
  circle: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: colors.accent,
  },
  rowBody: { flex: 1 },
  name: { color: colors.text, fontSize: 16 },
  due: { color: colors.subtext, fontSize: 13, marginTop: 2 },
  overdue: { color: colors.overdue },
});
