
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

