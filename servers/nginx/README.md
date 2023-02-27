## WIP - TODO

TODO: this is still a work in progress!
- docker:
  - create a conf file that has local routing defined, EG: path 'https://locahost/lobby' -> 'http://localhost:8080'
  - generate some SSL certs that can be used for localhost and map those to nginx
- deployment:
  - define some ansible deployment stuff; so we can actually do the deployment
  - add github workflow file that would kick off a deployment when this folder is updated


## Overview - How we use NGINX

NGINX is used for:
- (1) request routing, as a reverse proxy. To route requests based on URL path to the right server instance
- (2) SSL, nginx provides a convenient way to enable HTTPS communication

The general strategy for request routing is to first have a single NGINX at a fixed URL: "prod.triplea-game.org"

Then, based on path, EG: "/service-name", we will route requests to the machine that is running that service.

## Running locally

NGINX can be run locally via docker. The purpose of this is so we can run NGINX locally to replicate how the production
environment would behave.

To start NGINX locally, run:

```
./docker-run.sh
```

After opening the game client, in the 'testing' section the URL of servers can be changed to 'localhost'. This will
cause requests to be sent to the localhost NGINX running on docker.


## Build & Deployment

We deploy nginx to bare-metal using ansible. Whenever this folder is updated, on build we will redeploy
the latest to production. In order to avoid any disconnections, we will do a 'nginx' reload to pick up the
latest configuration.

