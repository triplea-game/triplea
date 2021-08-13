# Local Database Development

- Local database is run on docker containers
- You will need to install docker

## Operations

```
## start database
./spitfire-server/database/start_docker_db

## connect to database to inspect data
./spitfire-server/database/connect_to_docker_db

## drop schema, data and recreate
./spitfire-server/database/reset_docker_db
```

The game-headed client can be configured in 'settings > testing' to us the local server.
