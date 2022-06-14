Installs a Postgres DB and creates application schemas.

After this role is run, there will be an empty database ready
for 'flyway' to execute database migrations which will create
tables (and procedures and indices, etc..) and insert seed data.

Access to database for admins and deployments is done by first
SSH'ing into the server and then using `sudo -u` to switch to
the OS user 'postgres'.  As the 'postgres' user, no password is
required when accessing database.

EG:
```
ssh lobby-database-server
sudo -u postgres psql
```
