
alter table ta_users add column bcrypt_password character(60) check (char_length(bcrypt_password)=60);
alter table ta_users alter column password drop not null;
update ta_users set password=null, bcrypt_password=password where password like '$2a$%';
alter table ta_users alter column password type character(34);
alter table ta_users add constraint ta_users_password_check check (char_length(password)=34);
alter table ta_users add constraint ta_users_check check (password IS NOT NULL OR bcrypt_password IS NOT NULL);

-- Comments
comment on column ta_users.password is 'The legacy MD5Crypt hash of the password. Is going to replaced by the content bcrypt_password in the next incompatible lobby release. The length of the hash must always be 34 chars. Either password or bcrypt_password must be not null.';
comment on column ta_users.bcrypt_password is 'The BCrypt-Hashed password of the user, should be the same as the md5 password but in another form. The length of the hash must always be 60 chars. Either password or bcrypt_password must be not null.';
comment on constraint ta_users_password_check on ta_users is 'This check constraint ensures the legacy password hash has the right char length of 34 chars';
comment on constraint ta_users_bcrypt_password_check on ta_users is 'This check constraint ensures the legacy password hash has the right char length of 60 chars';
comment on constraint ta_users_check on ta_users is 'This check constraint ensures either password or bcrypt_password is not null.';

