-- Fix the column name in the trigger: budgetlimit -> budget_limit
create or replace function public.handle_new_user()
returns trigger as $$
begin
  -- 1. Create default settings
  insert into public.settings (user_id)
  values (new.id);

  -- 2. Create default category: Groceries
  insert into public.categories (user_id, name, type, budget_limit, icon)
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
