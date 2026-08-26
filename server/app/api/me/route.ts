import { NextRequest } from 'next/server';
import { requireUser } from '@/lib/auth';
import { corsPreflight, handleError, ok } from '@/lib/http';

export async function OPTIONS(): Promise<Response> {
  return corsPreflight();
}

export async function GET(req: NextRequest): Promise<Response> {
  try {
    const user = await requireUser(req);
    return ok({ user });
  } catch (err) {
    return handleError(err);
  }
}
