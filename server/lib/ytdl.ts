import { ApiError } from './env';
import { execFile } from 'child_process';
import { promisify } from 'util';
import type { Song } from './types';

const execFileAsync = promisify(execFile);

const PYTHON = process.env.PYTHON_PATH ?? 'C:\\Users\\kishor\\AppData\\Local\\Programs\\Python\\Python312\\python.exe';

// YouTube bot-checks the default `web` player client from datacenter IPs
// (Render/VPS). Non-web clients are far less likely to get the
// "Sign in to confirm you're not a bot" block. Tried in order until one works.
export const YTDL_COMMON_ARGS = [
  '--extractor-args',
  'youtube:player_client=android,ios,tv,mweb,web_safari,web_embedded,android_vr,web',
];

interface YtdlEntry {
  id?: string;
  title?: string;
  channel?: string;
  uploader?: string;
  live_status?: string | null;
  thumbnails?: { url?: string }[];
}

interface YtdlPlaylist {
  entries?: YtdlEntry[];
}

function decodeHtml(text: string): string {
  const map: Record<string, string> = {
    '&amp;': '&', '&lt;': '<', '&gt;': '>', '&quot;': '"', '&#39;': "'", '&#039;': "'", '&nbsp;': ' ',
  };
  return text.replace(/&(?:amp|lt|gt|quot|#0?39|#039|nbsp);/g, (m) => map[m] ?? m);
}

function thumbnailUrl(entry: YtdlEntry): string {
  const list = entry.thumbnails ?? [];
  for (const scale of ['maxresdefault', 'hq720', 'high', 'medium', 'default', 'mqdefault']) {
    const match = list.find((t) => (t.url ?? '').includes(scale));
    if (match?.url) return match.url;
  }
  return list[0]?.url ?? '';
}

async function runSearch(query: string, count = 10): Promise<YtdlEntry[]> {
  try {
    const { stdout } = await execFileAsync(
      PYTHON,
      [
        '-m', 'yt_dlp',
        '--no-warnings',
        ...YTDL_COMMON_ARGS,
        '--flat-playlist',
        '-J',
        `ytsearch${count}:${query}`,
      ],
      { timeout: 30000, maxBuffer: 8 * 1024 * 1024 }
    );
    const parsed = JSON.parse(stdout) as YtdlPlaylist;
    return parsed.entries ?? [];
  } catch (e: any) {
    const msg = e?.stderr?.trim?.() || e?.message || 'YouTube search failed';
    throw new ApiError(502, msg, 'youtube_error');
  }
}

export async function ytdlSearch(query: string, count = 10): Promise<Song[]> {
  const entries = await runSearch(query, count);
  const seen = new Set<string>();
  const songs: Song[] = [];
  for (const entry of entries) {
    if (!entry.id || seen.has(entry.id)) continue;
    seen.add(entry.id);
    songs.push({
      videoId: entry.id,
      title: decodeHtml(entry.title ?? 'Unknown title'),
      channel: decodeHtml(entry.channel ?? entry.uploader ?? 'Unknown channel'),
      thumbnailUrl: thumbnailUrl(entry),
    });
  }
  return songs;
}

export async function ytdlLiveSearch(query: string, count = 8): Promise<Song[]> {
  const entries = await runSearch(query, count);
  const seen = new Set<string>();
  const songs: Song[] = [];
  for (const entry of entries) {
    if (!entry.id || seen.has(entry.id)) continue;
    if (entry.live_status !== 'is_live') continue;
    seen.add(entry.id);
    songs.push({
      videoId: entry.id,
      title: decodeHtml(entry.title ?? 'Unknown title'),
      channel: decodeHtml(entry.channel ?? entry.uploader ?? 'Unknown channel'),
      thumbnailUrl: thumbnailUrl(entry),
    });
  }
  return songs;
}
