import { execFile } from 'child_process';
import { constants } from 'node:fs';
import { access, readFile, readdir } from 'node:fs/promises';
import { promisify } from 'util';

const execFileAsync = promisify(execFile);
const PYTHON = process.env.PYTHON_PATH ?? 'python3';

export async function GET() {
  const out: Record<string, unknown> = { timestamp: new Date().toISOString() };

  try {
    await access('/usr/local/bin/bgutil-pot', constants.X_OK);
    out.bgutilBinary = 'present+executable';
  } catch {
    out.bgutilBinary = 'MISSING';
  }

  try {
    out.sidecarLog = (await readFile('/tmp/bgutil-pot.log', { encoding: 'utf8' })).slice(-4000);
  } catch {
    out.sidecarLog = 'no /tmp/bgutil-pot.log';
  }

  try {
    out.pluginDirs = await readdir('/root/.config/yt-dlp/plugins', { recursive: true });
  } catch (e: any) {
    out.pluginDirs = `err: ${e?.message ?? e}`;
  }

  try {
    const { stdout, stderr } = await execFileAsync(PYTHON, ['-m', 'yt_dlp', '--list-plugins'], {
      timeout: 20000,
      maxBuffer: 1024 * 1024,
    });
    out.plugins = (stdout + stderr).trim();
  } catch (e: any) {
    out.plugins = `err: ${e?.stderr ?? e?.message ?? e}`;
  }

  const args = [
    '-m', 'yt_dlp',
    '--verbose',
    '--simulate',
    '--skip-download',
    '--extractor-args', 'youtube:player_client=web_embedded,android',
    '--get-title',
    'https://www.youtube.com/watch?v=rS38ukl_VsE',
  ];
  try {
    const { stdout, stderr } = await execFileAsync(PYTHON, args, {
      timeout: 45000,
      maxBuffer: 1024 * 1024,
    });
    out.extract = { title: stdout.trim(), log: stderr.slice(-4000) };
  } catch (e: any) {
    out.extract = { error: true, log: (e?.stderr ?? '') + '\n' + (e?.stdout ?? ''), msg: e?.message };
  }

  return Response.json(out);
}