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
    pot.running = true;
    pot.pingBody = (r.stdout + r.stderr).slice(0, 120);
  } catch (e: any) {
    pot.running = false;
    pot.pingErr = (e?.stderr ?? e?.message ?? '').toString().slice(0, 200);
  }

  try {
    const log = await readFile('/tmp/bgutil-pot.log', 'utf8');
    pot.log = lastLines(log, 4);
  } catch {
    pot.log = '(no log file)';
  }

  try {
    const prow = await execFileAsync('find', ['/root/.config/yt-dlp/plugins', '-type', 'f'], {
      timeout: 5000,
    });
    pot.pluginFiles = (prow.stdout as string).trim().split(/\r?\n/).slice(0, 12);
  } catch {
    pot.pluginFiles = '(none/find failed)';
  }

  try {
    const prov = await execFileAsync('python3', ['-m', 'yt_dlp', '--list-pot-providers'], {
      timeout: 20000,
      maxBuffer: 1024 * 1024,
    });
    pot.providers = `exit=OK\n${(prov.stdout + prov.stderr).trim()}`;
  } catch (e: any) {
    pot.providers = `exit=${(e as { code?: unknown }).code ?? '?'}\n${String(e?.stderr ?? e?.stdout ?? e?.message ?? '').trim().slice(-1500)}`;
  }

  return ok({ status: 'ok', commit: process.env.RENDER_GIT_COMMIT ?? process.env.COMMIT_SHA ?? 'local', uptime: process.uptime(), pot });
}