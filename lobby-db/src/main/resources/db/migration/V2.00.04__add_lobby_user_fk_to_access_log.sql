alter table access_log drop column registered;
alter table access_log add column lobby_user_id int references lobby_user(id);
