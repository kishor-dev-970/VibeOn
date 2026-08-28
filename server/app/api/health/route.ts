import { ok } from '@/lib/http';

export async function GET(): Promise<Response> {
  return ok({ status: 'ok', uptime: process.uptime() });
}