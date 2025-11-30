-- Update the handle_new_user function to also create default categories
create or replace function public.handle_new_user()
returns trigger as $$
begin
  -- 1. Create default settings
  insert into public.settings (user_id)
  values (new.id);

  -- 2. Create default category: Groceries
  insert into public.categories (user_id, name, type, budgetlimit, icon)
  values (
    new.id, 
    'Groceries', 
    'Expense', 
    50000, 
    '🥦'
  );

  return new;
end;
$$ language plpgsql security definer;
