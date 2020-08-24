# Local Build And Testing

## Dev Database
Use docker to launch a local postgres DB, flyway used to install schema. Steps:

In one step, to start docker with DB and data, run the convenience script:
```
./launch_db
```

Individual steps of DB deployment can be run when needed:
```bash
cd lobby-db/
# build docker only needs to be run once
./build_docker

# launches docker, after this step an empty database will be created
./run_docker

# wait about 3 or 5 seconds for database to start
# Flyway will install tables
./run_flyway

# Drops any existing data and adds some basic data (of note, a user with name and password 'test')
./load_sample_data
```

*Note*: integration tests will overwrite DB data, `./load_sample_data` will need to be re-run after running integ tests.

Docker for Mac can be obtained at: https://store.docker.com/editions/community/docker-ce-desktop-mac

## Building and Running the Code  / Typical gradle commands
- Eclipse and IDEA Launchers for are checked in and can be launched from IDE.

- Apply code formatting (should fix most checkstyle violations):
```bash
./gradlew spotlessApply
```

### Run all checks, tests, and integration tests:
- Start database on docker, see [database steps above](#Dev Database)
- In another console window, start http server:
```bash
cd http-server/
../gradlew run
``` 
Run all tests and checks (run **this** before submitting PRs):
```bash
./verify
```

## Connecting to a local lobby

- Launch DB
- Start an http server instance (use IDE launcher  or `./gradlew http-server:run`)
- Start a lobby instance (use IDE launcher  `./gradlew lobby:run`)
- Start a game-headed instance (use IDE launcher or `./gradlew game-headed:run`)
  - Go to settings > testing
  - Update lobby host address to 'localhost' and save
  - restart the game-headed instance
  - click 'play online', connection will be to the local lobby


