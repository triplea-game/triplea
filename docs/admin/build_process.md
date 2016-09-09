## build.gradle
http://github.com/triplea-game/triplea/blob/master/build.gradle

Core build script for the project.
- compile / run tests / build a 'shadow' jar, a jar with all dependencies bundled
- can be used to invoke the install4j packaging process

## build.install4j
http://github.com/triplea-game/triplea/blob/master/build.install4j

Configuration for install4j. Creates platform specific installation binaries

## .travis.yml
http://github.com/triplea-game/triplea/blob/master/.travis.yml

The travis.yml build file is a series of steps (gradle build commands for the most part) that run in a Linux container provided by travis.org. After gradle does compilation, unit testing, packaging of jars, gradle kicks off the install4j build. Finally the travis config specifies a list of which output files are to be deployed to github releses.
