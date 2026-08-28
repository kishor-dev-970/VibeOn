import { NextRequest } from 'next/server';
import { createReadStream, existsSync, statSync } from 'node:fs';
import { mkdir } from 'node:fs/promises';
import { Readable } from 'node:stream';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { execFile } from 'child_process';
import { promisify } from 'util';
import { ApiError } from '@/lib/env';
import { YTDL_COMMON_ARGS } from '@/lib/ytdl';
import { corsPreflight, handleError } from '@/lib/http';

const execFileAsync = promisify(execFile);

const PYTHON = process.env.PYTHON_PATH ?? 'C:\\Users\\kishor\\AppData\\Local\\Programs\\Python\\Python312\\python.exe';
const FFMPEG_DIR = process.env.FFMPEG_PATH ?? `${process.env.LOCALAPPDATA}\\Programs\\ffmpeg\\bin`;
const CACHE_DIR = process.env.VIDEO_CACHE_DIR ?? join(tmpdir(), 'social-music-videos');

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
};

const VIDEO_ID_RE = /^[A-Za-z0-9_-]{5,20}$/;

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

async function getCachedPath(videoId: string): Promise<string | null> {
  const file = join(CACHE_DIR, `${videoId}.mp4`);
  if (existsSync(file) && statSync(file).size > 0) return file;
  return null;
}

async function resolveProgressiveUrl(videoId: string): Promise<string | null> {
  const url = `https://www.youtube.com/watch?v=${videoId}`;
  const args = ['-m', 'yt_dlp', '--no-warnings', ...YTDL_COMMON_ARGS, '-f', '22/18/b[ext=mp4]/b', '--get-url', url];
  try {
    const { stdout } = await execFileAsync(PYTHON, args, {
      timeout: 30000,
      maxBuffer: 1024 * 1024,
    });
    const out = stdout.trim().split(/\r?\n/).filter((l) => l.startsWith('http'));
    return out.length === 1 ? out[0] : null;
  } catch {
    return null;
  }
}

async function mergeVideo(videoId: string): Promise<string> {
  await mkdir(CACHE_DIR, { recursive: true });
  const output = join(CACHE_DIR, `${videoId}.%(ext)s`);
  const url = `https://www.youtube.com/watch?v=${videoId}`;
  const args = [
    '-m', 'yt_dlp',
    '--no-warnings',
    ...YTDL_COMMON_ARGS,
    '--no-part',
    '--ffmpeg-location', FFMPEG_DIR,
    '-f', 'bv*[height<=720][ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b',
    '--merge-output-format', 'mp4',
    '-o', output,
    url,
  ];
  try {
    await execFileAsync(PYTHON, args, {
      timeout: 300000,
      maxBuffer: 1024 * 1024,
    });
  } catch (e: any) {
    const msg = e?.stderr?.trim?.() || e?.message || 'Video extraction failed';
    throw new ApiError(502, msg.slice(0, 500), 'video_extract_error');
  }
  const cached = await getCachedPath(videoId);
  if (!cached) throw new ApiError(502, 'Could not produce video file', 'video_extract_error');
  return cached;
}

async function serveFile(abspath: string, req: NextRequest): Promise<Response> {
  const { size } = statSync(abspath);
  const range = req.headers.get('range');
  const headers: Record<string, string> = {
    'Content-Type': 'video/mp4',
    'Accept-Ranges': 'bytes',
    'Cache-Control': 'private, max-age=3600',
    ...CORS_HEADERS,
  };

  if (range) {
    const m = /bytes=(\d*)-(\d*)/.exec(range);
    if (m) {
      const start = m[1] ? parseInt(m[1], 10) : 0;
      const end = m[2] ? parseInt(m[2], 10) : size - 1;
      if (start >= 0 && start < size && end >= start) {
        headers['Content-Range'] = `bytes ${start}-${end}/${size}`;
        headers['Content-Length'] = String(end - start + 1);
        const stream = createReadStream(abspath, { start, end });
        return new Response(Readable.toWeb(stream) as unknown as BodyInit, {
          status: 206,
          headers,
        });
      }
    }
  }

  headers['Content-Length'] = String(size);
  const stream = createReadStream(abspath);
  return new Response(Readable.toWeb(stream) as unknown as BodyInit, { status: 200, headers });
}

export async function GET(req: NextRequest): Promise<Response> {
  try {
    const videoId = req.nextUrl.searchParams.get('videoId');
    if (!videoId || !VIDEO_ID_RE.test(videoId)) throw new ApiError(400, 'videoId required', 'bad_request');

    const serving = req.nextUrl.searchParams.get('stream') === '1';

    // Direct media request from the native player -> serve the cached merged file.
    if (serving) {
      const cached = await getCachedPath(videoId);
      if (!cached) throw new ApiError(404, 'Video not ready', 'video_not_ready');
      return serveFile(cached, req);
    }

    // Fast path: single-file (progressive) direct URL, no merge needed.
    const progressive = await resolveProgressiveUrl(videoId);
    if (progressive) {
      return new Response(JSON.stringify({ videoUrl: progressive }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', ...CORS_HEADERS },
      });
    }

    // Server path: video is DASH-only, so download + merge, then stream from our cache.
    await mergeVideo(videoId);
    const proto = req.headers.get('x-forwarded-proto') ?? 'http';
    const host = req.headers.get('host') ?? 'localhost:3000';
    const videoUrl = `${proto}://${host}/api/songs/video/stream?videoId=${encodeURIComponent(videoId)}&stream=1`;
    return new Response(JSON.stringify({ videoUrl }), {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...CORS_HEADERS },
    });
  } catch (err) {
    return handleError(err);
  }
}