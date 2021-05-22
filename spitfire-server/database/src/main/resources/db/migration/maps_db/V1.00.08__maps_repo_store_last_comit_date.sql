/* recreate map_index table with 'last_commit_date' column rather than a 'version' column. */
drop table map_index;
create table map_index
(
    id           serial primary key,
    map_name     varchar(256) not null,
    last_commit_date      timestamptz not null check(last_commit_date < now()),
    repo_url     varchar(256) not null unique check (repo_url like 'http%'),
    category_id  integer      not null references map_category (id),
    date_created timestamptz  not null default now(),
    date_updated timestamptz  not null default now()
);
