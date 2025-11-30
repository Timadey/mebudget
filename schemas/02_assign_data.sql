-- RUN THIS AFTER REGISTERING THE USER 'adminuser@gmail.com'

do $$
declare
  target_user_id uuid;
begin
  -- Get the user ID
  select id into target_user_id from auth.users where email = 'adminuser@gmail.com';

  if target_user_id is null then
    raise exception 'User adminuser@gmail.com not found. Please register the user first.';
  end if;

  -- Update Categories
  update categories set user_id = target_user_id where user_id is null;

  -- Update Transactions
  update transactions set user_id = target_user_id where user_id is null;

  -- Update Investments
  update investments set user_id = target_user_id where user_id is null;

  -- Update Settings
  update settings set user_id = target_user_id where user_id is null;

  -- Now that data is assigned, we can enforce NOT NULL and Unique Constraints
  
  -- Categories
  alter table categories alter column user_id set not null;
  alter table categories alter column user_id set default auth.uid();

  -- Transactions
  alter table transactions alter column user_id set not null;
  alter table transactions alter column user_id set default auth.uid();

  -- Investments
  alter table investments alter column user_id set not null;
  alter table investments alter column user_id set default auth.uid();

  -- Settings
  alter table settings alter column user_id set not null;
  alter table settings alter column user_id set default auth.uid();
  
  -- Add unique constraint to settings
  alter table settings add constraint settings_user_id_key_key unique (user_id, key);

end $$;
