create table storage_server
(
    id           serial primary key,
    server_url   varchar(256) not null,
    date_created timestamptz  not null default now()
);

comment on table storage_server is
    $$Table recording instances of storage servers that have booted.
      When a storage server has started  up it should write a row into
      this table. This table can then in turn be used by the maps server
      to find available storage servers and provides the base URL for
      downloading files from the storage server or for uploading
      files to the storage server.$$;

create table storage_file
(
    id                serial primary key,
    folder            varchar(256) not null,
    file_name         varchar(256) not null,
    file_size         integer      not null,
    storage_server_id integer      not null references storage_server (id),
    date_created      timestamptz  not null default now()
);

comment on table storage_file is
    $$Table recording files that have been written to storage servers.
       Download file paths can be constructed by taking the server
       URL, concatenating the folder path and then the file name.
        The maps-server is responsible for recording information
        into this table, storage-servers are only tasked with
        writing file data.$$
