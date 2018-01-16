### Git

TripleA repositories may make use of [Git Large File Storage](https://git-lfs.github.com/) (LFS).  In order to work with such a repository, devs will need to have a Git LFS client installed.  Installation instructions for various platforms is available [here](https://github.com/git-lfs/git-lfs#getting-started).

### IDE Plugins

*Eclipse*
  - [Buildship plugin](https://github.com/eclipse/buildship/blob/master/docs/user/Installation.md)
  - [Gradle integration plugin](https://marketplace.eclipse.org/content/buildship-gradle-integration)

*Netbeans*
  - [Gradle plugin](http://plugins.netbeans.org/plugin/44510/gradle-support)

*IntelliJ*
  - Plugins:
    - Eclipse Code Formatter : configure in settings to pick up the eclipse xml formatter file.
    - checkstyle-IDEA : in settings select the triplea checkstyle file
    - Save Actions: configure in settings to at least add 'final' to local variables and class variables.

### Gradle - Building and Running the Code

Useful build commands:

```
./gradlew test
./gradlew run
```

## Set up IDE Formatter
See [code format](https://github.com/triplea-game/triplea/blob/master/docs/dev/code_format.md)

## Useful Tools

- SCM breeze, useful Git CLI helper: https://github.com/ndbroadbent/scm_breeze
- EGit in Eclipse: http://www.eclipse.org/egit/
  - more assistance can be found on http://www.vogella.com/tutorials/EclipseGit/article.html
