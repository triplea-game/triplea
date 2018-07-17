# Building and Running the Code

```
./gradlew test
./gradlew check
./gradlew run
```

# IDE Setup

Format, cleanup and import ordering files are checked in to: 
https://github.com/triplea-game/triplea/tree/master/eclipse/format

## *Eclipse*
 Plugins:
  - [Buildship](https://github.com/eclipse/buildship/blob/master/docs/user/Installation.md)
  - [Gradle integration](https://marketplace.eclipse.org/content/buildship-gradle-integration)

Useful:
  - [EGit in Eclipse](http://www.eclipse.org/egit/) - with [tutorial](http://www.vogella.com/tutorials/EclipseGit/article.html)

Lombok:
Install Plugin ([reference](https://projectlombok.org/downloads/lombok.jar)):
- Download 1.16.20 (Feb 24, 2018; check gradle build file for most recent) from: https://projectlombok.org/all-versions
- Execute it: `java -jar lombok.jar`
- If the installer does not work, there is a manual install option: https://groups.google.com/forum/#!topic/project-lombok/3rVS0eXVl5U
- On version upgrades these steps may need to be repeated
Enable annotation processing: 
- https://stackoverflow.com/questions/43404891/how-to-configure-java-annotation-processors-in-eclipse

  
Checkstyle:
 
The [Eclipse Checkstyle plugin](http://eclipse-cs.sourceforge.net) integrates Checkstyle into the Eclipse IDE and notifies you of violations in real time.
Installation instructions are available at the above website.  You should install the version of the plugin that matches the Checkstyle version used in the Gradle build (see the `checkstyle` configuration in `build.gradle`).

1. Open the Properties window for the TripleA project
    1. From the main menu: **Project > Properties**
    1. From the Package Explorer: Right click the project and select **Properties**
1. Select the **Checkstyle** node
1. Select the **Main** tab
1. Ensure the **Checkstyle active for this project** and **Use simple configuration** boxes are checked
1. Under **Simple** select the **TripleA** configuration you added in the workspace Preferences window above
1. Click **OK** to close the Properties window
1. Answer **Yes** to rebuild the project


## *Netbeans*
  - [Gradle plugin](http://plugins.netbeans.org/plugin/44510/gradle-support)

## *IntelliJ*

Plugins:
  - *Eclipse Code Formatter* 
    - configure in settings to pick up the eclipse xml formatter and import ordering files.
  - *checkstyle-IDEA* [plugin](https://github.com/jshiell/checkstyle-idea) 
    - after install finish configuration in: **Other Settings > Checkstyle** 
  - *Save Actions*
    - configure in settings to add 'final' to local variables and class variables.
  - *Lombok*
  - Method ordering: ![keep_dependents_first](https://user-images.githubusercontent.com/12397753/27557429-72fb899c-5a6e-11e7-8f9f-59cc508ba86c.png)

Lombok:
 - requires annotation processing to be turned on (settings > 'annotation processing')

# Docker

## Installation

### Mac

https://store.docker.com/editions/community/docker-ce-desktop-mac

## Project-specific images

The following project-specific Docker images, which may be useful during development and testing, are available:

  - [Lobby database](https://github.com/triplea-game/triplea/tree/master/lobby-db/Dockerfile)

# Team Communication

*Github Issues*: https://github.com/triplea-game/triplea/issues
- Primary means of communication, conversations are open and recorded for historical reference.
- Open, allows for team participation and task balancing

*Gitter*: https://gitter.im/triplea-game/social

Quick 1:1 messages, administrative informational broadcast messages. 
For example: "Restarting bot 15, looks to be on fire"
  

*Forum*: http://forums.triplea-game.org

Communication to the larger TripleA community. Feature and game changes discussions would belong here.


# Code Review 

### PR Guidelines
- describe functional changes
- describe what was tested
- describe refactoring that was done
- if UI changes were made, include screen shots giving an overview of how things look
- code contributors should test their updates quite thoroughly

### Code reviewer expectation:
- shall not "self-review" and "self-merge" their own work 

### Requirements for someone to have write access
- contribute some cod
- provide valuable code reviews
- sustained engagement over at least a few months

### PR Merge Guidelines

At least one code reviewer must:
- try out any functional changes
- do a thorough code check
Before merge:
- all comments questions should be answered/addressed

### Code Review Ordering
- Reviewers should start with the oldest PRs and work their way forward in time. Favor merging things in that order as well. The reason for the merge order is to make merge conflicts a bit more predictable, if you are the first PR in the queue, then there should be no merge conflicts. If 2nd, then in theory you would only have to worry about the one open PR request to cause merge conflicts.


# Versioning 

Simplified 3 number versioning system based on semantic versioning:

 
``` release.compatability.build_number```


**Release** - A version number used by the TripleA team for version partitioning. 
**compatability** - Incremented whenever backwards compatibility with previous version is lost
**Build** - auto-generated by the build system.


* Last discussed in: https://github.com/triplea-game/triplea/issues/1739

# Troubleshooting

Response to a frozen game problem, below are steps notes of what would be needed to debug such a problem:

  - launch the game from a console window
  - recreate the game freeze.
  - In a second console, "`kill -3`" the process id of the running game. As a one line that would be:
     - `ps -ef | grep java | grep GameRunner | awk '{print $2}' | xargs kill -3`
  - Go back to the first console, a lot of new text should have shown up. Kill the game (ctrl+c), the file: "`output.txt`" will have the debug data we want.

# [Checkstyle](http://checkstyle.sourceforge.net)

Build will fail if checkstyle violation is increased.


## Gradle

Checkstyle can be run with:

```
$ ./gradlew clean check
```

To run individually:

```
$ ./gradlew clean checkstyleMain checkstyleTest checkstyleIntegTest
```

Checkstyle reports can be found within the folder `build/reports/checkstyle`.

You are **strongly encouraged** to run the `check` task before submitting a PR.

## How to upgrade gradle

- Gradle distributions can be found at: https://services.gradle.org/distributions/ (we use a '-all' version)
- Update the gradle version in: https://github.com/triplea-game/triplea/blob/master/gradle/wrapper/gradle-wrapper.properties
- Execute: `./gradlew wrapper`
- Change the gradle zip back to '-all' from '-bin' in gradle-wrapper.properties
- Commit everything
- Do some smoke testing and submit a PR


# Code Standards

Unless specified otherwise, follow: [Google java style](http://google.github.io/styleguide/javaguide.html)

Project uses checkstyle which must pass for any code to be merged.


## Guidelines and Preferences

### Avoid using `null` as part of public APIs

Said another way, do not pass `null` arguments, and do not return null. 

### Deprecate Correctly
To deprecate add both a `@Deprecated` annotation  _and_ a `@deprecated` documentation 
in the javadocs. Include a comment on what can be done to avoid the deprecated call.

Example:
```
/**
 * @Deprecated Use 'fooBar()' instead
 */
 @deprecated
 public void foo(int param) {
 :
 :
```


## Variable and Method Ordering

Depth first ordering for methods according to call stack.
For more details, please see Chapter 5 'Formatting' in [Clean Code](http://ricardogeek.com/docs/clean_code.html)

Example:
```java

public void method1() {
  callPrivate1();
  callPrivate2();
}

private void callPrivate1() { }
private void callPrivate2() { }

public void method2() { }
```

Note:
 - vertical distance between first usage and declaration is minimized. 
 - checkstyle will require method overloads to be declared next to each other, that
  is an exception enforced by checkstyle.


## Variables
Define variables as close to their usage as possible.

## Using Lombok
- Use narrow scopes, if you don't need public getters, set them to the most restrictive scope possible, eg:
```@Getter -> @Getter(access = AccessLevel.PACKAGE)```
- Avoid `@Data`, it is a mutable data structure, avoid the mutability when you can.
- Beware of circular loops with @ToString and @EqualsAndHashcode, if two classes have a circular dependency, and then you toString that, you'll get a loop. The insidious part is you only get this when `toString` is actually called, that may only be in for example a logging statement or on error.
