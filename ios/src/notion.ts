/**
 * Minimal Notion REST client — a straight port of android/.../NotionApi.kt.
 * Each user supplies their own internal-integration token; the app only ever
 * touches the databases that token was granted.
 */

const BASE = 'https://api.notion.com/v1/';
const VERSION = '2022-06-28';

export class NotionError extends Error {}

type Json = Record<string, any>;

async function call(method: 'GET' | 'POST' | 'PATCH', path: string, token: string, body?: Json): Promise<Json> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 25_000);
  let resp: Response;
  try {
    resp = await fetch(BASE + path, {
      method,
      headers: {
        Authorization: `Bearer ${token}`,
        'Notion-Version': VERSION,
        'Content-Type': 'application/json; charset=utf-8',
      },
      body: method === 'GET' ? undefined : JSON.stringify(body ?? {}),
      signal: controller.signal,
    });
  } catch (e: any) {
    throw new NotionError(e?.name === 'AbortError' ? 'Timed out' : e?.message ?? 'Network error');
  } finally {
    clearTimeout(timer);
  }
  const text = await resp.text();
  if (!resp.ok) {
    let msg = `HTTP ${resp.status}`;
    try {
      const parsed = JSON.parse(text);
      if (parsed?.message) msg = parsed.message;
    } catch {}
    throw new NotionError(msg);
  }
  return text ? JSON.parse(text) : {};
}

export interface Database {
  id: string;
  title: string;
  properties: Json;
}

export function plainText(arr: any): string {
  if (!Array.isArray(arr)) return '';
  return arr.map((r) => r?.plain_text ?? '').join('');
}

export async function listDatabases(token: string): Promise<Database[]> {
  const res = await call('POST', 'search', token, {
    filter: { value: 'database', property: 'object' },
    page_size: 100,
  });
  const out: Database[] = [];
  for (const obj of res.results ?? []) {
    if (obj?.object !== 'database') continue;
    const title = plainText(obj.title) || '(untitled)';
    out.push({ id: obj.id, title, properties: obj.properties ?? {} });
  }
  return out;
}

/** Name of the title property in a database schema (there is always exactly one). */
export function titleProperty(properties: Json): string {
  for (const key of Object.keys(properties)) {
    if (properties[key]?.type === 'title') return key;
  }
  return 'Name';
}

export function propertiesOfType(properties: Json, type: string): string[] {
  return Object.keys(properties)
    .filter((k) => properties[k]?.type === type)
    .sort();
}

export async function createItem(token: string, databaseId: string, titleProp: string, text: string): Promise<void> {
  await call('POST', 'pages', token, {
    parent: { database_id: databaseId },
    properties: {
      [titleProp]: { title: [{ text: { content: text } }] },
    },
  });
}

export interface Task {
  id: string;
  name: string;
  due: string | null;
  url: string | null;
}

/**
 * Open tasks (done checkbox unchecked), sorted by due date ascending. When
 * `dueOnOrBefore` (ISO date) is given, only tasks due on/before that day are
 * returned — which also excludes tasks with no due date at all.
 */
export async function openTasks(
  token: string,
  databaseId: string,
  doneProp: string,
  dueProp: string,
  dueOnOrBefore: string | null,
): Promise<Task[]> {
  const and: Json[] = [{ property: doneProp, checkbox: { equals: false } }];
  if (dueOnOrBefore) and.push({ property: dueProp, date: { on_or_before: dueOnOrBefore } });
  const res = await call('POST', `databases/${databaseId}/query`, token, {
    filter: { and },
    sorts: [{ property: dueProp, direction: 'ascending' }],
    page_size: 100,
  });
  const out: Task[] = [];
  for (const page of res.results ?? []) {
    const props = page?.properties;
    if (!props) continue;
    let name = '';
    for (const key of Object.keys(props)) {
      if (props[key]?.type === 'title') {
        name = plainText(props[key].title);
        break;
      }
    }
    if (!name.trim()) name = '(untitled)';
    const due: string | null = props[dueProp]?.date?.start || null;
    out.push({ id: page.id, name, due, url: page.url || null });
  }
  return out;
}

export async function completeTask(token: string, pageId: string, doneProp: string): Promise<void> {
  await call('PATCH', `pages/${pageId}`, token, {
    properties: { [doneProp]: { checkbox: true } },
  });
}

/** Local calendar date as YYYY-MM-DD (what Notion's date filter expects). */
export function todayIso(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
