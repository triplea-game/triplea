create table map_index
(
    id               serial primary key,
    map_name         varchar(256) not null,
    last_commit_date timestamptz  not null check (last_commit_date < now()),
    repo_url         varchar(256) not null unique check (repo_url like 'http%'),
    preview_image_url varchar(256) not null check (repo_url like 'http%'),
    description         varchar(3000) not null,
    download_size_bytes integer       not null,
    download_url        varchar(256)  not null unique check (download_url like 'http%'),
    date_created     timestamptz  not null default now(),
    date_updated     timestamptz  not null default now()
);

create table tag_type
(
    id            int primary key,
    name          varchar(64) not null unique,
    type          varchar(32) not null,
    -- disallow display_order values over 1000, we do not expect nearly that many unique tag types
    display_order int         not null unique check (display_order >= 0 and display_order < 1000)
);

create table map_tag_values
(
    id        serial primary key,
    map_id    integer references map_index (id),
    tag_type_id    integer references tag_type (id),
    tag_value varchar(128) not null
);

alter table map_tag_values
    add constraint map_id__tag_id__is__unique unique (map_id, tag_type_id);
