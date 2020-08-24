## Overview
Database SQL to be executed is first checked in to a migration file and then run as part of a 
release. All updates to database should flow through migration files.

### Flyway Migration File Versioning
Migration files have the lobby version as part of their name, eg:

- `V1.09.00__lobby.sql` corresponds to lobby version 1.9.0

New migration files should have the next version number in their name and to avoid
merge conflicts, a date suffix.

For example, if current version is a `1.10.00`, and the next version `1.10.01`, then a migration file in PR
might be named:  `V1.10.01.20190313.1200__description_of_change.sql` (where 20180313 is current date, and 1200 
is the noon hour).

### Migration File Squashing
At some point before release, changes will start to be squashed together. Any squashing of files will be to
already checked in files and with the date suffix above will not conflict with any in-flight changes.

So for example one might see the following migration file listing:

```
V1.00.00__lobby.sql
V1.09.00__lobby.sql
V1.10.00__lobby.sql
V1.10.00.20190313
```

In the above example:
- `1.09.00__lobby.sql` is current production
- `1.10.00__lobby.sql` is next version changes squashed
- `1.10.00.201903...` is recently merged and eventually will be squashed into `1.10.00__lobby.sql`

