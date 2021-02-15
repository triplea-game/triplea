# Deployment Design

Deployments are triggered as part of
[travis](https://travis-ci.org/github/triplea-game/triplea) builds.

- Avoid logging in to servers, favor checking in changes that
are then automatically deployed in stages to prerelease and then
production.

## Deployments

- Prerelease will receive the latest code after every merge.

- Production will also receive latest code but the version number
  that is deployed is fixed and controlled by configuration.
  The 'using_latest' variable is what controls whether a specific
  version or if the latest code is used.

## Setup Deployments

There is a special hostgroup in inventory 'setup' where new servers should
be added. We run a setup deployment first. Once a server has received
the setup deployment it can be removed from the setup group. We do this
to avoid overly repetitive installation of things like firewall. Generally
we want to run the full installation each time to ensure that any configuration
drift is fixed, some tasks are really better just run once though.

## Production Deployment

- Avoid deploying changes directly to prerelease or prod, but
  if needed, it can be done. Use the '--limit' tag to deploy
  to just a specific host. EG:

```
./run_ansible_production --limit prod2-bot02.triplea-game.org
```
