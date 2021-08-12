# How to Deploy

Deployments to pre-release are done automatically as part
of the build and are done after each merge to master.

## Manual Deploy to Prod

```
cd infrastructure
echo "<vault_password>" > vault_password
export VERSION=<VERSION_TO_DEPLOY>
./run_ansible_production
rm vault_password
```

To deploy to prerelease, instead run the `./run_ansible_prerelease` script.

### Deploy to a single server

Same steps as above with different arguments to the deploy script:
```
./run_ansible_production --limit <server_name.triplea-game.org>
```

### Deploy to just bots

```
./run_ansible_production -t bots
```

## Notes

- '--limit' and '-t' can be used together to deploy just bots, just
a single server. To deploy to multiple servers, can comma delimit their
names.

- Deployments do not restart the servers nor do any updates take effect
until a restart has been done. Restart is manual.

