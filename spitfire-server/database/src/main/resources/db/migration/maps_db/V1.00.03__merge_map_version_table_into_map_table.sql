-- Merge table map_version into map

alter table map add column url varchar(256) not null unique check (url like 'http%');
alter table map add column description varchar(4096);
alter table map add column category_id integer not null references category(id);
alter table map add column preview_image_url varchar(256) check (preview_image_url is null or preview_image_url like 'http%');
alter table map add column version integer not null check (version > 0);

drop table map_version;
