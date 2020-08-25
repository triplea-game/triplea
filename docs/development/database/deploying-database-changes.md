# Deploying Database Changes

## Overview

- Database changes are executed through migration files.
- [Flyway](https://flywaydb.org/) is used to run new migration files.
- Migrations files live in the `*-db` subprojects, in: `src/main/resources/db`
- New migration files only need to checked in and merged to master to
  be executed in prerelease and then eventually production.
- Execution of flyway to 'prerelease' is automatic after each merge.
- Execution of flyway to 'production' is automatic on production deployments.

## Migration File Versioning

- Versioning will follow [semver](https://semver.org/)
- Do not modify files that have already been run against prerelease (Flyway will halt
  if an already applied migration has been modified).
- Group related database changes together to a single migration file and keep the
  migrations roughly single-purpose. For example, if adding indexes to existing tables,
  create one migration file for that and another for adding new tables.

An example migration file could be named: `V1.09.00__add_some_table.sql`

The structure of the migration file is:
```
V< compatibility > . < feature > . < patch >__< desciption >.sql
```

Increment the `compatibility` number when for example an application must be updated to
be able to continue reading the existing database (for example a table is dropped).

Increment the `feature` number when introducing a new set of database features. Typically
most work will start by increasing this. While an application might need to be updated to
pick up the changes, existing applications should still be able to run okay without
any updates.

Increment the `patch` number when refining an existing feature, (EG: adding indexes).
