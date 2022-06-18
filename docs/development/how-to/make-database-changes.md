# Make Database Changes

## Overview

- Database changes are executed through migration files
- To make database changes, add a new migration file containing the SQL to be run
- Each migration file should generally accomplish one overall goal
- [Flyway](https://flywaydb.org/) is used to run migration files.
- Do not modify existing migration files

## Create a Migration File

- Create a file in `servers/database/src/main/resources/db.migrations/<schema>/`
- Add  SQL code to that file

## Testing Database Migrations

- Start local docker database: `./servers/database/start_docker_db`
- Run `./servers/database/reset_docker_db` to clean database and re-run migrations
- Connect to local db & verify changes: `./servers/database/connect_to_docker_db`

See </docs/development/reference/db-migration-file-versioning.md> for
migration file versioning and naming.

