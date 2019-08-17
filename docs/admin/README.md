# Project Admin & Owner Documentation

## How to Release

Before a release has started, we need to be really confident it has been well tested. Check that the QA group has
given the green-light for the release.

### Game Engine Release
- Verify the release notes: https://github.com/triplea-game/triplea-game.github.io/blob/master/release_notes.md
   - Add link to full list of merged PRs included in release: https://github.com/triplea-game/triplea-game.github.io/blob/master/.github/generate_release_changes_url
- Mark the target release version as latest: https://github.com/triplea-game/triplea/releases

### Major releases
- Can send new game clients to a new lobby: https://github.com/triplea-game/triplea/blob/master/lobby_server.yaml
- Change the version number in the travis build file: https://github.com/triplea-game/triplea/blob/master/.travis.yml
- Trigger game client notifications: https://github.com/triplea-game/triplea/blob/master/latest_version.properties
- Update partner sites:  
  - http://www.freewarefiles.com/TripleA_program_56699.html  
  - http://download.cnet.com/TripleA/3000-18516_4-75184098.html  
- Post to forums:
  - https://forums.triplea-game.org/category/1/announcements
  - http://www.axisandallies.org/forums/index.php?board=53.0

### Lobby/Bot Version Upgrades
- This is described in the [infrastructure project](https://github.com/triplea-game/infrastructure)


## [Bot Account](https://github.com/tripleabuilderbot)

This is a github account that the project uses to do automated pushes to our github repository. The travis build kicks
in after code merges and we use SSH keys attached to this account to grant automated access. This account has
write access to the repository. We use this account so it that these keys are not tied to any one individual. 

### Personal Access Keys

![Tokens](https://cloud.githubusercontent.com/assets/12397753/26811743/822517d6-4a28-11e7-8342-ef4826e834b9.png)

- *push_tags*: 
  - *Description*: Write permission to TripleA repo for creating a new tag on each release.
      The tags are primarily for convenience, so we can relatively easily checkout a specific
      version that we released. This is also a carry-over of the process TripleA used when
      hosted in SVN.
  - *Location*: environment variable
  - *Usage*: [push_tags script](https://github.com/triplea-game/triplea/blob/master/.travis/push_tag#L13)
- *push_maps*
  - *Description*: Write-Access to TripleA github.io website repo to add map description files
  - *Location*: environment variable
  - *Usage*: [push_maps script](https://github.com/triplea-game/triplea/blob/master/.travis/push_maps#L8)
- *automatic releases*
  - *Description*: This key allows Travis to push to github releases. It is set up by the
      Travis ruby set up program when first configuring travis with TripleA.
  - *Stored Location*: encrypted in the travis.yml file
  - *Usage*: [travis.yml](https://github.com/triplea-game/triplea/blob/master/.travis.yml#L32)
- *Http-Server / Github Create Issue*
  - *Description*: Used by http-server to communicate with Github webservice API to create issues
      for error report upload.
  - *Stored Location*: filesystem service init file, see [infrastructure docs](https://github.com/triplea-game/infrastructure/docs)
  - *Usage*: [GithubIssueClient.java](https://github.com/triplea-game/triplea/blob/master/http-client/src/main/java/org/triplea/http/client/github/issues/GithubIssueClient.java)

