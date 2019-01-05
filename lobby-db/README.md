# lobby-db

This component is responsible for:
1. Creates a build artifact, a zip file, with DB migration files. This is eventually executed against
   the production database to 'deploy' those changes and make them live.
2. Support local development, provide tools and environment to run a DB locally (docker)


## Prod Deployments

We use [FlyWay](https://flywaydb.org/)
  - DB is not updated 'by-hand', anything to be run is checked in to 'migration files'


### Making Production Changes
- DB changes, are checked in to files here: [flyway migrations folder](https://github.com/triplea-game/triplea/tree/master/lobby-db/src/main/resources/db/migration)
- Artifact build is automatically triggered on merge, which includes:
   - [Travis](https://github.com/triplea-game/triplea/blob/master/.travis.yml) 
     invokes [Gradle](https://github.com/triplea-game/triplea/blob/master/build.gradle) to build a zip file
     of the flyway migration folder
   - Travis then pushes that zip file to [Github releases](https://github.com/triplea-game/triplea/releases)

Servers are 'listening' to the [infrastructure host_control.sh file](https://github.com/triplea-game/infrastructure/blob/master/roles/host_control.sh)
on the  [infrastructure prod branch](https://github.com/triplea-game/infrastructure/tree/prod)
 
Updating the lobby-db version on the control file will trigger servers to download that specific
migrations zip file and then execute flyway (which then runs any new SQL files not yet run against the DB).


## Dev Setup

### Prerequisites
- Docker
- `psql` (postgres-client) command 


### Usage

A [Dockerfile](https://github.com/triplea-game/triplea/blob/master/lobby-db/Dockerfile) 
with a lobby database image is used for development/testing. Convenience scripts are
provided, a typical flow looks like this:

```
      ## Create docker container
$ ./build-docker.sh
      ## Run docker Container
$ ./run-docker.sh
      ## Connect to running DB with `psql`
$ ./connect_to_db.sh
      ## Delete all data in DB
$ ./drop_db.sh
      ## Re-run fly migrations.
      ## Includes any new in-development files.
$ ./run_flyway.sh
```


## Connection Configuration

A lobby server running on the same host as the lobby database container may connect to the database using the following properties:

Property | Value | Notes
:-- | :-- | :--
User | `postgres` |
Password | _&lt;any&gt;_ | The lobby database is configured with authentication disabled, thus any password may be used.
Host | `localhost` |
Port | `5432` |

