create user lobby_user password 'lobby';
create database lobby_db owner lobby_user;

create user error_report_user password 'error_report';
create database error_report owner error_report_user;
