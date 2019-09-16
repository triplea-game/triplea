create table api_key
(
    id            serial primary key,
    lobby_user_id integer references lobby_user (id),
    key           character varying(256) not null unique,
    date_created  timestamptz            not null default now()
);

alter table api_key
    owner to lobby_user;

comment on table api_key is
    $$Table that stores api keys of users that have logged into the lobby. Denormalized to reflect time
        of login and to support anonymous user logins.$$;
