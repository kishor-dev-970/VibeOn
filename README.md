# Social Music

Listen to music with everyone. Sign in with your first and last name, search and play
songs (YouTube), and see which song everyone else is listening to right now.

```
D:\MyopenCode\
├── server/    Next.js backend (name sign-in, YouTube search proxy, activity feed)
├── mobile/    Expo app SDK 57 (sign-in, player, friends tabs)
└── supabase/  Database schema (users, now_playing, listening_history)
```

## How it works

1. **Sign-in** — on launch the app asks for your first and last name and calls
   `POST /api/auth/signup`. The server finds-or-creates the user in Supabase,
   assigns a unique 5-digit **user code**, and returns a 30-day JWT session.
2. **Playback** — the app searches YouTube through `GET /api/songs/search`
   (API key stays on the server) and plays ad-free via `expo-video` using
   server-side `yt-dlp`/ffmpeg extraction (`GET /api/songs/audio/stream` / video).
3. **Social feed** — while playing, the app calls `PUT /api/activity`. The Friends
   tab polls `GET /api/activity` every 30s; the server lists **all** app users as
   mutually-visible friends and joins their current song. Entries auto-expire after
   10 minutes of inactivity. Listening history is stored per user and shared through
   `GET /api/friends/:id/stats` (top songs this week + recent plays).

## Prerequisites

- Node.js 18+
- A [Supabase](https://supabase.com) project
- Android Studio / Xcode for running a dev build

## Setup

### 1. Database

In Supabase SQL editor run everything in [`supabase/schema.sql`](supabase/schema.sql).
**Existing projects**: also run [`supabase/migrations/001_user_codes.sql`](supabase/migrations/001_user_codes.sql).
Then copy **Project URL** and **service_role key** from Project Settings → API.

### 2. Backend env

```powershell
cd server
Copy-Item .env.local.example .env.local
# fill in values; generate SESSION_SECRET with: node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
npm install
npm run dev        # http://localhost:3000 (status page shows missing vars)
```

### 3. Mobile env

```powershell
cd mobile
Copy-Item .env.example .env
# EXPO_PUBLIC_API_URL: use your LAN IP for device testing, e.g. http://192.168.1.20:3000
npm install
npx expo prebuild --clean
npx expo run:android          # or npx expo run:ios
```

## Testing the social loop

1. Install the build and sign in with a name — you get a user code automatically.
2. Your own code is shown under **Settings**.
3. Every other user appears in the Friends tab; tap them to see their current song,
   top songs this week, and recent plays.

## Notes & next steps

- The 5-digit user code is the public handle — share it so people can recognize you.
- Friend activity uses 30s polling; swap in Supabase Realtime subscriptions for instant updates.
- YouTube audio/video extraction via `yt-dlp` is fine for demos but not a licensed music
  service — for production consider licensed catalogs (Spotify/Deezer SDKs).