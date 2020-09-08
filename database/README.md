# Database

Hosts database migrations and docker to run a database locally.

Databases are expected to be on a single server with different schemas

## Schemas

### Lobby DB

Supports the lobby-server and stores:
  - users
  - lobby chat history
  - user ban information
  - moderator audit logs
  - bug report history and rate limits

For more information see: [database documentation](/docs/development/database/)

#### Example data

The example data inserted into a local docker will create an admin user
named "test" with password "test".

### Maps DB

Supports the maps-server and stores:
- upload map information
