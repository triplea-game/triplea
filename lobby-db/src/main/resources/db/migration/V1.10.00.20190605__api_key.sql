create table moderator_api_key
(
    id                     serial primary key,
    public_id              varchar(36)    not null unique,
    lobby_user_id          int            not null references lobby_user (id),
    date_created           timestamptz    not null default current_timestamp,
    date_last_used         timestamptz,
    last_used_host_address varchar(64),
    api_key                character(128) not null unique
);

alter table moderator_api_key owner to lobby_user;

comment on table moderator_api_key is
    'Stores api-keys used by moderators to authenticate http server requests';
comment on column moderator_api_key.id is
    'Synthetic PK column';
comment on column moderator_api_key.public_id is
    'A unique identifier for the API key that can be used to reference an API key.';
comment on column moderator_api_key.lobby_user_id is
    'FK to lobby_user table, this is the moderator that initiated an action.';
comment on column moderator_api_key.date_created is
    'Row creation timestamp, when the api key was created.';
comment on column moderator_api_key.date_last_used is
    'Records most recent usage of the api_key';
comment on column moderator_api_key.api_key is
    'Sha512 hashed API key';
comment on column moderator_api_key.last_used_host_address is
    'Host address of the machine to last use a given API key';
