import { execFile } from 'child_process';
import { promisify } from 'util';
import { ok } from '@/lib/http';

const execFileAsync = promisify(execFile);

const POT_PING = 'http://127.0.0.1:4416/ping';

export async function GET(): Promise<Response> {
  let pot: Record<string, unknown> = { running: false };

  try {
    const r = await execFileAsync('curl', ['-fsS', '--max-time', '2', POT_PING], {
      timeout: 3000,
    });
    pot.running = true;
    pot.pingBody = (r.stdout + r.stderr).slice(0, 120);
  } catch (e: any) {
    pot.running = false;
    pot.pingErr = (e?.stderr ?? e?.message ?? '').toString().slice(0, 200);
  }

  return ok({ status: 'ok', commit: process.env.RENDER_GIT_COMMIT ?? process.env.COMMIT_SHA ?? 'local', uptime: process.uptime(), pot });
}
