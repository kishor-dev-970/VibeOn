import Constants from 'expo-constants';

export interface UpdateInfo {
  latestVersion: string;
  downloadUrl: string;
  notes: string;
  publishedAt: string;
}

const REPO = 'kishor-dev-970/VibeOn';
const RELEASES_API = `https://api.github.com/repos/${REPO}/releases/latest`;
const RELEASES_PAGE = `https://github.com/${REPO}/releases/latest`;

function parseVersion(v: string): number[] {
  return v
    .replace(/^v/i, '')
    .split(/[._-]/)
    .map((n) => parseInt(n, 10) || 0);
}

export function isNewerVersion(candidate: string, current: string): boolean {
  const a = parseVersion(candidate);
  const b = parseVersion(current);
  const len = Math.max(a.length, b.length);
  for (let i = 0; i < len; i++) {
    const x = a[i] ?? 0;
    const y = b[i] ?? 0;
    if (x > y) return true;
    if (x < y) return false;
  }
  return false;
}

export function installedVersion(): string {
  const native = Constants.nativeAppVersion;
  const fallback = Constants.expoConfig?.version;
  return (native || fallback || '0.0.0').replace(/^v/i, '');
}

export async function checkForUpdate(): Promise<UpdateInfo | null> {
  try {
    const res = await fetch(RELEASES_API, {
      headers: { Accept: 'application/vnd.github+json', 'User-Agent': 'VibeOn' },
    });
    if (!res.ok) return null;
    const json = await res.json();
    const tag: string = json.tag_name ?? '';
    const notes: string = json.body ?? '';
    const publishedAt: string = json.published_at ?? '';
    const apk = (json.assets ?? []).find(
      (a: any) => typeof a.name === 'string' && /\.apk$/i.test(a.name) && a.browser_download_url
    );
    const url: string = apk?.browser_download_url ?? RELEASES_PAGE;
    const latest = tag.replace(/^v/i, '');
    if (!latest) return null;
    return { latestVersion: latest, downloadUrl: url, notes, publishedAt };
  } catch {
    return null;
  }
}