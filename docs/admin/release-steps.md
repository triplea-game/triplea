# Release Steps

## How to Release

- Update release notes
- Update github releases, uncheck "this is a prerelease" on the release:
  <https://github.com/triplea-game/triplea/releases>
- Update servers.yml, increase latest version to the release. This will
trigger in-game notifications to upgrade

- create release branch (assuming the release is the latest version on master)
```
git checkout master
git checkout -b release/<release_number>  # eg: release/2.2
git push origin release/<release_number>
```

The release branch will be there in case we need to do any patches post-release.
Post-releases patches are not at all good things, but are there in case we
really need to fix something.

- Change the version number in 'product.properties', increase the version number
to prep for the next release.

- Update partner sites:  
  - http://www.freewarefiles.com/TripleA_program_56699.html  
  - http://download.cnet.com/TripleA/3000-18516_4-75184098.html  

- Post to forums:
  - https://forums.triplea-game.org/category/1/announcements
  - http://www.axisandallies.org/forums/index.php?board=53.0

## Releasing a Patch

The process is very similar to a standard release.

```
git checkout release/<release_number>
git checkout -b <patch-branch-name>
# do work
git commit
git push <patch-branch-name>
# create a PR to merge <patch-branch-name> into release/<release_number>
```

Once the above is done, Travis will build and push artifacts to github releases.
Double check the version number and follow the steps in the above sections to update
version numbers and issue notifications.


