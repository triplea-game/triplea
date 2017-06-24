
Key Build files:
- Gradle: Runs the build commands to compile, test, run the installer 
  - https://github.com/triplea-game/triplea/blob/master/build.gradle
- Install4j: Installer packaging software
  - https://github.com/triplea-game/triplea/blob/master/build.install4j
- Travis: CI builder, watches for merges and runs gradle, which runs in turns runs install4j, and that in turn pushes release artifacts to github releases.
  - https://github.com/triplea-game/triplea/blob/master/.travis.yml
  - https://github.com/triplea-game/triplea/tree/master/.travis

