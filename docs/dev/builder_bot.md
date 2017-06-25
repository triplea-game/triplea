---
title: Builder Bot
layout: longpage
permalink: /dev_docs/dev/builder_bot/
---
Bot Account: [github.com/tripleabuilderbot](https://github.com/tripleabuilderbot)

## Overview
An admin owned account used for automated build tasks that require repository write access.


## Personal Access Keys
- Travis automated releases key
  - Usage: [Code Snippet](https://github.com/triplea-game/triplea/blob/master/.travis.yml#L32)
  - Description: This key allows Travis to push to github releases. It is set up by the
      Travis ruby set up program when first configuring travis with TripleA.
- Map Push Key
  - Usage: [Code Snippet](https://github.com/triplea-game/triplea/blob/master/.travis/push_maps#L8)
  - Description: Write-Access to TripleA github.io website repo to add map description files
- Push Tag
  - Usage: [Code Snippet](https://github.com/triplea-game/triplea/blob/master/.travis/push_tag#L13)
  - Description: Write permission to TripleA repo for creating a new tag on each release.
      The tags are primarily for convenience, so we can relatively easily checkout a specific
      version that we released. This is also a carry-over of the process TripleA used when
      hosted in SVN.

![Tokens](https://cloud.githubusercontent.com/assets/12397753/26811743/822517d6-4a28-11e7-8342-ef4826e834b9.png)
## Travis Config:

The key values are recorded in travis as env variable values. They are write-once only. To regenerate them, regenerate the
key in github, delete the existing key in travis and recreate it with the newly known key value.

The config can be found here (You must be logged in as the bot or with admin/write-access to TripleA): [travis-ci.org/triplea-game/triplea/settings](https://travis-ci.org/triplea-game/triplea/settings)
![Travis](https://cloud.githubusercontent.com/assets/12397753/26811735/6e69c5de-4a28-11e7-8996-49338f428349.png)
## Tips
- Using 'private' mode browsing is useful when logging in as the bot account.
