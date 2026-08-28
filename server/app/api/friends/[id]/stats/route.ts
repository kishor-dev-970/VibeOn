import { NextRequest } from 'next/server';
import { ApiError } from '@/lib/env';
import { requireUser } from '@/lib/auth';
import { supabaseAdmin } from '@/lib/supabase';
import { getPresenceMs } from '@/lib/presence';
import { corsPreflight, handleError, ok } from '@/lib/http';

interface HistoryRow {
  video_id: string;
  title: string;
  channel: string;
  thumbnail_url: string;
  played_at: string;
}

interface NowPlayingRow {
  video_id: string;
  title: string;
  channel: string;
  thumbnail_url: string;
  is_playing: boolean;
  updated_at: string;
}

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
): Promise<Response> {
  try {
    await requireUser(req);
    const { id: friendId } = await params;
    const sb = supabaseAdmin();

    // Resolve the friend by user id or by their 5-digit user code.
    const { data: friendUser, error: friendErr } = await sb
      .from('users')
      .select('id')
      .or(`id.eq.${friendId},code.eq.${friendId}`)
      .maybeSingle();
    if (friendErr || !friendUser) {
      throw new ApiError(404, 'User not found', 'not_found');
    }

    // Get listening history for last 7 days
    const weekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString();
    const { data: history, error: histErr } = await sb
      .from('listening_history')
      .select('video_id, title, channel, thumbnail_url, played_at')
      .eq('user_id', friendUser.id as string)
      .gte('played_at', weekAgo)
      .order('played_at', { ascending: false });
    if (histErr) throw new ApiError(502, 'Failed to load friend stats', 'db_error');

    const rows = (history ?? []) as HistoryRow[];

    // Friend's current activity (if any).
    let nowPlaying: NowPlayingRow | null = null;
    try {
      const { data: np } = await sb
        .from('now_playing')
        .select('video_id, title, channel, thumbnail_url, is_playing, updated_at')
        .eq('user_id', friendUser.id as string)
        .maybeSingle();
      if (np?.video_id) nowPlaying = np as unknown as NowPlayingRow;
    } catch {
      nowPlaying = null;
    }

    const presenceMs = getPresenceMs(friendUser.id as string);
    let lastSeen: string | null = null;
    if (presenceMs !== undefined) {
      lastSeen = new Date(presenceMs).toISOString();
    } else if (nowPlaying?.updated_at) {
      lastSeen = nowPlaying.updated_at;
    } else if (rows.length > 0) {
      lastSeen = rows[0].played_at;
    }

    // Calculate unique songs
    const uniqueSongs = new Map<string, { title: string; channel: string; thumbnail_url: string; count: number }>();
    for (const row of rows) {
      const existing = uniqueSongs.get(row.video_id);
      if (existing) {
        existing.count += 1;
      } else {
        uniqueSongs.set(row.video_id, {
          title: row.title,
          channel: row.channel,
          thumbnail_url: row.thumbnail_url,
          count: 1,
        });
      }
    }

    // Estimate listening time: assume each play is ~3 minutes
    const estimatedMinutes = rows.length * 3;

    const songsListened = Array.from(uniqueSongs.entries()).map(([videoId, data]) => ({
      videoId,
      title: data.title,
      channel: data.channel,
      thumbnailUrl: data.thumbnail_url,
      playCount: data.count,
    }));

    // Sort by play count
    songsListened.sort((a, b) => b.playCount - a.playCount);

    const recentPlays = rows.slice(0, 10).map((row) => ({
      videoId: row.video_id,
      title: row.title,
      channel: row.channel,
      thumbnailUrl: row.thumbnail_url,
      playedAt: row.played_at,
    }));

    return ok({
      totalSongs: songsListened.length,
      totalPlays: rows.length,
      estimatedMinutes,
      songsListened: songsListened.slice(0, 50),
      recentPlays,
      weekStart: weekAgo,
      lastSeen,
      nowPlaying: nowPlaying
        ? {
            videoId: nowPlaying.video_id,
            title: nowPlaying.title,
            channel: nowPlaying.channel,
            thumbnailUrl: nowPlaying.thumbnail_url,
            isPlaying: nowPlaying.is_playing,
            updatedAt: nowPlaying.updated_at,
          }
        : null,
    });
  } catch (err) {
    return handleError(err);
  }
}