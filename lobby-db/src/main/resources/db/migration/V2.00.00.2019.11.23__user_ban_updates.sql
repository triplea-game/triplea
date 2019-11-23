alter table banned_user drop constraint banned_user_ban_expiry_check;
create index banned_user_ip on banned_user(ip, system_id);
