## Workflows

This folder contains "[github-action](https://github.com/features/actions)" configurations.

Generally for all changes we will run './verify' (all tests). On merge to master, we will
also upload game artifacts and deploy latest builds to the production servers with exception
for servers that do not support zero downtime deployments (they are not updated automatically).

