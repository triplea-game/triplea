comment on database maps_db is 'Database used to store, track and serve map information';

create table map (
  id serial primary key,
  name varchar(256) not null unique check(length(name) > 3),
  date_created timestamptz not null default now(),
  date_updated timestamptz not null default now()
);

create table category (
  id serial primary key,
  name varchar(32) unique not null check (length(name) > 2),
  date_created timestamptz not null default now()
);

insert into category (name) values
  ('BEST'),
  ('GOOD'),
  ('EXPERIMENTAL');


create table map_version (
  id serial primary key,
  map_id integer not null references map(id),
  version integer not null check (version > 0),
  url varchar(256) not null check (url like 'http%'),
  thumbnail varchar(256) not null check (thumbnail like 'http%'),
  category_id integer not null references category(id),
  description varchar(4096),
  date_created timestamptz not null default now(),
  date_updated timestamptz not null default now()
);

