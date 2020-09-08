# Local Database Development

- Local database is run on docker containers
- You need a local database to run the integration tests
- You need a local database to run the application servers

We run multiple schemas on one database. All schema info
and database config is in the 'database' subproject.

## Quick-Start

- Install docker on your system
- Run the `./database/build_docker_db` (one-time)
- Run `./start_docker_db`


## Working with Database

The start script will run flyway to apply migrations files
and will insert example data.

### Connect to Database on Docker

You can connect to your local docker database with:

```
./lobby-db/connect_to_docker_db
```

Note: You can view the connect script to obtain  connection parameters
such as port number and credentials.

### Reset Database Docker

If you are in development and wish to have a clean database (for example
you are modifying flyway migration files), to recreate a running
database, run:

```
./lobby-db/reset_docker_db
```

