# Release Steps

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
