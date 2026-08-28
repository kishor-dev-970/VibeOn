import { SignJWT, jwtVerify } from 'jose';
import { ApiError, requireEnv } from './env';
import { supabaseAdmin } from './supabase';
import { bumpPresence } from './presence';
import type { AppUser } from './types';

function secret(): Uint8Array {
  return new TextEncoder().encode(requireEnv('SESSION_SECRET'));
}

export async function createSessionToken(userId: string): Promise<string> {
  return new SignJWT({})
    .setProtectedHeader({ alg: 'HS256' })
    .setSubject(userId)
    .setIssuedAt()
    .setExpirationTime('30d')
    .sign(secret());
}

export async function requireUser(req: Request): Promise<AppUser> {
  const header = req.headers.get('authorization') ?? '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : '';
  if (!token) throw new ApiError(401, 'Missing bearer token', 'unauthorized');

  let userId: string;
  try {
    const { payload } = await jwtVerify(token, secret());
    userId = payload.sub ?? '';
  } catch {
    throw new ApiError(401, 'Invalid session token', 'invalid_session');
  }
  if (!userId) throw new ApiError(401, 'Invalid session token', 'invalid_session');

  const { data, error } = await supabaseAdmin()
    .from('users')
    .select('id, first_name, last_name, name, code, avatar_url')
    .eq('id', userId)
    .single();
  if (error || !data) throw new ApiError(401, 'User not found', 'user_not_found');
  bumpPresence(userId);
  return data as AppUser;
}
