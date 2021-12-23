# DB Migration File Versioning

Roughly follows [semver](https://semver.org/)

An example migration file could be named: `V1.09.00__add_some_table.sql`

The structure of the migration file is:

```
V< compatibility > . < feature > . < patch >__< desciption >.sql
```

Increment the `compatibility` number when for example an application must be updated
to be able to continue reading the existing database (for example a table is dropped).

Increment the `feature` number when introducing a new set of database features.
Typically most work will start by increasing this. While an application might need to
be updated to pick up the changes, existing applications should still be able to run
okay without any updates.

Increment the `patch` number when refining an existing feature, (EG: adding indexes).
