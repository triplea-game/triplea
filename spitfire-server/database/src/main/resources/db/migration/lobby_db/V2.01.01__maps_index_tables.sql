create table map_category
(
    id           serial primary key,
    name         varchar(32) unique not null check (length(name) > 2),
    date_created timestamptz        not null default now()
);

insert into map_category (name)
values ('BEST'),
       ('GOOD'),
       ('EXPERIMENTAL');

create table map_index
(
    id               serial primary key,
    map_name         varchar(256) not null,
    last_commit_date timestamptz  not null check (last_commit_date < now()),
    repo_url         varchar(256) not null unique check (repo_url like 'http%'),
    category_id      integer      not null references map_category (id),
    description         varchar(3000) not null,
    download_size_bytes integer       not null,
    download_url        varchar(256)  not null unique check (download_url like 'http%'),
    date_created     timestamptz  not null default now(),
    date_updated     timestamptz  not null default now()
);
