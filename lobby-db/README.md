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

- Following that, when the [infastructure master branch](https://github.com/triplea-game/infrastructure/tree/master) 
  is merged to [infrastructure prod branch](https://github.com/triplea-game/infrastructure/tree/prod), 
  then production servers will see the live updates triggering this sequence:
    - DB will download the flyway migration artifact created during travis builds
    - flyway is executed which triggers the checked-in database updates

- By convention we keep the version number the same between the lobby binaries and the lobby-db binaries.



## Dev Setup


### pre-requirements:
- Docker installed


There is a Dockerfile in this project for building a lobby database image that can be used for development/testing.


### Usage


Rough usage flow could look like this:

```
$ ./build-docker.sh
$ ./run-docker.sh

## This will open a CLI
$ ./connect_to_db.sh
     ## show tables
  ta_users#  \l
    ## sample query
  ta_users#  select 1 from dual;
    ## quit
  ta_users# \q

## to clean up data:
$ ./drop_db.sh

## run flyway migration to create tables,
## this will pick up locally added flyway
## migration files and can be used for 
## local testing
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

