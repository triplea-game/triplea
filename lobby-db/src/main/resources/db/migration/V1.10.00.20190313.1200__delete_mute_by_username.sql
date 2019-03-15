
-- mute only by network identifiers, drop mute by username
drop table muted_usernames;

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
