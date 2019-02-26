
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

