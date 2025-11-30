-- Fix the foreign key on settings table to allow cascade delete
alter table settings
drop constraint if exists settings_user_id_fkey;

alter table settings
add constraint settings_user_id_fkey
foreign key (user_id)
references auth.users(id)
on delete cascade;
