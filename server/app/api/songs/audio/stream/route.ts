import { NextRequest } from 'next/server';
import { ApiError } from '@/lib/env';
import { YTDL_COMMON_ARGS } from '@/lib/ytdl';
import { corsPreflight, handleError, ok } from '@/lib/http';
import { getOrExtract } from '@/lib/stream-cache';
import { execFile } from 'child_process';
import { promisify } from 'util';

const execFileAsync = promisify(execFile);

const PYTHON = process.env.PYTHON_PATH ?? 'C:\\Users\\kishor\\AppData\\Local\\Programs\\Python\\Python312\\python.exe';

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

async function extractAudioUrl(videoId: string): Promise<string> {
  const url = `https://www.youtube.com/watch?v=${videoId}`;

  // Audio-only DASH formats can be missing URLs (YouTube SABR experiment
  // / needing n-challenge solving), so fall back to the same single-file
  // progressive formats the video route uses.
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
        return lines[0];
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
}

export async function GET(req: NextRequest): Promise<Response> {
  try {
    const videoId = req.nextUrl.searchParams.get('videoId');
    if (!videoId) throw new ApiError(400, 'videoId required', 'bad_request');

    const url = await getOrExtract(`audio:${videoId}`, () => extractAudioUrl(videoId));
    return ok({ audioUrl: url });
  } catch (err) {
    // When everything failed, re-run the plain selector with -v so the error
    // body shows exactly which client/phase YouTube blocked (useful for
    // diagnosing the datacenter bot wall without container shell access).
    if (err instanceof ApiError) {
      const videoId = req.nextUrl.searchParams.get('videoId');
      try {
        const verbose = await execFileAsync(
          PYTHON,
          [
            '-m', 'yt_dlp',
            '--no-warnings',
            '-v',
            ...YTDL_COMMON_ARGS,
            '-f', 'b',
            '--get-url',
            `https://www.youtube.com/watch?v=${videoId}`,
          ],
          { timeout: 45000, maxBuffer: 1024 * 1024 }
        );
        if (verbose.stdout.trim().startsWith('http')) {
          const lines = verbose.stdout.trim().split(/\r?\n/).filter((l) => l.startsWith('http'));
          return ok({ audioUrl: lines[0] });
        }
      } catch (e) {
        const stderr = String((e as { stderr?: unknown }).stderr ?? '').trim();
        const interesting =
          stderr
            .split(/\r?\n/)
            .filter((l) => /ERROR: \[youtube\]|Sign in|Login required|bot|po_token|provider|HTTP Error|format/i.test(l))
            .slice(-6)
            .join('\n') || stderr;
        throw new ApiError(502, interesting || (err as ApiError).message, 'audio_extract_error');
      }
    }
    return handleError(err);
  }
}
