## lobby-db

A project that contains the migration scripts for the lobby postgres database.

### Prod Deployments
- Clone the project to lobby server
- cd to the `lobby-db` project folder

- run: `./prod_check_flyway.sh`
  - Follow prompts and add user+password to creds file if not already done.

First time you should see output like this:
```
triplea@triplea-server:~/lobby-db$ ./prod_run_flyway.sh 
 Error: Creds file does not exist at: /home/triplea/lobby.db.creds
I have created the file for you, it contains:
user=
pass=

Please update the file with valid database username and password and re-run this script
```

Now to run migrations:
- run: `./prod_run_flyway.sh`

If everything went well, it should all look like this:
```
triplea@triplea-lobby:~/lobby-db$ ./prod_run_flyway.sh 
Using user: postgres

BUILD SUCCESSFUL in 1s
1 actionable task: 1 executed
triplea@triplea-lobby:~/work/triplea/lobby-db$ ./prod_check_flyway.sh 

> Task :lobby-db:flywayInfo 
Schema version: 1.06
+-----------+---------+-------------------------------------+------+---------------------+---------+
| Category  | Version | Description                         | Type | Installed On        | State   |
+-----------+---------+-------------------------------------+------+---------------------+---------+
| Versioned | 1.00    | create tables                       | SQL  | 2018-03-14 21:14:11 | Success |
| Versioned | 1.01    | update tables                       | SQL  | 2018-03-14 21:14:11 | Success |
| Versioned | 1.02    | add bcrypt password column          | SQL  | 2018-03-14 21:14:11 | Success |
| Versioned | 1.03    | audit bans                          | SQL  | 2018-03-14 21:14:11 | Success |
| Versioned | 1.04    | audit mutes                         | SQL  | 2018-03-14 21:14:11 | Success |
| Versioned | 1.05    | add all user info to bans and mutes | SQL  | 2018-03-14 21:14:11 | Success |
| Versioned | 1.06    | add access log                      | SQL  | 2018-03-14 21:14:11 | Success |
+-----------+---------+-------------------------------------+------+---------------------+---------+



BUILD SUCCESSFUL in 0s
1 actionable task: 1 executed

```

## Dev Setup

### Install psql

- need a postgres client so you can connect to your local database

#### Windows psql client
??

#### Mac psql client
brew install postgresql

#### Linux psql client

sudo apt install psql


### Install docker

- we'll use docker to launch a local postgres DB. Steps for that are given below in the [Docker](#docker) section.


### How to use for developers


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


### Future TODOs

- Parameterize the gradle build so we can supply a different username+password
- Create a `deploy_prod.sh $DB_USER $DB_PASS` script that can be run from within the production server
  - this implies we'll do a local clone
  - user+pass can be supplied as command line args, or we can put a magic file in home directory with properties 
  - keep the script to be a local host on the production DB so we do not have to worry about opening up DB to public.

### References

- https://flywaydb.org/documentation
- https://flywaydb.org/documentation/gradle/migrate
- https://github.com/triplea-game/lobby

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
