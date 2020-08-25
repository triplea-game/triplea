# Local Database Development

- You need a local database to run the integration tests
- You need a local database to run the application servers
- Local database is run on docker containers

Databases are kept in subprojects suffixed with `db` and have
standard scripts to make things easier.

## Quick-Start

- Install docker on your system
- Run the `build_docker_db` script in each of the `*-db` projects
- Run `./start-docker-databases`

## Working with Local Databases

We will use `lobby-db` as an example.

### Build Docker

First (one-time) build your docker container with:

```
./lobby-db/build_docker_db
```

### Start Docker

Next, start the docker database with:

```
./lobby-db/start_docker_db
```

The start script will run flyway to apply migrations files
and will insert example data.

### Connect to Database on Docker

You can then connect to your local docker database with:

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

