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