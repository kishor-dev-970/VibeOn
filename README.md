# Social Music

Listen to music with your Facebook friends. Log in with Facebook, search and play
songs (YouTube), and see which song each of your friends is listening to right now.

```
D:\MyopenCode\
├── server/    Next.js backend (Facebook auth, YouTube search proxy, activity feed)
├── mobile/    Expo app SDK 57 (login, player, friends tabs)
└── supabase/  Database schema (users, now_playing)
```

## How it works

1. **Login** — the mobile app uses the native Facebook SDK (`react-native-fbsdk-next`)
   with `public_profile` + `user_friends` permissions. The access token is sent to
   `POST /api/auth/facebook`, verified server-side via Graph API `debug_token`,
   then the user is stored in Supabase and a 30-day JWT session is returned.
2. **Playback** — the app searches YouTube through `GET /api/songs/search`
   (API key stays on the server) and plays videos with `react-native-youtube-iframe`.
3. **Social feed** — while playing, the app calls `PUT /api/activity`. The Friends
   tab polls `GET /api/activity` every 5s; the server resolves your Facebook friends,
   matches them against app users, and joins their current song. Entries auto-expire
   after 10 minutes of inactivity.

## Prerequisites

- Node.js 18+
- A [Supabase](https://supabase.com) project
- A [Facebook developer app](https://developers.facebook.com)
- A [YouTube Data API v3](https://console.cloud.google.com) key
- Android Studio / Xcode for running a dev build (required — Facebook login cannot run in Expo Go)

## Setup

### 1. Database

In Supabase SQL editor run everything in [`supabase/schema.sql`](supabase/schema.sql).
Then copy **Project URL** and **service_role key** from Project Settings → API.

### 2. Facebook app

1. Create an app at developers.facebook.com (type: Consumer).
2. Add product **Facebook Login**.
3. Settings → Basic: note the **App ID** and **Client Token**; copy the App Secret too.
4. For Android: add platform with package name `com.example.socialmusic`, class
   `com.example.socialmusic.MainActivity`, plus a key hash for your debug keystore:
   ```
   keytool -exportcert -alias androiddebugkey -keystore %USERPROFILE%\.android\debug.keystore | openssl sha1 -binary | openssl base64
   ```
5. Keep the app in **Development mode** for testing. In dev mode only roles
   (Administrators/Developers/Testers) can log in — add your testers under Roles.

> **Important limitation:** Facebook's `user_friends` permission only returns friends
> who have *also* logged into this same app with Facebook. Two friends must both
> install/login before they appear in each other's feed. This is a Facebook policy,
> not a bug.

### 3. YouTube API

Google Cloud Console → enable **YouTube Data API v3** → create an API key.
Restrict it to that API. Free quota = 10,000 units/day (~100 searches).

### 4. Backend env

```powershell
cd server
Copy-Item .env.local.example .env.local
# fill in values; generate SESSION_SECRET with: node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
npm install
npm run dev        # http://localhost:3000 (status page shows missing vars)
```

### 5. Mobile env

```powershell
cd mobile
Copy-Item .env.example .env
# EXPO_PUBLIC_API_URL: use your LAN IP for device testing, e.g. http://192.168.1.20:3000
# EXPO_PUBLIC_FACEBOOK_APP_ID: same App ID as above
# also put App ID + Client Token into mobile/app.json → plugins → react-native-fbsdk-next
npm install
npx expo prebuild --clean     # generates native projects incl. FB config
npx expo run:android          # or npx expo run:ios (dev build, required for FB login)
```

## Testing the social loop

1. Add a friend as Tester in your FB app roles.
2. Both of you install the dev build, log in with Facebook.
3. One plays a song; open the Friends tab on the other device — the song appears
   within ~5 seconds, showing playing/paused state.

## Notes & next steps

- Friend activity uses 5s polling; swap in Supabase Realtime subscriptions for instant updates.
- The user's Facebook token is stored server-side (`users.fb_token`) to resolve the
  friend list on demand; it expires (~60 days) and users just log in again.
- Before public release you need Facebook App Review for `user_friends`.
- YouTube audio playback via iframe embed is fine for demos but not a licensed music
  service — for production consider licensed catalogs (Spotify/Deezer SDKs).
