comment on database lobby_db is 'The Database of the TripleA Lobby';

create table bad_word (
    word character varying(40) not null primary key,
    date_created  timestamp without time zone not null default now()
);
alter table bad_word owner to lobby_user;
comment on table bad_word is 'A table representing a blacklist of words';


create table banned_ips (
    ip character varying(40) not null primary key,
    ban_till timestamp without time zone
);
alter table banned_ips owner to lobby_user;

create table banned_macs (
    mac character varying(40) not null primary key,
    ban_till timestamp without time zone
);
alter table banned_macs owner to lobby_user;

create table banned_usernames (
    username character varying(40) not null primary key,
    ban_till timestamp without time zone
);
alter table banned_usernames owner to lobby_user;

create table ta_users (
    username character varying(40) not null primary key,
    password character varying(60) not null,
    email character varying(40) not null,
    joined timestamp without time zone not null,
    lastlogin timestamp without time zone not null,
    admin integer not null
);
alter table ta_users owner to lobby_user;
-- update_tables
alter table ta_users
    alter column email type varchar(254),
    alter column joined type timestamptz,
    alter column joined set default current_timestamp,
    alter column lastlogin type timestamptz,
    alter column lastlogin set default current_timestamp,
    alter column admin type boolean using case when admin=0 then false else true end,
    alter column admin set default false;

drop table banned_ips;

delete from banned_macs where ban_till <= now();
alter table banned_macs
    alter column mac type character(28),
	add constraint banned_macs_mac_check check (char_length(mac)=28),
    alter column ban_till type timestamptz,
	add constraint banned_macs_ban_till_check check (ban_till is null or ban_till > now());

delete from banned_usernames where ban_till <= now();
alter table banned_usernames
    alter column ban_till type timestamptz,
	add constraint banned_usernames_ban_till_check check (ban_till is null or ban_till > now());


-- Comments

comment on table ta_users is 'The table storing all the information about TripleA users.';
comment on column ta_users.username is 'Defines the in-game username of everyone. The primary key constraint should probably be moved to an id column, preferably using pseudo_encrypt(nextval(''something'')) as default value.';
comment on column ta_users.email is 'Email storage of every user. Large size to match the maximum email length. More information here: https://stackoverflow.com/a/574698 Should be made unique in the future.';
comment on column ta_users.joined is 'The timestamp of the creation of the account. Is created automatically by the DB, should never be modified, e.g. considered immutable';
comment on column ta_users.lastlogin is 'The timestamp of the last successful login, is altered by the game engine.';
comment on column ta_users.admin is 'The role of the user, controls privileges. If true the user is able to ban and mute other people. Might be changed to another type once more "ranks" become neccessary. Defaults to false.';

comment on table banned_macs is 'A Table storing banned mac adresses.';
comment on column banned_macs.mac is 'An MD5Crypted/hashed MAC address. This hashing method should be replaced by SHA-2 or SHA-3 for performance and collision reasons (salting is not needed).  The hash must be of length 28.';
comment on column banned_macs.ban_till is 'A timestamp indicating how long the ban should be active, if NULL the ban is forever.';
comment on constraint banned_macs_mac_check on banned_macs is 'Ensures the hashed mac always has the right length.';
comment on constraint banned_macs_ban_till_check on banned_macs is 'Ensures no storage is being wasted by banning someone backdated to the past.';

comment on table banned_usernames is 'A Table storing banned usernames.';
comment on column banned_usernames.username is 'The username of the banned user. Actually no direct reference to the ta_users.username, the engine allows to define prohibited usernames, should probably be avoided, and an SQL reference created instead.';
comment on column banned_usernames.ban_till is 'A timestamp indicating how long the ban should be active, if NULL the ban is forever.';
comment on constraint banned_usernames_ban_till_check on banned_usernames is 'Ensures no storage is being wasted by banning someone backdated to the past.';

-- add_bcrypt_password_column

alter table ta_users add column bcrypt_password character(60) check (char_length(bcrypt_password)=60);
alter table ta_users alter column password drop not null;
update ta_users set password=null, bcrypt_password=password where password like '$2a$%';
alter table ta_users alter column password type character(34);
alter table ta_users add constraint ta_users_password_check check (char_length(password)=34);
alter table ta_users add constraint ta_users_check check (password IS NOT NULL OR bcrypt_password IS NOT NULL);

-- Comments
comment on column ta_users.password is 'The legacy MD5Crypt hash of the password. Is going to replaced by the content bcrypt_password in the next incompatible lobby release. The length of the hash must always be 34 chars. Either password or bcrypt_password must be not null.';
comment on column ta_users.bcrypt_password is 'The BCrypt-Hashed password of the user, should be the same as the md5 password but in another form. The length of the hash must always be 60 chars. Either password or bcrypt_password must be not null.';
comment on constraint ta_users_password_check on ta_users is 'This check constraint ensures the legacy password hash has the right char length of 34 chars';
comment on constraint ta_users_bcrypt_password_check on ta_users is 'This check constraint ensures the legacy password hash has the right char length of 60 chars';
comment on constraint ta_users_check on ta_users is 'This check constraint ensures either password or bcrypt_password is not null.';

-- audit_bans

alter table banned_macs
  add column mod_username varchar(40) not null default '__unknown__',
  add column mod_ip inet not null default '0.0.0.0'::inet,
  add column mod_mac char(28) not null default '$1$MH$WarCz.YHukJf1YVqmMBoS0',
  add constraint banned_macs_mod_mac_check check (char_length(mod_mac)=28);

alter table banned_macs
  alter column mod_username drop default,
  alter column mod_ip drop default,
  alter column mod_mac drop default;

comment on column banned_macs.mod_username is 'The username of the moderator that executed the ban.';
comment on column banned_macs.mod_ip is 'The IP address of the moderator that executed the ban.';
comment on column banned_macs.mod_mac is 'The hashed MAC address of the moderator that executed the ban.';

alter table banned_usernames
  add column mod_username varchar(40) not null default '__unknown__',
  add column mod_ip inet not null default '0.0.0.0'::inet,
  add column mod_mac char(28) not null default '$1$MH$WarCz.YHukJf1YVqmMBoS0',
  add constraint banned_usernames_mod_mac_check check (char_length(mod_mac)=28);

alter table banned_usernames
  alter column mod_username drop default,
  alter column mod_ip drop default,
  alter column mod_mac drop default;

comment on column banned_usernames.mod_username is 'The username of the moderator that executed the ban.';
comment on column banned_usernames.mod_ip is 'The IP address of the moderator that executed the ban.';
comment on column banned_usernames.mod_mac is 'The hashed MAC address of the moderator that executed the ban.';

-- add_all_user_info_to_bans_and_mutes

alter table banned_macs
  add column username varchar(40) not null default '__unknown__',
  add column ip inet not null default '0.0.0.0'::inet;

alter table banned_macs
  alter column username drop default,
  alter column ip drop default;

comment on column banned_macs.username is 'The username of the banned user.';
comment on column banned_macs.ip is 'The IP address of the banned user.';

alter table banned_usernames
  add column ip inet not null default '0.0.0.0'::inet,
  add column mac char(28) not null default '$1$MH$WarCz.YHukJf1YVqmMBoS0',
  add constraint banned_usernames_mac_check check (char_length(mac)=28);

alter table banned_usernames
  alter column ip drop default,
  alter column mac drop default;

comment on column banned_usernames.ip is 'The IP address of the banned user.';
comment on column banned_usernames.mac is 'The hashed MAC address of the banned user.';

-- add_access_log

create table access_log (
  access_time timestamptz not null default now(),
  username varchar(40) not null,
  ip inet not null,
  mac char(28) not null check (char_length(mac)=28),
  registered boolean not null
);

comment on column access_log.access_time is 'The date and time the lobby was accessed.';
comment on column access_log.username is 'The name of the user accessing the lobby.';
comment on column access_log.ip is 'The IP address of the user accessing the lobby.';
comment on column access_log.mac is 'The hashed MAC address of the user accessing the lobby.';
comment on column access_log.registered is 'True if the user was registered when accessing the lobby; otherwise false if the user was anonymous';

alter table access_log owner to lobby_user;


-- 'banned_usernames' will now keep track of username blacklist with no expiration
alter table banned_usernames
 drop column ip;

alter table banned_usernames
  drop column mac;

alter table banned_usernames
  drop column ban_till;


-- mod name identifies a mod, drop unnecessary network identifier tracking for moderator
alter table banned_usernames
  drop column mod_ip;

alter table banned_usernames
  drop column mod_mac;

create table lobby_user
(
    id              serial primary key,
    username        character varying(40) not null unique,
    password        character varying(60),
    email           character varying(40) not null,
    check (email <> ''),
    date_created    timestamptz           not null default current_timestamp,
    last_login      timestamptz,
    role            character varying(16) not null default 'PLAYER' check (role in ('PLAYER', 'MODERATOR', 'ADMIN')),
    bcrypt_password character(60) check (char_length(bcrypt_password) = 60)
);

alter table lobby_user
    owner to lobby_user;
alter table lobby_user
    add constraint lobby_user_pass_check check (password IS NOT NULL OR bcrypt_password IS NOT NULL);


comment on table lobby_user is 'The table storing all the information about Lobby TripleA users.';
comment on column lobby_user.id is 'Synthetic PK column';
comment on column lobby_user.username is 'Defines the in-game username of everyone.';
comment on column lobby_user.password is 'The legacy MD5Crypt hash of the password. The length of the hash must always be 34 chars. Either password or bcrypt_password must be not null.';
comment on column lobby_user.email is 'Email storage of every user. Large size to match the maximum email length. More information here: https://stackoverflow.com/a/574698.';
comment on column lobby_user.date_created is 'The timestamp of the creation of the account.';
comment on column lobby_user.last_login is 'The timestamp of the last successful login.';
comment on column lobby_user.role is
    $$The role of the user, controls privileges. If moderator the user is able to ban and mute other people.
     Admin is able to add/remove other moderators.$$;
comment on column lobby_user.bcrypt_password is 'The BCrypt-Hashed password of the user, should be the same as the md5 password but in another form. The length of the hash must always be 60 chars. Either password or bcrypt_password must be not null.';


create table moderator_action_history
(
    id            serial primary key,
    lobby_user_id int         not null references lobby_user (id),
    date_created  timestamptz not null default current_timestamp,
    action_name   varchar(64) not null,
    action_target varchar(40) not null
);

alter table moderator_action_history
    owner to lobby_user;

comment on table moderator_action_history is 'Table storing an audit history of actions taken by moderators';
comment on column moderator_action_history.id is 'Table storing an audit history of actions taken by moderators';

comment on column moderator_action_history.lobby_user_id is 'FK to lobby_user table, this is the moderator that initiated an action.';
comment on column moderator_action_history.date_created is 'Row creation timestamp, when the action was taken.';
comment on column moderator_action_history.action_name is 'Specifier of what action the moderator took, eg: ban|mute';
comment on column moderator_action_history.action_target is 'The target of the action, eg: banned player name, banned mac address';


create table error_report_history(
  id       serial primary key,
  user_ip  character varying(30) not null,
  date_created timestamp not null default now()
);

alter table error_report_history owner to lobby_user;

comment on table error_report_history is 'Table that stores timestamps by user IP address of when error reports were created. Used to do rate limiting.';
comment on column error_report_history.id is 'Synthetic PK column';
comment on column error_report_history.user_ip is 'IP address of a user that has submitted an error report';
comment on column error_report_history.date_created is 'Timestamp when error report was created in DB';
alter table banned_usernames
    add column date_created timestamptz not null default now();
comment on column banned_usernames.date_created is
    'Timestamp of when the banned username is added';
alter table banned_usernames
    drop column mod_username;

create table banned_user
(
    id           serial primary key,
    public_id    varchar(36)   not null unique,
    username     varchar(40)   not null,
    hashed_mac   character(28) not null check (char_length(hashed_mac) = 28),
    ip           inet          not null,
    ban_expiry   timestamptz   not null check (ban_expiry > now()),
    date_created timestamptz   not null default now()
);

alter table banned_user owner to lobby_user;

-- migrate existing data to the new table name
insert into banned_user(public_id, username, hashed_mac, ip, ban_expiry)
select random()::varchar, username, mac, ip, coalesce(ban_till, now() + interval '30 days')
from banned_macs
where ban_till > now();

drop table banned_macs;

comment on table banned_user is
    $$Table that records player bans, when players join lobby we check their IP address and hashed mac
          against this table. If there there is an IP or mac match, then the user is not allowed to join.$$;
comment on column banned_user.id is 'synthetic PK column';
comment on column banned_user.public_id is
    $$A value that publicly identifiers the ban. When a player is rejected from joining lobby we can
        show them this ID value. If the player wants to dispute the ban, they can give us the public id
        and we would be able to remove the ban.$$;
comment on column banned_user.username is
    'The name of the player at the time of banning.';
comment on column banned_user.hashed_mac is
    $$Mac is hashed when sent to TripleA, this is the hashed mac value that software received.
        One note, the mac sent by TripleA is software generated and can be altered by a custom
        compiled version of TripleA$$;
comment on column banned_user.ip is 'IP address of the user at the time of banning.';
comment on column banned_user.ban_expiry is 'Timestamp of when the player ban expires';
comment on column banned_user.date_created is 'Timestamp of when the ban was created.';
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
    A temp password can be marked as invalid if multiple are issued.$$;

create table temp_password_request_history
(
    id           serial primary key,
    inetaddress  inet        not null,
    username     varchar(40) not null,
    date_created timestamptz not null default now()
);

alter table temp_password_request_history
    owner to lobby_user;

comment on table temp_password_request_history is
    $$Table that stores requests for temporary passwords for audit purposes. This will let us rate limit requests and
    prevent a single player from spamming email to many userse.$$;
comment on column temp_password_request_history.id is 'synthetic PK column';
comment on column temp_password_request_history.inetaddress is 'IP of the address making the temp password request.';
comment on column temp_password_request_history.username is 'The requested username for a temp password.';
comment on column temp_password_request_history.date_created is 'Timestamp of when the temp password request is made';

create index temp_password_request_history_inet on temp_password_request_history (inetaddress);
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
