## .docker

With docker you can run TripleA databases and servers.

This folder contains scripts & config files for interacting with docker and
helper scripts for starting servers.

## Starting lobby

Run:

```bash
./start-servers.sh
```
Lobby will be running on localhost port 5000.

## Test Data

As part of database startup, we'll also insert sample data that can
be handy for testing, for example a lobby user with (user:pass) "test:test"
is created.
