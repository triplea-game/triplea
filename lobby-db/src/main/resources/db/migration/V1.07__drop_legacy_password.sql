/* 'ta_users.bcrypt_password' column is now used as a replacement. */
alter table ta_users drop column password;
