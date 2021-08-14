# Local Database Development

- Local database is run on docker containers
- You will need to install docker

Docker for Mac can be obtained at: <https://store.docker.com/editions/community/docker-ce-desktop-mac>

## Operations

```
## start database
./servers/database/start_docker_db

## connect to database to inspect data
./servers/database/connect_to_docker_db

## drop schema, data and recreate
./servers/database/reset_docker_db
```
