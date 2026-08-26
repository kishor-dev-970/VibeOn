import { NextRequest } from 'next/server';
import { ApiError } from '@/lib/env';
import { getFbFriends, mapFbUser } from '@/lib/facebook';
import { requireUser } from '@/lib/auth';
import { supabaseAdmin } from '@/lib/supabase';
import { corsPreflight, handleError, ok } from '@/lib/http';
import type { FriendActivity, NowPlaying, Song } from '@/lib/types';

const ACTIVITY_TTL_MS = 10 * 60 * 1000;

interface NowPlayingRow {
  video_id: string;
  title: string;
  channel: string;
  thumbnail_url: string;
  is_playing: boolean;
  updated_at: string;
  users?: { fb_id: string };
}

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

export async function GET(req: NextRequest): Promise<Response> {
  try {
    const user = await requireUser(req);
    const sb = supabaseAdmin();

    const { data: meRow, error: meErr } = await sb
      .from('users')
      .select('fb_token')
      .eq('id', user.id)
      .single();
    if (meErr || !meRow?.fb_token) {
      throw new ApiError(401, 'Facebook session expired, please log in again', 'fb_token_missing');
    }

    const friends = await getFbFriends(meRow.fb_token);
    const friendFbIds = friends.map((f) => f.id);

    const byFbId = new Map<string, NowPlaying>();
    if (friendFbIds.length > 0) {
      const cutoff = new Date(Date.now() - ACTIVITY_TTL_MS).toISOString();
      const { data: rows, error } = await sb
        .from('now_playing')
        .select(
          'video_id, title, channel, thumbnail_url, is_playing, updated_at, users!inner(fb_id)'
        )
        .in('users.fb_id', friendFbIds)
        .gte('updated_at', cutoff);

      if (error) throw new ApiError(502, 'Failed to load activity', 'db_error');

      for (const row of rows as unknown as NowPlayingRow[]) {
        const fbId = row.users?.fb_id;
        if (!fbId || !row.video_id) continue;
        byFbId.set(fbId, {
          videoId: row.video_id,
          title: row.title,
          channel: row.channel,
          thumbnailUrl: row.thumbnail_url,
          isPlaying: row.is_playing,
          updatedAt: row.updated_at,
        });
      }
    }

    const activities: FriendActivity[] = friends.map((f) => {
      const profile = mapFbUser(f);
      return {
        id: profile.fbId,
        name: profile.name,
        avatarUrl: profile.avatarUrl,
        nowPlaying: byFbId.get(profile.fbId) ?? null,
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
