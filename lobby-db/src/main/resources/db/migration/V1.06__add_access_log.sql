
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

alter table access_log owner to postgres;

