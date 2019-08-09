## Dev Database
Use docker to launch a local postgres DB, flyway used to install schema. Steps:
```bash
cd lobby-db
# build docker only needs to be run once
./build_docker
./run_docker
# wait about 3 or 5 seconds for database to start
./run_flyway
```
Docker for Mac can be obtained at: https://store.docker.com/editions/community/docker-ce-desktop-mac

## Building and Running the Code  / Typical gradle commands
- Launchers for lobby and headed-game client should be checked in and can be launched from IDE.

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

## DB Data for Local Testing
To load sample data to a locally running DB, run:
```bash
./lobby-db/load_sample_data
```
