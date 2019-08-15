create table user_temp_password
(
    id               serial primary key,
    lobby_user_id    int         not null references lobby_user (id),
    temp_password    varchar(60) not null,
    date_created     timestamptz not null default now(),
    date_invalidated timestamptz
);

alter table user_temp_password
    owner to lobby_user;

comment on table user_temp_password is
    $$Table that stores temporary passwords issued to players. They are intended to be single use.$$;
comment on column user_temp_password.id is 'synthetic PK column';
comment on column user_temp_password.lobby_user_id is 'FK to lobby_user table.';
comment on column user_temp_password.temp_password is 'Temp password value created for user.';
comment on column user_temp_password.date_created is 'Timestamp of when the ban temporary password was created.';
comment on column user_temp_password.date_invalidated is
    $$Timestamp of when the temporary password is either used or marked invalid.
    A temp password can be marked as invalid if multiple are issued.$$
