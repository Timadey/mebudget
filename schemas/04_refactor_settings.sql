-- 1. Drop existing settings table (Destructive action, assuming dev environment)
drop table if exists settings;

-- 2. Create new settings table
create table settings (
  user_id uuid references auth.users not null primary key,
  pin_hash text,
  pin_enabled boolean default false,
  budget_duration text default 'monthly',
  last_verified_at timestamptz,
  onboarding_completed boolean default false,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 3. Enable RLS
alter table settings enable row level security;

-- 4. Create Policies
create policy "Users can view their own settings"
  on settings for select
  using (auth.uid() = user_id);

create policy "Users can update their own settings"
  on settings for update
  using (auth.uid() = user_id);

create policy "Users can insert their own settings"
  on settings for insert
  with check (auth.uid() = user_id);

-- 5. Create Trigger to auto-create settings on user signup
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.settings (user_id)
  values (new.id);
  return new;
end;
$$ language plpgsql security definer;

-- Trigger on auth.users
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- 6. Backfill for existing users (Optional, but good for dev)
insert into settings (user_id)
select id from auth.users
on conflict (user_id) do nothing;
