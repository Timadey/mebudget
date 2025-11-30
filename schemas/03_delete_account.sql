-- 1. Update Foreign Keys to Cascade Delete
-- This ensures that when a user is deleted, their data is also deleted.

-- Categories
alter table categories drop constraint if exists categories_user_id_fkey;
alter table categories add constraint categories_user_id_fkey 
  foreign key (user_id) references auth.users(id) on delete cascade;

-- Transactions
alter table transactions drop constraint if exists transactions_user_id_fkey;
alter table transactions add constraint transactions_user_id_fkey 
  foreign key (user_id) references auth.users(id) on delete cascade;

-- Investments
alter table investments drop constraint if exists investments_user_id_fkey;
alter table investments add constraint investments_user_id_fkey 
  foreign key (user_id) references auth.users(id) on delete cascade;

-- Settings
alter table settings drop constraint if exists settings_user_id_fkey;
alter table settings add constraint settings_user_id_fkey 
  foreign key (user_id) references auth.users(id) on delete cascade;


-- 2. Create RPC function to allow users to delete themselves
-- Note: Standard Supabase client cannot delete users directly. We need a Postgres function.

create or replace function delete_own_account()
returns void as $$
begin
  -- Delete the user from auth.users
  -- The cascading constraints above will handle the rest
  delete from auth.users where id = auth.uid();
end;
$$ language plpgsql security definer;

-- Grant execute permission to authenticated users
grant execute on function delete_own_account to authenticated;
