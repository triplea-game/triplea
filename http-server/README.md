# Http-Server

## Strategic Fit and Overview

Http-Server is an http/REST-like backend component to complement the lobby.
The server is intended to provide a lightweight way to do client/server
interactions without needing to use the Lobby java sockets interface.

The server is intended to be run as a stand-alone process and may access
the same database as the lobby.

## Starting the server
Execute the main method in `ServerApplication`. If no args are provided then
defaults suitable for a prerelease/development are used.

### Environment Variables

For full functionality, secrets are provided via environment variables and need
to be set prior to launching the server. See the `configuration-prerelease.yml`
for those environment variable names. By default the server should be launchable
without any additional configuration, but some elements of the system may not function
without valid values.

## Configuration

Application configuration is obtained from `AppConfig.java` which is wired
from a YML file that is specified at startup. The prerelease and production
YML files may differ in the values of configuration properties, but otherwise
should have the same number of configuration properties, the same keys
and the same environment variables.

Of note, a reference to `AppConfig` is passed to the main server application
`ServerApplication` which can then wire those properties to any endpoint
'controllers' that would need configuration values.


## Typical Design of Endpoints

Endpoints typically are powered by four types of classes.


### (1) ControllerFactory class
Wires up all dependencies and creates the controller class.



### (2) Controller class
Controller classes need to be registered in `ServerApplicaton.java`
to be enabled.

This class contains endpoint markups and receives HTTP requests.
The controller methods should do quick/basic validation and then
delegate as much as possible to a 'service' class. 

### (3) Service class
The service class contains 'business' logic and should perform any database
interactions. The service class is also a translation layer to aggregate
and transform data from what we get database to what the front-end HTTP
client expects.

### (4) DAO class

These classes live in the `lobby-db` subproject, they interact
with database and should have simple input/output parameters.

Any transaction methods can live there as a 'default' method.

For example, password hashing should be done at the service layer
(unless we can do it by DB function in SQL directly), and then
the hashed password is passed to DAO for lookup.


# Design Notes

## API Keys for Moderators

HTTP endpoints are publicly available, they can be found and attacked.
Any endpoint that initiates a moderator action will accept
headers for a moderator API key and a password for that key.

Any such endpoints that take moderator keys should verify
the keys and lock out IP addresses that make too many attempts.

### Key Distribution

Super-moderators can elevate users to moderator and generate
a 'single-use-key'. This key is then provided to that moderator.

The moderator can then 'register' the key, where they provide
the single-use-key and a password. The backend verifies
the single-use-key and then generates a new key. This is 
done so that only the moderator will then 'know' the value
of their new key. The password provided is used as a salt.

### API Key Password

The purpose behind this is so that if an API key is compromised,
it won't be useful unless the password is also compromised too.

This means a moderator will need to have their OS data to
be hacked and also a key logger or something that can scrape
the password from in-memory of TripleA.

The API key password is stored in-memory when TripleA launches
and shall not be persisted anyways.

API keys are stored in "client settings", which are stored
with the OS and persist across TripleA installations.


## API Key Rate Limiting

Rate-limiting: of note, the backend implementation should be careful to apply rate limiting
to any/all endpoints that take an API key so as to avoid brute-force attacks to try and crack
an API key value.

# WARNINGS!

## Multiple SLF4J Bindings Not Allowed

Dropwizard uses Logback and has a binding with SLF4J baked in. Additional SLF4J bindings 
should generate a warning, but will ultimately cause problems (when run from gradle) and drop 
wizard may fail to start with this error:

```bash
java.lang.IllegalStateException: Unable to acquire the logger context
    at io.dropwizard.logging.LoggingUtil.getLoggerContext(LoggingUtil.java:46)
```

## Stream is already closed

This can happen when the server side does not fail fast on incorrect input.
This can be if we use headers that are missing or do not check that parameters are present.

The stack trace indicating this will look like this:
```bash
ERROR [2019-06-06 05:07:22,247] org.glassfish.jersey.server.ServerRuntime$Responder: An I/O error has occurred while writing a response message entity to the container output stream.
! java.lang.IllegalStateException: The output stream has already been closed.
```

The impact of this is: 
- server thread hangs
- client hangs
- server does not shutdown cleanly

This is bad as it could be used in a DDOS attack.

### Prevention

Essentially fail-fast:
- When looking for headers, verify headers exist or terminate the request
- Verify that all needed GET parameters are present or terminate the request

To terminate the request, just throw a IllegalArgumentException, it'l be mapped to a 400.

## 404 error, but endpoint is registered!?

Make sure in addition to the `@Path` annotation on the endpoint method,
ensure the controller class has a `@Path("")` annotation on it.

# Lobby-db-dao

This projects contains "DAO" code, data access layer.

Contains Java code focused on executing SQL. Parameters passed to this
layer should be as simple as possible so that this layer is as close
to pure SQL as possible.


## JDBI result mapper

To return data objects from JDBI queries, register mappers in `JdbiDatabase.java`

### Patterns for result mappers

(1)  The mapped data object should have a static `buildResultMapper` function

(2) Use constants to reference columns names


### Patterns for JDBI Result Data Objects

#### Naming
Naming:  Suffixed `DaoData.java` so it is clear that these objects 
are coming from database.

#### Do not couple front-end to the database

Do not return DAO data objects to http clients. Typically the backend
server layer will convert DAO data objects into a shared data object
that is shared between front-end and backend. This way DB changes do 
not cascade to the front-end clients.


## Design Pattern for Transactions

- Create a new interface; e.g.: `ModeratorKeyRegistrationDao.java`
- Add a default method with the `@Transaction` annotation.
- add a dummy select query so that JDBI sees the interface as valid
- pass the needed DAO objects as parameters to the default method
- use mockito mocks to test the method

