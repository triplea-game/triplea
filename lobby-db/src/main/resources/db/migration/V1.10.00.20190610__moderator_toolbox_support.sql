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
