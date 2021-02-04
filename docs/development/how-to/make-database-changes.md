# Make Database Changes

## Overview

- Database changes are executed through migration files.
- Each migration file should generally accomplish one overall goal
- [Flyway](https://flywaydb.org/) is used to run new migration files.
- Do not modify existing migration files

## Create a Migration File

- Start local docker database: `./database/start_docker_db`
- (A) Create a file in `database/src/main/resources/db.migrations/<schema>/`
- (B) Add  SQL code to that file
- (C) Run `./database/reset_docker_db` to redeploy changes to database
- Verfiy changes:
  - connect to local db: `./database/connect_to_docker_db`
  - run/write DB unit tests
- Repeat (B) and (C) until done
- Repate (A), (B), (C) until all desired DB changes are ready
- Commit and submit for PR

See </docs/development/reference/db-migration-file-versioning.md> for
migration file versioning and naming.

