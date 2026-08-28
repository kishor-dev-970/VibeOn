import { ok } from '@/lib/http';

export async function GET(): Promise<Response> {
  return ok({
    status: 'ok',
    commit: process.env.RENDER_GIT_COMMIT ?? process.env.COMMIT_SHA ?? 'local',
    uptime: process.uptime(),
  });
}