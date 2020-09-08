alter table error_report_history
    add column system_id varchar(36);
alter table error_report_history
    add column report_title varchar(125);
alter table error_report_history
    add column game_version varchar(16);
alter table error_report_history
    add column created_issue_link varchar(128);

update error_report_history
set system_id          = '---' || random(),
    report_title       = '---' || random(),
    game_version       = '2.0.20234',
    created_issue_link = '----' || random();

alter table error_report_history
    alter column system_id set not null;
alter table error_report_history
    alter column report_title set not null;
alter table error_report_history
    alter column game_version set not null;
alter table error_report_history
    alter column created_issue_link set not null;

-- the title and version pair should be unique, otherwise we are looking at a duplicate
alter table error_report_history
    add constraint error_report_history_unique_title_version unique (report_title, game_version);
-- all links should be unique
alter table error_report_history
    add constraint error_report_history_unique_link unique (created_issue_link);
