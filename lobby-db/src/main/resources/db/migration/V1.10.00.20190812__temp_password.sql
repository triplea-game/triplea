create table temp_password_request
(
    id               serial primary key,
    lobby_user_id    int         not null references lobby_user (id),
    temp_password    varchar(60) not null,
    date_created     timestamptz not null default now(),
    date_invalidated timestamptz
);

alter table temp_password_request
    owner to lobby_user;

comment on table temp_password_request is
    $$Table that stores temporary passwords issued to players. They are intended to be single use.$$;
comment on column temp_password_request.id is 'synthetic PK column';
comment on column temp_password_request.lobby_user_id is 'FK to lobby_user table.';
comment on column temp_password_request.temp_password is 'Temp password value created for user.';
comment on column temp_password_request.date_created is 'Timestamp of when the ban temporary password was created.';
comment on column temp_password_request.date_invalidated is
    $$Timestamp of when the temporary password is either used or marked invalid.
    A temp password can be marked as invalid if multiple are issued.$$
