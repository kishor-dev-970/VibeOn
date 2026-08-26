import { NextRequest } from 'next/server';
import { ApiError, requireEnv } from '@/lib/env';
import { requireUser } from '@/lib/auth';
import { corsPreflight, handleError, ok } from '@/lib/http';
import type { Song } from '@/lib/types';

const ENTITIES: Record<string, string> = {
  '&amp;': '&',
  '&lt;': '<',
  '&gt;': '>',
  '&quot;': '"',
  '&#39;': "'",
  '&#039;': "'",
  '&nbsp;': ' ',
};

function decodeHtml(text: string): string {
  return text.replace(/&(?:amp|lt|gt|quot|#0?39|#039|nbsp);/g, (m) => ENTITIES[m] ?? m);
}

interface YtSearchItem {
  id?: { videoId?: string };
  snippet?: {
    title?: string;
    channelTitle?: string;
    thumbnails?: Record<string, { url?: string }>;
  };
}

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

export async function GET(req: NextRequest): Promise<Response> {
  try {
    await requireUser(req);
    const q = req.nextUrl.searchParams.get('q');
    if (!q || q.trim().length < 2) throw new ApiError(400, 'Query parameter "q" is required', 'bad_request');

    const params = new URLSearchParams({
      part: 'snippet',
      type: 'video',
      videoEmbeddable: 'true',
      maxResults: '15',
      q,
      key: requireEnv('YOUTUBE_API_KEY'),
    });
    const res = await fetch(`https://www.googleapis.com/youtube/v3/search?${params.toString()}`);
    const json = (await res.json()) as { items?: YtSearchItem[]; error?: { message?: string } };
    if (!res.ok) throw new ApiError(502, json.error?.message ?? 'YouTube search failed', 'youtube_error');

    const songs: Song[] = (json.items ?? [])
      .filter((item) => item.id?.videoId)
      .map((item) => ({
        videoId: item.id!.videoId!,
        title: decodeHtml(item.snippet?.title ?? 'Unknown title'),
        channel: decodeHtml(item.snippet?.channelTitle ?? 'Unknown channel'),
        thumbnailUrl:
          item.snippet?.thumbnails?.high?.url ??
          item.snippet?.thumbnails?.medium?.url ??
          item.snippet?.thumbnails?.default?.url ??
          '',
      }));
    return ok({ songs });
  } catch (err) {
    return handleError(err);
  }
}
