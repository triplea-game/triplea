# lobby-db

## Output
* Zip file with DB [migrations files](https://github.com/triplea-game/triplea/tree/master/lobby-db/src/main/resources/db/migration).
This zip file is downloaded in testing and production environments and executed with [FlyWay](https://flywaydb.org/) 
to update database.
* To automatically apply updates and keep a known DB state, we check-in
any DB changes to migration files to then automatically run against database.

 
## Dev Setup

### Prerequisites
- Docker
- `psql` (postgres-client) command 

### DockerFile

For a local lobby database: [Dockerfile](https://github.com/triplea-game/triplea/blob/master/lobby-db/Dockerfile) 

### Typical Usage

To launch a local DB on docker with schema:
```
./launch_db
```

After the first time you can just docker to launch faster:
```
run_docker
```

To connect to your local database:
```
./connect_to_db
```

To recreate DB data:
```
$ ./drop_db
      ## Re-run fly migrations.
      ## Includes any new in-development files.
$ ./run_flyway
```


## Connection Configuration

A lobby server running on the same host as the lobby database container may connect to the database using the following properties:

Property | Value | Notes
:-- | :-- | :--
User | `postgres` |
Password | _&lt;any&gt;_ | The lobby database is configured with authentication disabled, thus any password may be used.
Host | `localhost` |
Port | `5432` |

