alter table lobby_user drop column password;
alter table lobby_user alter column bcrypt_password set not null;
