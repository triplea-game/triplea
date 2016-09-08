[![Travis](https://img.shields.io/travis/ajoberstar/gradle-git.svg?style=flat-square)](https://travis-ci.org/triplea-game/triplea) [![tripleA license](https://img.shields.io/github/license/triplea-game/tripleA.svg?style=flat-square)](https://github.com/triplea-game/triplea/blob/master/LICENSE)<br>
TripleA is a free game engine that runs on open source and is community supported. 

Installing TripleA and Playing
==============================
- Download and install TripleA: http://triplea-game.github.io/download/
- Online PDF rule book: http://github.com/triplea-game/assets/blob/master/rulebooks/TripleA_RuleBook.pdf

TripleA Websites
================
- HomePage: http://triplea-game.github.io/
- Community Website: http://www.tripleawarclub.org/
- Community Maps Repository: http://github.com/triplea-maps
- Source Forge (legacy): https://sourceforge.net/projects/triplea/
- Bug reports, feature requests: http://github.com/triplea-game/triplea/issues/new

TripleA Game Features
=====================
- Free to play, 100% open source and community supported
- Online lobby, find, join and observe live games
- Play by correspondence (email)
- Play single player against the AI
- Many community created maps available for in-game download
- Generic game engine allows you to [build your own maps and mods](https://github.com/triplea-maps/Project)

Reporting Bugs / Feature Requests
=================================
- http://www.triplea-game.org/bug_report/


Map Making
==========
Instructions and more: https://github.com/triplea-game/triplea/tree/master/docs/map_making
Beyond experimental maps:  https://github.com/triplea-game/triplea/wiki/Broken-Maps


Development 
===========
- [Initiatives and Projects](https://github.com/triplea-game/triplea/issues/1073)
- [Bug backlog](https://github.com/triplea-game/triplea/issues?q=is%3Aissue+is%3Aopen+label%3A%22Bug+Backlog%22)
- [Developer and Project Documentation](https://github.com/triplea-game/triplea/tree/master/docs)

Developer Setup
=================
*Eclipse*
  - Import the project as a gradle project
  - Plugins to Install:
    - [Buildship plugin](https://github.com/eclipse/buildship/blob/master/docs/user/Installation.md)
    - [Gradle integration plugin](https://marketplace.eclipse.org/content/buildship-gradle-integration)

*Netbeans*
  - Use the [Gradle plugin](http://plugins.netbeans.org/plugin/44510/gradle-support)

*Intellij IDEA*
  - Import the project as a gradle project, specify the settings.gradle file
  - Plugins to Install:
    - [eclipse code formatter plugin](https://plugins.jetbrains.com/plugin/6546)
    - [eclipse launcher plugin (eclipser)](https://plugins.jetbrains.com/plugin/7153?pr=idea)
      - Once installed, right click the eclipse launchers found in 'triplea/eclipse/launchers'
      - Select 'convert with eclipser', then the launchers will be available to run as intelliJ run configurations

Note that eclipse and NetBeans are currently not supporting JUnit 5. Therefore we use the JUnit 4 based Runner `JUnitPlatform.class` until there is official support. Apply it by annotating the JUnit 5 based class with `@RunWith(JUnitPlatform.class)`

  - *Troubleshooting*
    - If you are getting "JAVA HOME not yet set", see: http://stackoverflow.com/questions/31215452/intellij-idea-importing-gradle-project-getting-java-home-not-defined-yet

Development - Building
======================

[Gradle](http://gradle.org) is used. The gradlew (on Windows gradlew.bat) file is a proxy to execute build commands. 
On first call these files will install the correct version of Gradle on your system. Most commonly used commands:

* creates a jar file from the project, dependencies are not added:
```
./gradlew jar
# creates into build/libs/triplea-<version>.jar
./gradlew test
# compiles the project and runs the unit tests
./gradlew check
# runs the tests and runs the checkstyle application to be able to review the code quality
a report html page can be found under {projectDir}/build/reports/checkstyle/
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

Building Game Installers and Travis Deployment
==============================================

On each and every merge to master, travis picks up, runs gradle, which then invokes builds, tests, and invokes install4j to create game installers. Travis then is configured to push the installer files to [github releases ](https://github.com/triplea-game/triplea/releases)

More documentation on how build system is configured can be found in  the [/docs]
(https://github.com/triplea-game/triplea/blob/master/docs/build_process.md)



Task and Issue Tracking
========================

* Discussion - technical discussions, anything non-technical should be pushed to forum
* Feature Backlog - prioritized list of potential features
* Bug Backlog - prioritized list of bugs to fix
* Infra/Code Backlog - prioritized list of build, infra, code improvement, process, misc issues
* Next Release - what we are currently targeting for the next release (anything in progress should have an assignee)

Every issue should get a tag identifying whether it is a discussion, feature, bug, infra/code (couple of tags here). We then have tags to categorize the portion of TripleA experience it affects such as UI, performance, AI, game play/rules, map support, etc.

For the most part each issue should end up with 2 tags with 1 from each list (sometimes it might not get a second tag or have multiple tags from the second list).

For background and discussion, please see: See: https://github.com/triplea-game/triplea/issues/1059
