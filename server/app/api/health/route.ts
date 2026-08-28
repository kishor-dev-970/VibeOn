import { execFile } from 'child_process';
import { promisify } from 'util';
import { readFile } from 'fs/promises';
import { ok } from '@/lib/http';

const execFileAsync = promisify(execFile);

const POT_PING = 'http://127.0.0.1:4416/ping';

function lastLines(s: string, n: number): string {
  const lines = s.trim().split(/\r?\n/);
  return lines.slice(-n).join('\n');
}

export async function GET(): Promise<Response> {
  let pot: Record<string, unknown> = { running: false };

  try {
    const r = await execFileAsync('curl', ['-fsS', '--max-time', '5', POT_PING], {
      timeout: 7000,
    });
    pot.running = /ok|pong|200|OK/i.test(r.stdout + r.stderr);
  } catch {
    pot.running = false;
  }

  try {
    const log = await readFile('/tmp/bgutil-pot.log', 'utf8');
    pot.log = lastLines(log, 4);
  } catch {
    pot.log = '(no log file)';
  }

  try {
    const prov = await execFileAsync('python3', ['-m', 'yt_dlp', '--list-pot-providers'], {
      timeout: 15000,
      maxBuffer: 512 * 1024,
    });
    pot.providers = (prov.stdout + prov.stderr).trim();
  } catch (e: any) {
    pot.providers = `(list failed: ${(e?.stderr ?? e?.message ?? '').toString().split('\n')[0]})`;
  }

  return ok({ status: 'ok', commit: process.env.RENDER_GIT_COMMIT ?? process.env.COMMIT_SHA ?? 'local', uptime: process.uptime(), pot });
}