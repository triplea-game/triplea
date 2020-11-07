# Local Build And Testing

All commands assume a working directory of the triplea clone. EG: `cd ~/work/; git clone git@github.com....triplea; cd ./triplea`

Docker for Mac can be obtained at: https://store.docker.com/editions/community/docker-ce-desktop-mac


## Start Dev Database

This is needed to run integration tests or to launch a local lobby.

```
./database/start_docker_db
```
The above will: 
- Launch local docker DB (postgres)
- run flyway (schema install)
- load sample data (adds a moderator "test:test" user valid for lobby login):


For any SQL work, connect to the local database with:
```
./database/connect_to_docker_db
```

To reload the local database with fresh data (needed after running tests), run:
```
./database/reset_docker_db
```

## Running all checks & tests

```
./verify
```

To run just static analysis checks (checkstyles & PMD & custom checks), run:
```
./verify-no-tests
```


## Formatting

```
./gradlew spotlessApply
```

To format everything, including documentation files run:
```
./format
```


## Testing a local lobby

* Start lobby by using the checked-in IDE launcher 'lobby-server'
* Start a bot, look for the headless game launcher, the bot will connect to your local lobby
* Start the client, look for headed-game client launcher
* In engine settings, go to test, select "use local lobby"
* Click play online, log in without an account or use the predefined
  moderator account "test:test"
