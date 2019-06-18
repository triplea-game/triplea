alter table lobby_user
    add column super_mod boolean default false;
comment on column lobby_user.super_mod is
    $$Indicates if this user is an admin of moderators, has ability to
    add and remove moderator status from other users.$$;
