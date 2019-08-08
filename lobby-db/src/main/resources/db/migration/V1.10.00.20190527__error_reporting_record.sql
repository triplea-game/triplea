create table error_report_history(
  id       serial primary key,
  user_ip  character varying(30) not null,
  date_created timestamp not null default now()
);

alter table error_report_history owner to lobby_user;

comment on table error_report_history is 'Table that stores timestamps by user IP address of when error reports were created. Used to do rate limiting.';
comment on column error_report_history.id is 'Synthetic PK column';
comment on column error_report_history.user_ip is 'IP address of a user that has submitted an error report';
comment on column error_report_history.date_created is 'Timestamp when error report was created in DB';
