This role is meant to be used as part of an include block. EG:

```yaml
-- roles/[role]/tasks/main.yml

- name:  Install [application] database
  include_role:
    name: database/flyway_run_migration
  vars:
    migrations_file: "[name_of_zip_file_found_on_ansible control host]"
    flyway_db_name: ""
    flyway_migration_dir: "[name of directory created when expanding the zip file]"
    flyway_user: ""
    flyway_password: ""
```
