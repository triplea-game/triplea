# Release Steps

## Create release branch

```
git checkout master   # or checkout a specific SHA instead of master
git checkout -b release/<release_number> # eg: release/10.2
git push origin release/<release_number>
```

This will build installers and create a release in github releases.


## Increment version


Change the version number in 'product.properties', increase the version number
to prep for the next release and commit this to master.

## Finalize Release Notes

{ TODO: link to the release note creation script and instructions here }

## Update servers.yml

Increase latest version to the release. This will trigger in-game
notifications to upgrade


## Update partner sites:

  - http://www.freewarefiles.com/TripleA_program_56699.html
  - http://download.cnet.com/TripleA/3000-18516_4-75184098.html


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

