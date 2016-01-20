TripleA website: http://triplea-game.github.io/

TripleA Game Features
=====================
- Various multiplayer gameplay options:
  - live online lobby
  - direct network connection
  - play by email
  - play by forum
- AI for single player play
- Generic game engine allows you to build your own maps and mods


Installing TripleA and Playing
===============================

Download the installer appropriate for your operating system here: http://github.com/triplea-game/triplea/releases/latest, then run the installer, it will guide you through the rest of the game installation.


Learning to Play
================

- Install the game and play the new tutorial map
- Play against the AI
- Read the rule book: http://github.com/triplea-game/triplea/blob/master/TripleA_RuleBook.pdf
- Join the online TripleA game lobby and observe games


Bug Reports
===========

Please submit bug reports to: http://github.com/triplea-game/triplea/issues/new


TripleA Maps
============

Maps are hosted in: http://github.com/triplea-maps
For issues global to all maps, please refer to the map adminstration project: http://github.com/triplea-maps/Project


Build system
============

[Gradle](http://gradle.org) is used. The gradlew (on Windows gradlew.bat) file is a proxy to execute build commands. 
On first call these files will install the correct version of Gradle on your system. Most commonly used commands:

* creates a jar file from the project, dependencies are not added:
```
./gradlew jar
# creates into build/libs/triplea-<version>.jar
./gradlew test
# compiles the project and runs the unit tests
```

* creates a self contained jar file from the project, all JAR dependencies are included
```
./gradlew shadowJar
# creates into build/libs/triplea-<version>-all.jar
```
* run the application right from the source (no jar is created); this may be used from inside an IDE to debug
```
./gradlew run
# creates into build/libs/triplea-<version>-all.jar
```

  
Tooling and IDE setup
=====================
 
With the help of the Gradle system any modern IDE support is provided out of box, such as:

* Eclipse:
  - use the [Buildship plugin](https://github.com/eclipse/buildship/blob/master/docs/user/Installation.md)
  - [Gradle integration plugin](https://marketplace.eclipse.org/content/buildship-gradle-integration)
* Intellij IDEA - out of box integration support (just import project, and specify the settings.gradle file)
* Netbeans - use the [Gradle plugin](http://plugins.netbeans.org/plugin/44510/gradle-support)

 
Automated Deployment
=====================

We use gradle+install4j to create Windows, Mac, and a Linux game installer. We use Travis to invoke gradle,run tests and build the source code and installers whenever there is a merge to master. Travis is then configured to push those installer to [github releases automatically](https://github.com/triplea-game/triplea/releases) on each merge.

More documentation on how build system is configured can be found on the [triplea github wiki]
(https://github.com/triplea-game/triplea/wiki/Continuous-Build-Process-Configs)
