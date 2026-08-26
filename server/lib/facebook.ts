import { ApiError, requireEnv } from './env';

const GRAPH = 'https://graph.facebook.com/v21.0';

interface FbUser {
  id: string;
  name: string;
  picture?: { data?: { url?: string } };
}

async function graphGet<T>(path: string, params: Record<string, string>, userToken?: string): Promise<T> {
  const search = new URLSearchParams(params);
  if (userToken) search.set('access_token', userToken);
  const res = await fetch(`${GRAPH}/${path}?${search.toString()}`);
  const json = (await res.json()) as { error?: { message?: string } };
  if (!res.ok) {
    throw new ApiError(
      res.status === 400 || res.status === 401 ? 401 : 502,
      json.error?.message ?? 'Facebook Graph API request failed',
      'facebook_error'
    );
  }
  return json as T;
}

export async function verifyFacebookToken(userToken: string): Promise<string> {
  const appId = requireEnv('FACEBOOK_APP_ID');
  const appSecret = requireEnv('FACEBOOK_APP_SECRET');
  let data: { is_valid?: boolean; user_id?: string };
  try {
    const res = await fetch(
      `${GRAPH}/debug_token?${new URLSearchParams({
        input_token: userToken,
        access_token: `${appId}|${appSecret}`,
      })}`
    );
    const json = (await res.json()) as {
      data?: { is_valid?: boolean; user_id?: string };
      error?: { message?: string };
    };
    if (!res.ok) {
      throw new ApiError(502, json.error?.message ?? 'Failed to verify Facebook token', 'facebook_error');
    }
    data = json.data ?? {};
  } catch (err) {
    if (err instanceof ApiError) throw err;
    throw new ApiError(502, 'Failed to verify Facebook token', 'facebook_error');
  }
  if (!data.is_valid || !data.user_id) {
    throw new ApiError(401, 'Invalid or expired Facebook token', 'invalid_fb_token');
  }
  return data.user_id;
}

export async function getFbMe(userToken: string): Promise<FbUser> {
  return graphGet<FbUser>('me', { fields: 'id,name,picture.type(large)' }, userToken);
}

export async function getFbFriends(userToken: string): Promise<FbUser[]> {
  const json = await graphGet<{ data?: FbUser[] }>(
    'me/friends',
    { fields: 'id,name,picture.type(large)' },
    userToken
  );
  return json.data ?? [];
}

export function mapFbUser(fb: FbUser): { fbId: string; name: string; avatarUrl: string | null } {
  return { fbId: fb.id, name: fb.name, avatarUrl: fb.picture?.data?.url ?? null };
}
