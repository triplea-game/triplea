create table moderator_api_key
(
    id             serial primary key,
    lobby_user_id  int            not null references lobby_user (id),
    date_created   timestamptz    not null default current_timestamp,
    date_last_used timestamptz,
    api_key        character(128) not null unique check (char_length(api_key) = 128)
);

-- alter table moderator_api_key owner to triplea_lobby;
alter table moderator_api_key
    owner to postgres;

comment on table moderator_api_key is 'Stores api-keys used by moderators to authenticate http server requests';
comment on column moderator_api_key.id is 'Synthetic PK column';
comment on column moderator_api_key.lobby_user_id is 'FK to lobby_user table, this is the moderator that initiated an action.';
comment on column moderator_api_key.date_created is 'Row creation timestamp, when the api key was created.';
comment on column moderator_api_key.date_last_used is 'Records most recent usage of the api_key';
comment on column moderator_api_key.api_key is 'Sha512 hashed API key';
