create table temp_password_request_history
(
    id           serial primary key,
    inetaddress  inet        not null,
    username     varchar(40) not null,
    date_created timestamptz not null default now()
);

alter table temp_password_request_history
    owner to lobby_user;

comment on table temp_password_request_history is
    $$Table that stores requests for temporary passwords for audit purposes. This will let us rate limit requests and
    prevent a single player from spamming email to many userse.$$;
comment on column temp_password_request_history.id is 'synthetic PK column';
comment on column temp_password_request_history.inetaddress is 'IP of the address making the temp password request.';
comment on column temp_password_request_history.username is 'The requested username for a temp password.';
comment on column temp_password_request_history.date_created is 'Timestamp of when the temp password request is made';

create index temp_password_request_history_inet on temp_password_request_history (inetaddress);
