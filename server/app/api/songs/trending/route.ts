import { NextRequest } from 'next/server';
import { corsPreflight, handleError, ok } from '@/lib/http';
import { ytdlSearch } from '@/lib/ytdl';

const SEARCH_QUERIES = [
  'trending songs 2025',
  'top hits this week',
  'popular music videos',
  'viral songs now',
];

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

// O(n) Fisher-Yates uniform shuffle
function shuffle<T>(arr: readonly T[]): T[] {
  const result = [...arr];
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}

export async function GET(_req: NextRequest): Promise<Response> {
  try {
    const queries = shuffle(SEARCH_QUERIES).slice(0, 2);
    // Fetch queries concurrently in parallel: O(1) concurrent wait instead of sequential
    const batches = await Promise.all(queries.map((q) => ytdlSearch(q, 50).catch(() => [])));
    const all = batches.flat();

    const seen = new Set<string>();
    const unique = all
      .filter((s) => {
        if (!s.videoId || seen.has(s.videoId)) return false;
        seen.add(s.videoId);
        return true;
      })
      .slice(0, 100);
    return ok({ songs: unique });
  } catch (err) {
    return handleError(err);
  }
}
