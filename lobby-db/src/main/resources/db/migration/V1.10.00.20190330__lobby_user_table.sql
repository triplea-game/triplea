create table lobby_user
(
    id              serial primary key,
    username        character varying(40) not null unique,
    password        character varying(60),
    email           character varying(40) not null,
    date_created    timestamptz           not null default current_timestamp,
    last_login      timestamptz,
    admin           boolean               not null default false,
    bcrypt_password character(60) check (char_length(bcrypt_password) = 60)
);

-- alter table user owner to triplea_lobby;
alter table lobby_user
    owner to postgres;
alter table lobby_user
    add constraint lobby_user_pass_check check (password IS NOT NULL OR bcrypt_password IS NOT NULL);


comment on table lobby_user is 'The table storing all the information about Lobby TripleA users.';
comment on column lobby_user.id is 'Synthetic PK column';
comment on column lobby_user.username is 'Defines the in-game username of everyone.';
comment on column lobby_user.password is 'The legacy MD5Crypt hash of the password. The length of the hash must always be 34 chars. Either password or bcrypt_password must be not null.';
comment on column lobby_user.email is 'Email storage of every user. Large size to match the maximum email length. More information here: https://stackoverflow.com/a/574698.';
comment on column lobby_user.date_created is 'The timestamp of the creation of the account.';
comment on column lobby_user.last_login is 'The timestamp of the last successful login.';
comment on column lobby_user.admin is 'The role of the user, controls privileges. If true the user is able to ban and mute other people.';
comment on column lobby_user.bcrypt_password is 'The BCrypt-Hashed password of the user, should be the same as the md5 password but in another form. The length of the hash must always be 60 chars. Either password or bcrypt_password must be not null.';


insert into lobby_user (username, password, email, date_created, last_login, admin)
select username, password, email, joined, lastLogin, admin
from ta_users;

drop table ta_users;

create table moderator_action_history
(
    id            serial primary key,
    lobby_user_id int         not null references lobby_user (id),
    date_created  timestamptz not null default current_timestamp,
    action_name   varchar(64) not null,
    action_target varchar(40) not null
);

-- alter table moderator_action_history owner to triplea_lobby;
alter table moderator_action_history
    owner to postgres;

comment on table moderator_action_history is 'Table storing an audit history of actions taken by moderators';
comment on column moderator_action_history.id is 'Table storing an audit history of actions taken by moderators';

comment on column moderator_action_history.lobby_user_id is 'FK to lobby_user table, this is the moderator that initiated an action.';
comment on column moderator_action_history.date_created is 'Row creation timestamp, when the action was taken.';
comment on column moderator_action_history.action_name is 'Specifier of what action the moderator took, eg: ban|mute';
comment on column moderator_action_history.action_target is 'The target of the action, eg: banned player name, banned mac address';
