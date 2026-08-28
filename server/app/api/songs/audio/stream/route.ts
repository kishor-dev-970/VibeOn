import { NextRequest } from 'next/server';
import { ApiError } from '@/lib/env';
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
    const args = [
      '-m', 'yt_dlp',
      '--no-warnings',
      '-f', 'bestaudio',
      '--get-url',
      url,
    ];

    let stdout = '';
    try {
      const { stdout: out } = await execFileAsync(PYTHON, args, {
        timeout: 30000,
        maxBuffer: 1024 * 1024,
      });
      stdout = out.trim();
    } catch (e: any) {
      const msg = e?.stderr?.trim?.() || e?.message || 'Audio extraction failed';
      throw new ApiError(502, msg, 'audio_extract_error');
    }

    if (!stdout || !stdout.startsWith('http')) {
      throw new ApiError(502, 'Could not extract audio URL', 'audio_extract_error');
    }

    return ok({ audioUrl: stdout });
  } catch (err) {
    return handleError(err);
  }
}
