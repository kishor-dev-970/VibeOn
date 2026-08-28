import { NextRequest } from 'next/server';
import { ApiError } from '@/lib/env';
import { YTDL_COMMON_ARGS } from '@/lib/ytdl';
import { corsPreflight, handleError, ok } from '@/lib/http';
import { execFile } from 'child_process';
import { promisify } from 'util';

const execFileAsync = promisify(execFile);

const PYTHON = process.env.PYTHON_PATH ?? 'C:\\Users\\kishor\\AppData\\Local\\Programs\\Python\\Python312\\python.exe';

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

export async function GET(req: NextRequest): Promise<Response> {
  try {
    const videoId = req.nextUrl.searchParams.get('videoId');
    if (!videoId) throw new ApiError(400, 'videoId required', 'bad_request');

    const url = `https://www.youtube.com/watch?v=${videoId}`;

    // Audio-only DASH formats can be missing URLs (YouTube SABR experiment
    // / needing n-challenge solving), so fall back to the same single-file
    // progressive formats the video route uses (proven to work from Render).
    const selectors = ['bestaudio', '22/18/b[ext=mp4]/b'];
    let lastError: unknown = null;

    for (const selector of selectors) {
      const args = [
        '-m', 'yt_dlp',
        '--no-warnings',
        ...YTDL_COMMON_ARGS,
        '-f', selector,
        '--get-url',
        url,
      ];
      try {
        const { stdout } = await execFileAsync(PYTHON, args, {
          timeout: 45000,
          maxBuffer: 1024 * 1024,
        });
        const lines = stdout.trim().split(/\r?\n/).filter((l) => l.startsWith('http'));
        if (lines.length === 1) {
          return ok({ audioUrl: lines[0] });
        }
      } catch (e) {
        lastError = e;
      }
    }

    const msg =
      lastError && typeof lastError === 'object' && 'stderr' in lastError
        ? String((lastError as { stderr?: unknown }).stderr ?? '').trim()
        : '';
    throw new ApiError(502, msg || 'Could not extract audio URL', 'audio_extract_error');
  } catch (err) {
    return handleError(err);
  }
}
