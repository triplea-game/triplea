# Deployment Design

Infrastructure is managed as code. This means changes are checked in to
configuration scripts and those scripts are then across the servers.
Nobody should be logging in to a server to make changes (only to
view logs or do other kinds of troubleshooting).

Deployments are triggered as part of the build via github actions.

## Deployments

- Production is deployed to on demand. The version to deploy to production
  is defined in configuration.
