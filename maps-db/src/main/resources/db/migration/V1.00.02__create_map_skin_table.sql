create table map_skin (
    id serial primary key,
    map_id integer not null references map(id),
    version integer not null check (version > 0),
    skin_name varchar(256) not null unique check(length(skin_name) > 3),
    url varchar(256) not null unique check (url like 'http%'),
    preview_image_url varchar(256) check (preview_image_url is null or preview_image_url like 'http%'),
    description varchar(4096),
    date_created timestamptz not null default now(),
    date_updated timestamptz not null default now()
);
