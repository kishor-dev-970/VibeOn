import { NextRequest } from 'next/server';
import { ApiError } from '@/lib/env';
import { requireUser } from '@/lib/auth';
import { supabaseAdmin } from '@/lib/supabase';
import { corsPreflight, handleError, ok } from '@/lib/http';
import type { FriendActivity, NowPlaying, Song } from '@/lib/types';
import { getPresenceMs } from '@/lib/presence';

const ACTIVITY_TTL_MS = 10 * 60 * 1000;

interface NowPlayingRow {
  user_id: string;
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

export async function GET(req: NextRequest): Promise<Response> {
  try {
    const user = await requireUser(req);
    const sb = supabaseAdmin();

    const { data: users, error: usersErr } = await sb
      .from('users')
      .select('id, name, code, avatar_url')
      .neq('id', user.id)
      .order('name');
    if (usersErr) throw new ApiError(502, 'Failed to load users', 'db_error');

    const userIds = (users ?? []).map((u) => u.id);
    const byUserId = new Map<string, NowPlaying>();
    if (userIds.length > 0) {
      const cutoff = new Date(Date.now() - ACTIVITY_TTL_MS).toISOString();
      const { data: rows, error } = await sb
        .from('now_playing')
        .select('user_id, video_id, title, channel, thumbnail_url, is_playing, updated_at')
        .in('user_id', userIds)
        .gte('updated_at', cutoff);

      if (error) throw new ApiError(502, 'Failed to load activity', 'db_error');
      for (const row of (rows as unknown as NowPlayingRow[]) ?? []) {
        if (!row.video_id) continue;
        byUserId.set(row.user_id, {
          videoId: row.video_id,
          title: row.title,
          channel: row.channel,
          thumbnailUrl: row.thumbnail_url,
          isPlaying: row.is_playing,
          updatedAt: row.updated_at,
        });
      }
    }

    const activities: FriendActivity[] = (users ?? []).map((u) => {
      const now = byUserId.get(u.id);
      const presenceMs = getPresenceMs(u.id);
      let lastActive: string | null = null;
      if (presenceMs !== undefined) {
        lastActive = new Date(presenceMs).toISOString();
      } else if (now?.updatedAt) {
        lastActive = now.updatedAt;
      }
      return {
        id: u.id,
        name: u.name,
        code: u.code,
        avatarUrl: u.avatar_url,
        nowPlaying: now ?? null,
        lastActive,
      };
    });
    return ok({ friends: activities });
  } catch (err) {
    return handleError(err);
  }
}

export async function PUT(req: NextRequest): Promise<Response> {
  try {
    const user = await requireUser(req);
    const body = (await req.json().catch(() => null)) as
      | { song?: Song; isPlaying?: boolean }
      | null;
    const song = body?.song;
    if (!song?.videoId) throw new ApiError(400, 'song with videoId is required', 'bad_request');

    const { error } = await supabaseAdmin().from('now_playing').upsert(
      {
        user_id: user.id,
        video_id: song.videoId,
        title: song.title ?? '',
        channel: song.channel ?? '',
        thumbnail_url: song.thumbnailUrl ?? '',
        is_playing: body?.isPlaying ?? true,
        updated_at: new Date().toISOString(),
      },
      { onConflict: 'user_id' }
    );
    if (error) throw new ApiError(502, 'Failed to update activity', 'db_error');

    // Log to listening history
    if (body?.isPlaying) {
      await supabaseAdmin().from('listening_history').insert({
        user_id: user.id,
        video_id: song.videoId,
        title: song.title ?? '',
        channel: song.channel ?? '',
        thumbnail_url: song.thumbnailUrl ?? '',
      });
    }

    return ok({ success: true });
  } catch (err) {
    return handleError(err);
  }
}

export async function DELETE(req: NextRequest): Promise<Response> {
  try {
    const user = await requireUser(req);
    const { error } = await supabaseAdmin()
      .from('now_playing')
      .delete()
      .eq('user_id', user.id);
    if (error) throw new ApiError(502, 'Failed to clear activity', 'db_error');
    return ok({ success: true });
  } catch (err) {
    return handleError(err);
  }
}
