import { NextRequest } from 'next/server';
import { ApiError, requireEnv } from '@/lib/env';
import { requireUser } from '@/lib/auth';
import { corsPreflight, handleError, ok } from '@/lib/http';
import type { Song } from '@/lib/types';

interface YtSearchItem {
  id?: { videoId?: string };
  snippet?: {
    title?: string;
    channelTitle?: string;
    thumbnails?: Record<string, { url?: string }>;
  };
  liveStreamingDetails?: {
    activeLiveChatId?: string;
  };
}

const ENTITIES: Record<string, string> = {
  '&amp;': '&', '&lt;': '<', '&gt;': '>', '&quot;': '"', '&#39;': "'", '&#039;': "'", '&nbsp;': ' ',
};

function decodeHtml(text: string): string {
  return text.replace(/&(?:amp|lt|gt|quot|#0?39|#039|nbsp);/g, (m) => ENTITIES[m] ?? m);
}

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

export async function GET(req: NextRequest): Promise<Response> {
  try {
    await requireUser(req);
    const apiKey = requireEnv('YOUTUBE_API_KEY');
    const genre = req.nextUrl.searchParams.get('genre') ?? 'hindi';

    const queries: Record<string, string[]> = {
      hindi: ['hindi live radio', 'bollywood live music', 'hindi songs live'],
      punjabi: ['punjabi live radio', 'punjabi songs live', 'punjabi music live'],
      english: ['english live music', 'pop live radio', 'english songs live'],
    };

    const searchQueries = queries[genre] ?? queries.hindi;
    const allSongs: Song[] = [];

    for (const q of searchQueries) {
      const params = new URLSearchParams({
        part: 'snippet',
        type: 'video',
        videoEmbeddable: 'true',
        eventType: 'live',
        maxResults: '5',
        q,
        key: apiKey,
      });
      const res = await fetch(`https://www.googleapis.com/youtube/v3/search?${params.toString()}`);
      const json = (await res.json()) as { items?: YtSearchItem[]; error?: { message?: string } };
      if (!res.ok) throw new ApiError(502, json.error?.message ?? 'YouTube search failed', 'youtube_error');

      for (const item of json.items ?? []) {
        if (!item.id?.videoId) continue;
        allSongs.push({
          videoId: item.id.videoId,
          title: decodeHtml(item.snippet?.title ?? 'Unknown title'),
          channel: decodeHtml(item.snippet?.channelTitle ?? 'Unknown channel'),
          thumbnailUrl:
            item.snippet?.thumbnails?.high?.url ??
            item.snippet?.thumbnails?.medium?.url ??
            item.snippet?.thumbnails?.default?.url ??
            '',
        });
      }
    }

    const seen = new Set<string>();
    const unique = allSongs.filter((s) => {
      if (seen.has(s.videoId)) return false;
      seen.add(s.videoId);
      return true;
    });

    return ok({ songs: unique });
  } catch (err) {
    return handleError(err);
  }
}
