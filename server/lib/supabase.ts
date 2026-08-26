import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { requireEnv } from './env';

let client: SupabaseClient | null = null;

export function supabaseAdmin(): SupabaseClient {
  if (!client) {
    client = createClient(requireEnv('SUPABASE_URL'), requireEnv('SUPABASE_SERVICE_ROLE_KEY'), {
      auth: { persistSession: false },
    });
  }
  return client;
}
