create table bad_words (
    word character varying(40) not null primary key
);
alter table bad_words owner to postgres;

create table banned_ips (
    ip character varying(40) not null primary key,
    ban_till timestamp without time zone
);
alter table banned_ips owner to postgres;

create table banned_macs (
    mac character varying(40) not null primary key,
    ban_till timestamp without time zone
);
alter table banned_macs owner to postgres;

create table banned_usernames (
    username character varying(40) not null primary key,
    ban_till timestamp without time zone
);
alter table banned_usernames owner to postgres;

create table muted_ips (
    ip character varying(40) not null primary key,
    mute_till timestamp without time zone
);
alter table muted_ips owner to postgres;

create table muted_macs (
    mac character varying(40) not null primary key,
    mute_till timestamp without time zone
);
alter table muted_macs owner to postgres;

create table muted_usernames (
    username character varying(40) not null primary key,
    mute_till timestamp without time zone
);
alter table muted_usernames owner to postgres;

create table ta_users (
    username character varying(40) not null primary key,
    password character varying(60) not null,
    email character varying(40) not null,
    joined timestamp without time zone not null,
    lastlogin timestamp without time zone not null,
    admin integer not null
);
alter table ta_users owner to postgres;
