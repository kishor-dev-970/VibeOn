import { NextRequest } from 'next/server';
import { ApiError } from '@/lib/env';
import { corsPreflight, handleError, ok } from '@/lib/http';
import { ytdlSearch } from '@/lib/ytdl';

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

export async function GET(req: NextRequest): Promise<Response> {
  try {
    const q = req.nextUrl.searchParams.get('q');
    if (!q || q.trim().length < 2) throw new ApiError(400, 'Query parameter "q" is required', 'bad_request');
    const songs = await ytdlSearch(q, 15);
    return ok({ songs });
  } catch (err) {
    return handleError(err);
  }
}
