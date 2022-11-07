# postgres_create_database

This role creates a database & a corresponding user with password.
The database will start out completely blank, applications and
data migration tools can use the username+password to populate
a schema or execute queries.

This role is meant to be used as part of an include block. EG:
```yaml
-- roles/[role]/tasks/main.yml

- name: Install [application] database
  include_role:
    name: database/postgres_create_database
  vars:
    database_name: "db_name"
    database_user: "db_user_name"
   database_password: "vault_encrypted_password"
```
