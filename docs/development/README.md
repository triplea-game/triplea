# Developer Setup Guide

## Before Getting Started
- Install JDK 21 (project is using this Java version)
- [Install IDE](./ide-setup) (IDEA is better supported, YMMV with Eclipse)

## Mac

- Install Docker Desktop: <https://store.docker.com/editions/community/docker-ce-desktop-mac>
  - we're going to need help on configs that will work for both Mac docker & linux docker. 
    The existing docker files are likely written with only linux in mind

## Windows

- Set up WSL (see [WSL installation guide](https://learn.microsoft.com/de-de/windows/wsl/install)), 
  this will give you a command line that can be used to run docker, gradle and the code check scripts
- Open git folder, e.g., `C:\Users\<user>\git\triplea`, in WSL via explorer `Shift+Right click` and option `Open Linux shell here`
- Install/upgrade [GitHub CLI](https://github.com/cli/cli#installation)

Install: 
```bash
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-key C99B11DEB97541F0
sudo apt-add-repository https://cli.github.com/packages
sudo apt update
sudo apt install gh
```
 
Upgrade:
```bash
sudo apt update
sudo apt upgrade gh
```

- Login to your GitHub account (e.g. via `HTTPS > Credentials > Login with a web browser`, copy URL to open window in browser and copy one-time code for device connection)
```bash
gh auth login
```
<img width="716" height="255" alt="image" src="https://github.com/user-attachments/assets/d79a9ada-930f-4eaa-993d-03344159e3d4" />

- Sync changes in your IDE with WSL
```bash
git status
```
- Declare repository `triplea-game/triplea` as your default to create PR to with WSL
```bash
gh repo set-default triplea-game/triplea
```

- (if wanted) declare start path for WSL by adjusting `.bashrc` or `.zshrc` inside WSL by adding at the end
`cd ~/projects/my-repo`

```bash
nano ~/.bashrc
```
 
## Getting Started

- Fork & Clone: <https://github.com/triplea-game/triplea>
- Use a feature-branch workflow, see: [typical git workflow](typical-git-workflow.md)
- Submit a pull request see: [pull requests process](../project/pull-requests.md)

If you are new to Open Source & GitHub:
  - https://docs.github.com/en/get-started/quickstart/contributing-to-projects
  - [Create an SSH Key](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account)
    (usually you will not need to add the SSH key to your keychain, just create it and add it to GitHub)

## Compile and launch TripleA (CLI)

```bash
# Build & Launch TripleA Game-Client
./gradlew :game-app:game-headed:run

# Run all build checks
./verify

# Run formatting
./gradlew spotlessApply

# Runs all tests
./gradlew test

# Run tests for a (sub)project
./gradlew :game-app:game-core:test

# Run a specific test
./gradlew :game-app:game-core:test --tests games.strategy.triplea.UnitUtilsTest

# Runs a specific test method
./gradlew :game-app:game-core:test --tests games.strategy.triplea.UnitUtilsTest.multipleTransportedUnitsAreTransferred

# Run specific tests using wildcard (be sure to use quotes around wildcard)
./gradlew :game-app:game-core:test --tests 'games.strategy.triplea.UnitUtilsTest.*Units*'
```

To run tests even if there are no changes from the previous build, use the `--rerun-tasks` option:
```
./gradlew --rerun-tasks :game-app:game-core:test
```

## Run Formatting

We use 'Google Java Format', be sure to install the plugin in your IDE to properly format from IDE.

To apply formatting via CLI:
```
./gradlew spotlessApply
```

## Code Conventions (Style Guide)

See: [reference/code-conventions](code-conventions)

## Lobby / Server Development

Look for the corresponding readmes, check: <https://github.com/triplea-game>


# Pitfalls and Pain Points to be aware of

## Save-Game Compatibility

- Do not rename private fields or delete private fields of anything that extends `GameDataComponent`
- Do not move class files (change package) of anything that extends `GameDataComponent`

The above are to protect save game compatibility.  Game saves are done via Java object serialization. The serialized
data is binary and written to file. Changing any object that was serialized to a game data file will prevent the
save games from loading.

## Network Compatibility

'@RemoteMethod' indicates methods invoked over network. The API of these methods may not change.

# FAQ - common problems

### Game crashes after splash screen displayed

This can be caused by missing resource files, such as images or icons.

The `run` Gradle task for `game-headed` will download and unzip game assets into the `game-headed` project's `/build/assests` directory.
This directory will then be processed as a main resource, by the `:game-app:game-headed:processResources` task, in order to be packaged in the resulting project jar.
When the game starts it expects to find the `assets` folder at the root of the classpath, so that load files in it using `getResourceAsStream()`.
Since this is a resource file packaged in the library jar produced by this project, the working dir should **not** influence how assets are loaded.

In short:
- Check that `./gradlew downloadAssets` has been run and there is a 'build/assets' folder present; and that the
contents have been copied to `build/resources/main/assets` (Gradle should handle this automatically as necessary).

### How do I view log files after the .exe crashes?

Navigate to the TripleA install directory and launch the jar manually using:

```bash
java -jar bin/game-headed-<SOME_VERSION_NUMBER>.jar
```

And you should see the log printed to the console.
