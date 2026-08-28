create table if not exists users (
  id uuid primary key default gen_random_uuid(),
  first_name text not null,
  last_name text not null,
  name text not null,
  code text unique not null,
  avatar_url text,
  created_at timestamptz not null default now(),
  unique (first_name, last_name)
);

create table if not exists now_playing (
  user_id uuid primary key references users (id) on delete cascade,
  video_id text,
  title text not null default '',
  channel text not null default '',
  thumbnail_url text not null default '',
  is_playing boolean not null default false,
  updated_at timestamptz not null default now()
);

create index if not exists now_playing_updated_idx on now_playing (updated_at desc);

-- Listening history: logs every song a user plays
CREATE TABLE IF NOT EXISTS listening_history (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  video_id text NOT NULL,
  title text NOT NULL DEFAULT '',
  channel text NOT NULL DEFAULT '',
  thumbnail_url text NOT NULL DEFAULT '',
  played_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_listening_history_user ON listening_history(user_id, played_at DESC);