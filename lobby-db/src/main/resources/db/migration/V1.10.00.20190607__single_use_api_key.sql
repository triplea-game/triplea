create table moderator_single_use_key
(
    id            serial primary key,
    lobby_user_id int            not null references lobby_user (id),
    date_created  timestamptz    not null default current_timestamp,
    date_used     timestamptz,
    api_key       varchar(128) not null unique
);

alter table moderator_single_use_key owner to lobby_user;

comment on table moderator_single_use_key is $$Stores single use api-keys used by moderators. The single-use
  key is provided to a moderator at which point they use it to do an initial authentication. If successful
  the backend generates a new, permanent key and invalidates the single use key. The single use key is
  only valid for a limited time and is valid for only one usage.$$;
comment on column moderator_single_use_key.id is 'Synthetic PK column';
comment on column moderator_single_use_key.lobby_user_id is
    'FK to lobby_user table, this is the moderator to whom the key is assigned';
comment on column moderator_single_use_key.date_created is 'Row creation timestamp, when the api key was created.';
comment on column moderator_single_use_key.date_used is
    'Records the date of when the API is used, at which point it is no longer considered value.';
comment on column moderator_single_use_key.api_key is 'Sha512 hashed API key';
