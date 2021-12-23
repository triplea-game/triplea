# Database

Hosts database migrations and docker to run a database locally.

Stores:
  - users
  - lobby chat history
  - user ban information
  - moderator audit logs
  - bug report history and rate limits
  - uploaded map information

For more information see: [database documentation](/docs/development/database/)

## Working with database locally

- install docker
- run: `./spitfire-server/database/start_docker_db`
- connect to DB with: `./spitfire-server/database/connect_to_docker_db`

## Example data

The example data inserted into a local docker will create an admin user
named "test" with password "test".
