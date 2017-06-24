
## Overview
Bot Account: [github.com/tripleabuilderbot](https://github.com/tripleabuilderbot)

An admin owned account used for automated build tasks that require repository write access.


## Personal Access Keys
- *Travis Automated Releases Ley*
  - Description: This key allows Travis to push to github releases. It is set up by the
      Travis ruby set up program when first configuring travis with TripleA.
  - Access: encrypted in the travis.yml file
  - Usage: [travis.yml](https://github.com/triplea-game/triplea/blob/master/.travis.yml#L32)
- *Map Push Key*
  - Description: Write-Access to TripleA github.io website repo to add map description files
  - Access: environment variable
  - Usage: [push_maps script](https://github.com/triplea-game/triplea/blob/master/.travis/push_maps#L8)
- *Push Tag Key*
  - Description: Write permission to TripleA repo for creating a new tag on each release.
      The tags are primarily for convenience, so we can relatively easily checkout a specific
      version that we released. This is also a carry-over of the process TripleA used when
      hosted in SVN.
  - Access: environment variable
  - Usage: [push_tags script](https://github.com/triplea-game/triplea/blob/master/.travis/push_tag#L13)

Screenshot below of the three tokens described above:
![Tokens](https://cloud.githubusercontent.com/assets/12397753/26811743/822517d6-4a28-11e7-8342-ef4826e834b9.png)

## Regenerating Travis Environment Variables:

Can be done through the Travis UI. Note they are write-once, so they just need to be deleted and re-created with known values.

The config can be found here (You must be logged in as the bot or with admin/write-access to TripleA): [travis-ci.org/triplea-game/triplea/settings](https://travis-ci.org/triplea-game/triplea/settings)
![Travis](https://cloud.githubusercontent.com/assets/12397753/26811735/6e69c5de-4a28-11e7-8996-49338f428349.png)

## Tip
- Using 'private' mode browsing is useful when logging in as the bot account.
