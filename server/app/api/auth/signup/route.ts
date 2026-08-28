import { NextRequest } from 'next/server';
import { ApiError } from '@/lib/env';
import { createSessionToken } from '@/lib/auth';
import { supabaseAdmin } from '@/lib/supabase';
import { corsPreflight, handleError, ok } from '@/lib/http';
import type { AppUser } from '@/lib/types';

const USER_SELECT = 'id, first_name, last_name, name, code, avatar_url';

function normalizeName(value: unknown, label: string): string {
  const trimmed = String(value ?? '').trim().replace(/\s+/g, ' ');
  if (!trimmed) throw new ApiError(400, `${label} is required`, 'bad_request');
  if (trimmed.length > 100) throw new ApiError(400, `${label} is too long`, 'bad_request');
  return trimmed;
}

async function generateUniqueCode(): Promise<{ code: string; error: unknown }> {
  for (let i = 0; i < 50; i++) {
    const code = String(Math.floor(10000 + Math.random() * 90000));
    const { data, error } = await supabaseAdmin()
      .from('users')
      .select('id')
      .eq('code', code)
      .maybeSingle();
    if (error) return { code: '', error };
    if (!data) return { code, error: null };
  }
  return { code: '', error: new Error('Ran out of candidate user codes') };
}

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

export async function POST(req: NextRequest): Promise<Response> {
  try {
    const body = (await req.json().catch(() => null)) as
      | { firstName?: string; lastName?: string }
      | null;
    const firstName = normalizeName(body?.firstName, 'First name');
    const lastName = normalizeName(body?.lastName, 'Last name');
    const name = `${firstName} ${lastName}`;
    const sb = supabaseAdmin();

    // Log back in to an existing account with the same first + last name.
    const { data: existing, error: findErr } = await sb
      .from('users')
      .select(USER_SELECT)
      .eq('first_name', firstName)
      .eq('last_name', lastName)
      .maybeSingle();
    if (findErr) throw new ApiError(502, 'Failed to load account', 'db_error');
    if (existing) {
      const token = await createSessionToken(existing.id);
      return ok({ token, user: existing as AppUser });
    }

    const { code, error: codeErr } = await generateUniqueCode();
    if (codeErr) throw new ApiError(502, 'Failed to allocate user code', 'db_error');

    const { data: user, error } = await sb
      .from('users')
      .insert({ first_name: firstName, last_name: lastName, name, code })
      .select(USER_SELECT)
      .single();
    if (error || !user) throw new ApiError(502, 'Failed to create account', 'db_error');

    const token = await createSessionToken(user.id);
    return ok({ token, user: user as AppUser });
  } catch (err) {
    return handleError(err);
  }
}