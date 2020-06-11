## Flyway Quick Overview
Flyway is a database migration utility. We define a set of 'changeset' files that contain SQL commands,
the changeset files are executed by flyway in order. Flyway keeps tracking tables to know which
changesets have been executed and it does not re-execute changesets. Flyway also computes a md5 hash
of changeset files and will halt if existing changesets have been modified (deleting the md5 value
will tell flyway to recompute the md5 of a file and execute any new changesets. In general avoid
modifying existing changeset files).

## Flyway Role
  - Downloads flyway executable
  - Downloads changeset files
  - Executes changesets
