-- 1. Add user_id to categories (Nullable initially to preserve data)
alter table categories add column user_id uuid references auth.users;
create index idx_categories_user_id on categories(user_id);

-- 2. Add user_id to transactions
alter table transactions add column user_id uuid references auth.users;
create index idx_transactions_user_id on transactions(user_id);

-- 3. Add user_id to investments
alter table investments add column user_id uuid references auth.users;
create index idx_investments_user_id on investments(user_id);

-- 4. Update settings table
alter table settings add column user_id uuid references auth.users;

-- Drop old unique constraint
alter table settings drop constraint if exists settings_key_key;

-- Add new unique constraint scoped to user (Note: this might fail if multiple rows have same key and null user_id, but we assume single user initially)
-- We will defer the unique constraint until after migration or handle duplicates.
-- For now, let's just add the column. We'll fix constraints in step 02.

-- 5. Enable RLS
alter table categories enable row level security;
alter table transactions enable row level security;
alter table investments enable row level security;
alter table settings enable row level security;

-- 6. Create Policies
-- Note: These policies will hide rows where user_id is NULL from standard users.
-- This is desired behavior until the data is claimed.

-- Categories
create policy "Users can view their own categories" on categories for select using (auth.uid() = user_id);
create policy "Users can insert their own categories" on categories for insert with check (auth.uid() = user_id);
create policy "Users can update their own categories" on categories for update using (auth.uid() = user_id);
create policy "Users can delete their own categories" on categories for delete using (auth.uid() = user_id);

-- Transactions
create policy "Users can view their own transactions" on transactions for select using (auth.uid() = user_id);
create policy "Users can insert their own transactions" on transactions for insert with check (auth.uid() = user_id);
create policy "Users can update their own transactions" on transactions for update using (auth.uid() = user_id);
create policy "Users can delete their own transactions" on transactions for delete using (auth.uid() = user_id);

-- Investments
create policy "Users can view their own investments" on investments for select using (auth.uid() = user_id);
create policy "Users can insert their own investments" on investments for insert with check (auth.uid() = user_id);
create policy "Users can update their own investments" on investments for update using (auth.uid() = user_id);
create policy "Users can delete their own investments" on investments for delete using (auth.uid() = user_id);

-- Settings
create policy "Users can view their own settings" on settings for select using (auth.uid() = user_id);
create policy "Users can insert their own settings" on settings for insert with check (auth.uid() = user_id);
create policy "Users can update their own settings" on settings for update using (auth.uid() = user_id);
create policy "Users can delete their own settings" on settings for delete using (auth.uid() = user_id);
