-- VibeOn Android backend schema (PostgreSQL for Supabase)
-- Run this once in the Supabase SQL editor, or via `supabase db push`.

-- ---------------------------------------------------------------------------
-- profiles: one row per signed-up user, mirrors auth.users
-- ---------------------------------------------------------------------------
create table if not exists profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  first_name text not null default '',
  last_name text not null default '',
  name text not null default '',
  email text not null default '',
  code text not null default '',
  avatar_url text,
  last_active timestamptz,
  created_at timestamptz not null default now()
);

-- Unique 6-char invite codes (A-Z, 2-9, no 0/O/1/I)
create unique index if not exists profiles_code_uniq on profiles (code);

-- ---------------------------------------------------------------------------
-- now_playing: current live status per user (upserted on user_id)
-- ---------------------------------------------------------------------------
create table if not exists now_playing (
  user_id uuid primary key references profiles (id) on delete cascade,
  is_playing boolean not null default false,
  song_title text,
  artist_name text,
  artwork_url text,
  video_id text,
  updated_at timestamptz not null default now()
);

create index if not exists now_playing_updated_idx on now_playing (updated_at desc);

-- ---------------------------------------------------------------------------
-- play_events: one row per track play, drives friend stats
-- ---------------------------------------------------------------------------
create table if not exists play_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references profiles (id) on delete cascade,
  song_title text not null,
  artist_name text,
  artwork_url text,
  video_id text,
  duration_seconds bigint not null default 210,
  played_at timestamptz not null default now()
);

create index if not exists play_events_user_idx on play_events (user_id, played_at desc);

-- ---------------------------------------------------------------------------
-- Row Level Security: anyone signed in can view the social layer (everyone is
-- a friend on VibeOn), but can only write their own rows.
-- ---------------------------------------------------------------------------
alter table profiles enable row level security;
alter table now_playing enable row level security;
alter table play_events enable row level security;

drop policy if exists "profiles are readable by everyone" on profiles;
create policy "profiles are readable by everyone"
  on profiles for select
  to authenticated
  using (true);

drop policy if exists "users can insert their own profile" on profiles;
create policy "users can insert their own profile"
  on profiles for insert
  to authenticated
  with check (auth.uid() = id);

drop policy if exists "users can update their own profile" on profiles;
create policy "users can update their own profile"
  on profiles for update
  to authenticated
  using (auth.uid() = id)
  with check (auth.uid() = id);

drop policy if exists "now_playing is readable by everyone" on now_playing;
create policy "now_playing is readable by everyone"
  on now_playing for select
  to authenticated
  using (true);

drop policy if exists "users can update their own now_playing" on now_playing;
create policy "users can update their own now_playing"
  on now_playing for insert
  to authenticated
  with check (auth.uid() = user_id);

drop policy if exists "users can upsert their own now_playing" on now_playing;
create policy "users can upsert their own now_playing"
  on now_playing for update
  to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "users can delete their own now_playing" on now_playing;
create policy "users can delete their own now_playing"
  on now_playing for delete
  to authenticated
  using (auth.uid() = user_id);

drop policy if exists "play_events is readable by everyone" on play_events;
create policy "play_events is readable by everyone"
  on play_events for select
  to authenticated
  using (true);

drop policy if exists "users can insert their own play_events" on play_events;
create policy "users can insert their own play_events"
  on play_events for insert
  to authenticated
  with check (auth.uid() = user_id);

-- ---------------------------------------------------------------------------
-- Trigger: keep profiles.last_active fresh on every now_playing upsert
-- ---------------------------------------------------------------------------
create or replace function touch_profile_last_active() returns trigger
language plpgsql
as $$
begin
  update profiles set last_active = now() where id = new.user_id;
  return new;
end;
$$;

drop trigger if exists trg_touch_profile_last_active on now_playing;
create trigger trg_touch_profile_last_active
  after insert or update on now_playing
  for each row execute function touch_profile_last_active();

-- ---------------------------------------------------------------------------
-- Trigger: auto-create a profile from auth.users on sign-up
-- ---------------------------------------------------------------------------
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public
as $$
begin
  insert into public.profiles (id, first_name, last_name, name, email, code)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'first_name', ''),
    coalesce(new.raw_user_meta_data ->> 'last_name', ''),
    coalesce(new.raw_user_meta_data ->> 'name', ''),
    coalesce(new.email, ''),
    coalesce(new.raw_user_meta_data ->> 'code', '')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();