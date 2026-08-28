import { NextRequest } from 'next/server';
import { corsPreflight, handleError, ok } from '@/lib/http';
import { ytdlLiveSearch, ytdlSearch } from '@/lib/ytdl';

const LIVE_QUERIES: Record<string, string[]> = {
  hindi: ['hindi live radio', 'bollywood live music', 'hindi songs live 24/7'],
  punjabi: ['punjabi live radio', 'punjabi songs live', 'punjabi music 24/7'],
  english: ['english live music radio', 'pop live radio', 'english songs live 24/7'],
};

const FALLBACK_QUERIES: Record<string, string[]> = {
  hindi: ['best hindi songs 2025', 'bollywood hits'],
  punjabi: ['best punjabi songs 2025', 'punjabi hits'],
  english: ['best pop hits 2025', 'top english songs'],
};

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

export async function GET(req: NextRequest): Promise<Response> {
  try {
    const genre = req.nextUrl.searchParams.get('genre') ?? 'hindi';
    const liveQueries = LIVE_QUERIES[genre] ?? LIVE_QUERIES.hindi;
    const fallbackQueries = FALLBACK_QUERIES[genre] ?? FALLBACK_QUERIES.hindi;

    const seen = new Set<string>();
    const all: any[] = [];

    for (const q of liveQueries) {
      for (const s of await ytdlLiveSearch(q, 8)) {
        if (!seen.has(s.videoId)) {
          seen.add(s.videoId);
          all.push(s);
        }
      }
    }

    if (all.length === 0) {
      for (const q of fallbackQueries) {
        for (const s of await ytdlSearch(q, 10)) {
          if (!seen.has(s.videoId)) {
            seen.add(s.videoId);
            all.push(s);
          }
        }
      }
    }

    return ok({ songs: all });
  } catch (err) {
    return handleError(err);
  }
}
