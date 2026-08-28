import { NextRequest } from 'next/server';
import { corsPreflight, handleError, ok } from '@/lib/http';
import { ytdlLiveSearch, ytdlSearch } from '@/lib/ytdl';

const LIVE_QUERIES: Record<string, string[]> = {
  hindi: ['hindi songs live 24/7', 'bollywood music live stream', 'hindi gaane live radio'],
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

    const batches = await Promise.all(
      liveQueries.map((q) => ytdlLiveSearch(q, 8).catch(() => [] as any[]))
    );
    for (const batch of batches) {
      for (const s of batch) {
        if (!seen.has(s.videoId)) {
          seen.add(s.videoId);
          all.push(s);
        }
      }
    }

    if (all.length === 0) {
      const fallbackBatches = await Promise.all(
        fallbackQueries.map((q) => ytdlSearch(q, 10).catch(() => [] as any[]))
      );
      for (const batch of fallbackBatches) {
        for (const s of batch) {
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
