## lobby-db

A project that contains the migration scripts for the lobby postgres database.



### Prod Deployments

We use [FlyWay](https://flywaydb.org/) to execute database changes. This means we do not change DB by hand,
but instead check in the commands we wish to run and then deploy and run those commands
with FlyWay.


- Production database changes are triggered by checking in
  [flyway changes](https://github.com/triplea-game/triplea/tree/master/lobby-db/src/main/resources/db/migration)
  followed by checking in an updated version number to infrastructure
  [host_control.sh](https://github.com/triplea-game/infrastructure/blob/master/roles/host_control.sh)

- Following that, when the [infastructure master branch] is merged to [infrastructure prod branch], then
  production servers will see the live updates triggering this sequence:
    - DB will download the flyway migration artifact created during travis builds
    - flyway is executed which triggers the checked-in database updates

- By convention we keep the version number the same between the lobby binaries and the lobby-db binaries.


### FlyWay References

- https://flywaydb.org/documentation
- https://flywaydb.org/documentation/gradle/migrate
- https://github.com/triplea-game/lobby


## Dev Setup

pre-requirements:
- docker installed


## Docker

There is a Dockerfile in this project for building a lobby database image that can be used for development/testing.


### Build

Build the lobby database image using the following command (run from this directory):

```
$ docker build --tag triplea/lobby-db:latest .
```

### Run

Start a new lobby database container using the following command:

```
$ docker run -d --name=triplea-lobby-db -p 5432:5432 triplea/lobby-db
```

### Usage

A lobby server running on the same host as the lobby database container may connect to the database using the following properties:

Property | Value | Notes
:-- | :-- | :--
User | `postgres` |
Password | _&lt;any&gt;_ | The lobby database is configured with authentication disabled, thus any password may be used.
Host | `localhost` |
Port | `5432` |

Convenience scripts for executing common commands are included. You will need to install
a postgres database first locally.


Typical commands:
```
## drops+creates empty database (no tables)
./drop_db.sh

## runs flyway migrations (creates tables)
./run_flyway.sh

## util script to connect to a local DB
./connect_to_db.sh

## common psql commands
\l  ## list databases
\c ta_users   ## connect to 'ta_users' DB
\d ## show tables
```
