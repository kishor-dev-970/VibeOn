import { NextResponse } from 'next/server';
import { ApiError } from './env';

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
};

export function corsPreflight(): NextResponse {
  return new NextResponse(null, { status: 204, headers: CORS_HEADERS });
}

export function ok(data: unknown, status = 200): NextResponse {
  return NextResponse.json(data, { status, headers: CORS_HEADERS });
}

export function handleError(err: unknown): NextResponse {
  if (err instanceof ApiError) {
    return NextResponse.json({ error: err.message, code: err.code }, { status: err.status, headers: CORS_HEADERS });
  }
  console.error(err);
  return NextResponse.json({ error: 'Internal server error' }, { status: 500, headers: CORS_HEADERS });
}
