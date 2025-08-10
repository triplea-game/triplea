# Release Steps

## Create release branch

```
git checkout master   # or checkout a specific SHA instead of master
git checkout -b release/<release_number> # eg: release/10.2
git push origin release/<release_number>
```

This will build installers and create a release in github releases.

## Increment version

Change the version number in 'product-version.txt', increase the version number
to prep for the next release and commit this to master.

Add a port number entry in: </infrastructure/ansible/roles/lobby_server/defaults/main.yml>
Just add one to the previous highest port number.
Client request routing to lobby depends on a header value sent by game clients.
NGINX will route this to a corresponding lobby.
Incrementing version number will cause a new lobby to be deployed.

## Finalize Release Notes

Update the release-notes.md page on website.

## Update servers.yml

Increase latest version to the release. This will trigger in-game
notifications to upgrade

## Post to forums:

  - https://forums.triplea-game.org/category/1/announcements
  - http://www.axisandallies.org/forums/index.php?board=53.0

# Hotfix - Releasing a Patch

The process is very similar to a standard release.

```
git checkout release/<release_number>
git checkout -b <patch-feature-branch-name>
# do work
git commit
git push <patch-branch-name>
# create a PR to merge <patch-branch-name> into release/<release_number>
```

Once the above is done, artifacts will be built and pushed to github releases.
Double check the version number and follow the steps in the above sections
to update version numbers and issue notifications.

