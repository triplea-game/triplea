---
layout: longpage
title: Dev Setup
permalink: /dev_docs/dev/setup/
---

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
    - [eclipse code formatter plugin](https://plugins.jetbrains.com/plugin/6546-eclipse-code-formatter)
    - [eclipse launcher plugin (eclipser)](https://plugins.jetbrains.com/plugin/7153-eclipser)
      - Once installed, right click the eclipse launchers found in 'triplea/eclipse/launchers'
      - Select 'convert with eclipser', then the launchers will be available to run as intelliJ run configurations

Note that eclipse and NetBeans are currently not supporting JUnit 5. Therefore we use the JUnit 4 based Runner `JUnitPlatform.class` until there is official support. Apply it by annotating the JUnit 5 based class with `@RunWith(JUnitPlatform.class)`

  - *Troubleshooting*
    - If you are getting "JAVA HOME not yet set", see: http://stackoverflow.com/questions/31215452/intellij-idea-importing-gradle-project-getting-java-home-not-defined-yet

Development - Building
======================

[Gradle](https://gradle.org) is used. The gradlew (on Windows gradlew.bat) file is a proxy to execute build commands.
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

Github issues is used as a ticketing system. This includes task tracking, as well as communication and decision making.

We use labels to keep issues categorized: https://github.com/triplea-game/triplea/wiki/Issue-Labelling

Labels have 'tiers'. We'll have one set of labels to broadly categorize issues, and then often more labels to help refine those categories. So we might have something like "Bug + UI related" to indicate there is a bug in the game graphics. Or we may have "Bug + Game Rules" to indicate there is a bug in how the game rules are played out. As yet another example, we may have "Feature Back Log + UI" to track an improvement to the UI (basically these would be ideas of how to make the UI better).



# More Dev Setup Instructions (TODO: merge with the above)

## Install Dev Environment Requirements

### Github account:
go to github.com and create an account. (strong recommendation to set up two factor authentication so that your account will remain secure)

### Local Install:
- Git
- Gradle (2.0+)
- eclipse or intellij
- something to run Git commands with if on windows (examples: cygwin, tortoiseGit, sourcetree)

### Git Configuration:
- set up git author name and email
- set up passwordless github access. Essentially you'll generate a private and public key, and you'll upload the public key to github.
  - https://github.com/saga-project/BigJob/wiki/Configuration-of-SSH-for-Password-less-Authentication
  - TODO: how does this change for windows? Add notes if there are any differences or remove this todo if none.

## Fork the 'main' tripleA repostory
Go here: https://github.com/triplea-game/triplea, click the "fork" button in the top right

## Clone
On your new repository page, there will be a link to clone your repo on the right. If the link begins with "https" you'll be prompted for passwords. If you set up passwordless, there will be a ssh option and the link will start with "git@.." (this is preferred, more convenient and secure)

Go to your local git environment and run "git clone" and then the URL copied from git.


# Building

Run this gradle command from the project root:
$ gradle assemble


## IDE Setup

### IntelliJ
`<placeholder, please fill me in>`
`<placeholder for setting the code style formatter>`

### Eclipse
Workspace one folder above where you cloned triplea
- new project "triplea"
- fix build folders, src has an "excluded" in it:
-- build path should just be 'src' and 'test', and everything in them with no exclusion
- all jars in lib folder should be on classpath, likely picked up automatically
`<placeholder for setting the code style formatter>`


## IDE: Running tripleA and tests

Main game launcher is "GameRunner.java". When running, you need the following folders on the classpath:
- maps
- assets

Same for when running tests.
