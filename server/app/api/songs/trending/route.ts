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

export async function GET(_req: NextRequest): Promise<Response> {
  try {
    const shuffled = [...SEARCH_QUERIES].sort(() => Math.random() - 0.5);
    const queries = shuffled.slice(0, 2);
    const all: any[] = [];
    for (const q of queries) {
      all.push(...(await ytdlSearch(q, 60)));
    }
    const seen = new Set<string>();
    const unique = all
      .filter((s) => {
        if (seen.has(s.videoId)) return false;
        seen.add(s.videoId);
        return true;
      })
      .slice(0, 100);
    return ok({ songs: unique });
  } catch (err) {
    return handleError(err);
  }
}
