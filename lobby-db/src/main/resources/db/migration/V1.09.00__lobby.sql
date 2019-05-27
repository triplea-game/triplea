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

drop table muted_ips;

delete from muted_macs where mute_till <= now();
alter table muted_macs
    alter column mac type character(28),
	add constraint muted_macs_mac_check check (char_length(mac)=28),
    alter column mute_till type timestamptz,
	add constraint muted_macs_mute_till_check check (mute_till is null or mute_till > now());

delete from muted_usernames where mute_till <= now();
alter table muted_usernames
    alter column mute_till type timestamptz,
	add constraint muted_usernames_mute_till_check check (mute_till is null or mute_till > now());


-- Comments
comment on database lobby is 'The Database of the TripleA Lobby';

comment on table ta_users is 'The table storing all the information about TripleA users.';
comment on column ta_users.username is 'Defines the in-game username of everyone. The primary key constraint should probably be moved to an id column, preferably using pseudo_encrypt(nextval(''something'')) as default value.';
comment on column ta_users.email is 'Email storage of every user. Large size to match the maximum email length. More information here: https://stackoverflow.com/a/574698 Should be made unique in the future.';
comment on column ta_users.joined is 'The timestamp of the creation of the account. Is created automatically by the DB, should never be modified, e.g. considered immutable';
comment on column ta_users.lastlogin is 'The timestamp of the last successful login, is altered by the game engine.';
comment on column ta_users.admin is 'The role of the user, controls privileges. If true the user is able to ban and mute other people. Might be changed to another type once more "ranks" become neccessary. Defaults to false.';

comment on table bad_words is 'A table representing a blacklist of words, used to filter chat messages and prohibit usernames in the lobby.';
comment on column bad_words.word is 'This column stores all the banned words. The primary key constraint might be replaced with a unique constraint.';

comment on table banned_macs is 'A Table storing banned mac adresses.';
comment on column banned_macs.mac is 'An MD5Crypted/hashed MAC address. This hashing method should be replaced by SHA-2 or SHA-3 for performance and collision reasons (salting is not needed).  The hash must be of length 28.';
comment on column banned_macs.ban_till is 'A timestamp indicating how long the ban should be active, if NULL the ban is forever.';
comment on constraint banned_macs_mac_check on banned_macs is 'Ensures the hashed mac always has the right length.';
comment on constraint banned_macs_ban_till_check on banned_macs is 'Ensures no storage is being wasted by banning someone backdated to the past.';

comment on table banned_usernames is 'A Table storing banned usernames.';
comment on column banned_usernames.username is 'The username of the banned user. Actually no direct reference to the ta_users.username, the engine allows to define prohibited usernames, should probably be avoided, and an SQL reference created instead.';
comment on column banned_usernames.ban_till is 'A timestamp indicating how long the ban should be active, if NULL the ban is forever.';
comment on constraint banned_usernames_ban_till_check on banned_usernames is 'Ensures no storage is being wasted by banning someone backdated to the past.';

comment on table muted_macs is 'A Table storing muted mac adresses.';
comment on column muted_macs.mac is 'An MD5Crypted/hashed MAC address. This hashing method should be replaced by SHA-2 or SHA-3 for performance and collision reasons (salting is not needed). The hash must be of length 28.';
comment on column muted_macs.mute_till is 'A timestamp indicating how long the mute should be active, if NULL the mute is forever.';
comment on constraint muted_macs_mac_check on muted_macs is 'Ensures the hashed mac always has the right length.';
comment on constraint muted_macs_mute_till_check on muted_macs is 'Ensures no storage is being wasted by muting someone backdated to the past.';

comment on table muted_usernames is 'A Table storing muted usernames.';
comment on column muted_usernames.username is 'The username of the muted user. Actually no direct reference to the ta_users.username for whatever reason using an SQL reference would make the mute change along with the username of a user.';
comment on column muted_usernames.mute_till is 'A timestamp indicating how long the mute should be active, if NULL the mute is forever.';
comment on constraint muted_usernames_mute_till_check on muted_usernames is 'Ensures no storage is being wasted by muting someone backdated to the past.';

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

-- audit_mutes

alter table muted_macs
  add column mod_username varchar(40) not null default '__unknown__',
  add column mod_ip inet not null default '0.0.0.0'::inet,
  add column mod_mac char(28) not null default '$1$MH$WarCz.YHukJf1YVqmMBoS0',
  add constraint muted_macs_mod_mac_check check (char_length(mod_mac)=28);

alter table muted_macs
  alter column mod_username drop default,
  alter column mod_ip drop default,
  alter column mod_mac drop default;

comment on column muted_macs.mod_username is 'The username of the moderator that executed the mute.';
comment on column muted_macs.mod_ip is 'The IP address of the moderator that executed the mute.';
comment on column muted_macs.mod_mac is 'The hashed MAC address of the moderator that executed the mute.';

alter table muted_usernames
  add column mod_username varchar(40) not null default '__unknown__',
  add column mod_ip inet not null default '0.0.0.0'::inet,
  add column mod_mac char(28) not null default '$1$MH$WarCz.YHukJf1YVqmMBoS0',
  add constraint muted_usernames_mod_mac_check check (char_length(mod_mac)=28);

alter table muted_usernames
  alter column mod_username drop default,
  alter column mod_ip drop default,
  alter column mod_mac drop default;

comment on column muted_usernames.mod_username is 'The username of the moderator that executed the mute.';
comment on column muted_usernames.mod_ip is 'The IP address of the moderator that executed the mute.';
comment on column muted_usernames.mod_mac is 'The hashed MAC address of the moderator that executed the mute.';

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

alter table muted_macs
  add column username varchar(40) not null default '__unknown__',
  add column ip inet not null default '0.0.0.0'::inet;

alter table muted_macs
  alter column username drop default,
  alter column ip drop default;

comment on column muted_macs.username is 'The username of the muted user.';
comment on column muted_macs.ip is 'The IP address of the muted user.';

alter table muted_usernames
  add column ip inet not null default '0.0.0.0'::inet,
  add column mac char(28) not null default '$1$MH$WarCz.YHukJf1YVqmMBoS0',
  add constraint muted_usernames_mac_check check (char_length(mac)=28);

alter table muted_usernames
  alter column ip drop default,
  alter column mac drop default;

comment on column muted_usernames.ip is 'The IP address of the muted user.';
comment on column muted_usernames.mac is 'The hashed MAC address of the muted user.';

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

alter table access_log owner to triplea_lobby;

