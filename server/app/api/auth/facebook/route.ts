import { NextRequest } from 'next/server';
import { ApiError } from '@/lib/env';
import { getFbMe, mapFbUser, verifyFacebookToken } from '@/lib/facebook';
import { createSessionToken } from '@/lib/auth';
import { supabaseAdmin } from '@/lib/supabase';
import { corsPreflight, handleError, ok } from '@/lib/http';

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

export async function POST(req: NextRequest): Promise<Response> {
  try {
    const body = (await req.json().catch(() => null)) as { accessToken?: string } | null;
    const accessToken = body?.accessToken;
    if (!accessToken) throw new ApiError(400, 'accessToken is required', 'bad_request');

    await verifyFacebookToken(accessToken);
    const profile = mapFbUser(await getFbMe(accessToken));

    const { data: user, error } = await supabaseAdmin()
      .from('users')
      .upsert(
        {
          fb_id: profile.fbId,
          name: profile.name,
          avatar_url: profile.avatarUrl,
          fb_token: accessToken,
        },
        { onConflict: 'fb_id' }
      )
      .select('id, fb_id, name, avatar_url')
      .single();
    if (error || !user) throw new ApiError(502, 'Failed to create user session', 'db_error');

    const token = await createSessionToken(user.id);
    return ok({ token, user });
  } catch (err) {
    return handleError(err);
  }
}
