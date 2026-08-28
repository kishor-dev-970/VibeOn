import { requireEnv } from '@/lib/env';

function envStatus(name: string): boolean {
  try {
    requireEnv(name);
    return true;
  } catch {
    return false;
  }
}

const endpoints = [
  ['POST', '/api/auth/signup', 'Create or log in with first + last name'],
  ['GET', '/api/me', 'Current user profile'],
  ['GET', '/api/songs/search?q=', 'Search YouTube for songs'],
  ['GET', '/api/activity', "Friends' now-playing feed (all users)"],
  ['PUT', '/api/activity', 'Set my now-playing song'],
  ['DELETE', '/api/activity', 'Clear my now-playing status'],
];

export default function Home() {
  const required = [
    'SUPABASE_URL',
    'SUPABASE_SERVICE_ROLE_KEY',
    'SESSION_SECRET',
  ];
  const missing = required.filter((name) => !envStatus(name));

  return (
    <main style={{ maxWidth: 720, margin: '0 auto', padding: '48px 24px', fontFamily: 'monospace' }}>
      <h1 style={{ fontSize: 28 }}>Social Music API</h1>
      <p style={{ color: '#666' }}>
        Backend for the Social Music mobile app (Expo). Users sign in with their first + last
        name, and every user can see what the others are listening to in the live feed.
      </p>

      <h2>Status</h2>
      {missing.length === 0 ? (
        <p style={{ color: '#0a0' }}>All environment variables configured.</p>
      ) : (
        <p style={{ color: '#c00' }}>
          Missing env vars: {missing.join(', ')} — see .env.local.example
        </p>
      )}

      <h2>Endpoints</h2>
      <ul style={{ lineHeight: 1.9 }}>
        {endpoints.map(([method, path, desc]) => (
          <li key={path + method}>
            <strong>{method}</strong> <code>{path}</code> — {desc}
          </li>
        ))}
      </ul>
    </main>
  );
}
