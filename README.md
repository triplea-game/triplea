[![Travis](https://img.shields.io/travis/ajoberstar/gradle-git.svg?style=flat-square)](https://travis-ci.org/triplea-game/triplea) [![tripleA license](https://img.shields.io/github/license/triplea-game/tripleA.svg?style=flat-square)](https://github.com/triplea-game/triplea/blob/master/LICENSE)<br>
TripleA is a free game engine that runs on open source and is community supported. 


Installing TripleA and Playing
==============================
- Download and install TripleA: http://triplea-game.github.io/download/
- Bug reports, suggestions: http://github.com/triplea-game/triplea/issues/new
- Online PDF rule book: http://github.com/triplea-game/assets/blob/master/rulebooks/TripleA_RuleBook.pdf


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
Use github issues to create a tracking ticket: https://github.com/triplea-game/triplea/issues/new
We encourage people to create these relatively liberally. To be most effective though, here is the information that will be of most help:
- be as concise and specific as you can
- describe the context of a problem, what do you need to do to trigger it, how do you set it up. Include any relevant files zipped to the tracking ticket, or include a link to the same files. Please keep in mind it may be some time before an issue is actually worked on
- describe what goes wrong, what is the problem as exactly as you can state it? Can be pretty simple sometimes: "Then this error pops up: (include contents of the error message, or if it's really long, you can create a github git to save it to and post a link to that: https://gist.github.com/)
- describe what you would have wanted to have happened, or what you were expecting. This helps keep us from guessing what the behavior should have been, so when we fix it, we know exactly how things "should" be.

The overall goal is to keep a tracking ticket so we can discuss and identify problems/features directions we are going. Secondly, an issue is most ideal when it is an isolated independent nugget, that someone can pick up, read, and really understand enough context of the problem and have directions to reproduce the problem, at which point they can immediately begin work.


Related Websites
================
- TripleA HomePage: http://triplea-game.github.io/download/
- TripleA Community Website: http://www.tripleawarclub.org/
- TripleA Maps: http://github.com/triplea-maps
- TripleA source forge (legacy): https://sourceforge.net/projects/triplea/


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

  - *Troubleshooting*
    - If you are getting "JAVA HOME not yet set", see: http://stackoverflow.com/questions/31215452/intellij-idea-importing-gradle-project-getting-java-home-not-defined-yet

 
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

Building Game Installers and Travis Deployment
==============================================

On each and every merge to master, travis picks up, runs gradle, which then invokes builds, tests, and invokes install4j to create game installers. Travis then is configured to push the installer files to [github releases ](https://github.com/triplea-game/triplea/releases)

More documentation on how build system is configured can be found on the [triplea github wiki]
(https://github.com/triplea-game/triplea/wiki/Continuous-Build-Process-Configs)
