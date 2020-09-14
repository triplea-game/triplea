# Game Asset Management

Game assets are sound and image files distributed with the game. They are downloaded into the 'game-headed/assets' folder where they are then picked up by the game. Game asset files are stored in the 'assets' repo: https://github.com/triplea-game/assets/

***Why an assets repo?***

File assets are kept in a different repo so we avoid checking in binary files to git. The asset repo is still a git repository where binary files are checked in, but it is intended to *only* contain binary files. The reason for the split is that git never deletes files, it only marks files as deleted. Github imposes a limit on the size of repository, updating images or sounds many times would cause us to approach and eventually breach that limit. Git operations are also slower when repositories are larger. By isolating the assets repo, we can recreate it from scratch when it starts to get too large.

***How are assets packaged?***

`game-headed` `build.gradle` has a task to download images. This is executed prior to running the game and when installers are created. Install4j is configured to build installers and bundle all files in the 'assets' folder with the installer.

On the assets repo side, a travis job executes after each merge to compress all image files and then zip them together with sound files. The zip file is then posted to github releases: https://github.com/triplea-game/assets/releases

The link to the zip file is in `game-headed/build.gradle`, it needs to be updated to pick up new releases.

