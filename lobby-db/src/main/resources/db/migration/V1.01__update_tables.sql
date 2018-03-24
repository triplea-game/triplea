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
comment on database ta_users is 'The Database of the TripleA Lobby';

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

