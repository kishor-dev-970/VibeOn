-- Migration for existing projects: switch users from Facebook auth to
-- name-based accounts with a unique 5-digit user code.
-- Run this ONCE in the Supabase SQL editor.

-- 1) Add new columns
alter table users add column if not exists first_name text;
alter table users add column if not exists last_name text;
alter table users add column if not exists code text;

-- 2) Backfill names from the existing single `name` column
update users set
  first_name = split_part(name, ' ', 1),
  last_name  = case when strpos(name, ' ') > 0 then substring(name from strpos(name, ' ') + 1) else '' end
where first_name is null or last_name is null;

-- 3) Backfill unique 5-digit codes
create or replace function assign_user_codes() returns void
language plpgsql as $$
declare
  r record;
  new_code text;
begin
  for r in select id from users where code is null loop
    loop
      new_code := (10000 + floor(random() * 90000))::int::text;
      exit when not exists (select 1 from users where code = new_code);
    end loop;
    update users set code = new_code where id = r.id;
  end loop;
end;
$$;
select assign_user_codes();
drop function assign_user_codes();

-- 4) Finalize constraints
alter table users alter column first_name set not null;
alter table users alter column last_name set not null;
alter table users alter column code set not null;
alter table users add constraint users_code_key unique (code);
alter table users add constraint users_name_key unique (first_name, last_name);

-- 5) Drop Facebook auth columns
alter table users drop column if exists fb_id;
alter table users drop column if exists fb_token;