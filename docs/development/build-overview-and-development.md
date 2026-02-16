# Build Overview

Currently, the build uses a mix of Gradle tasks and bash scripts.
Gradle plugins used by the build are located in `/gradle/build-logic`.

The build uses the Gradle Kotlin DSL. 
This make the build easier to maintain by increasing the completion and refactoring assistance the IDE is able to provide.

## Build Structure

By explicitly specifying the physical location of nested subprojects in the root `settings.gradle.kts` file, the build is able to avoid [unintentionally creating empty projects](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html#avoid_empty_projects).
These empty projects slow the build and make it more difficult to understand the project structure.
This allows you to reference projects using non-hierarchical names, for example `:game-core` instead of `:game-app:game-core`.

## Convention Plugins

The TripleA build defines Gradle [Convention Plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html#header) to avoid cross-project configuration and duplication of configuration.
There are currently the following types of projects:

### `triplea-java-library`

This is a standard "vanilla" java library type.
It applies the `java-library` plugin and applies universal configuration, code conventions, and sets up static analysis. 

## Test Fixtures

The `:game-core` project exposes [Test Fixtures](https://docs.gradle.org/current/userguide/java_testing.html#producing_and_using_test_fixtures_within_a_single_project) to share common testing code and resources between projects.
Other projects (like `:ai`) can access these fixtures to use during testing by adding a dependency like `testImplementation(testFixtures(project(":game-core")))`.

The fixture in `:game-core` includes map data present in `/game-app/game-core/src/testFixtures/resources`, that can be loaded by tests via the `TestMapGameDataLoader` class and the `TestMapGameData` enum.

# Future Work

To continue to improve build speeds and make the build structure more idiomatic, some near future work should:

- Remove the use of `subprojects` and `allprojects` and replace these with Gradle [Convention Plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html#header).
This will make the build easier to maintain by avoiding the pitfalls of cross-project configuration, it will prevent difficulties updating to future Gradle versions, and it will prepare the build to take advantage of future Gradle features like [Isolated Projects](https://docs.gradle.org/current/userguide/isolated_projects.html#header) that will further increase build speed.
- Remove the need for bash scripts like those in `/game-app/run/` or `/verify` that exist only to launch Gradle builds with Gradle lifecycle tasks. 
- Improve Dependency hygiene by enabling Gradle Dependabot alerts on GitHub, pruning unused project dependencies, properly using the `api` and `implementation` configurations to export dependencies only when necessary, etc.
- Remove all/most configuration from the root `build.gradle(.kts)` file.
- Describe projects using the `description` field, especially any unique components of a project's build.
- Describe all tasks using the `description` and `group` field, use custom task types rather than ad-hoc tasks.
- Define the necessary Java version using [JVM Toolchains](https://docs.gradle.org/current/userguide/toolchains.html#sec:consuming) and daemon toolchains.
This will remove the need for any specific version of Java to be installed to run the build.
- Conform to other Gradle [Best Practices](https://docs.gradle.org/current/userguide/best_practices.html).

The end result of this should be a simpler more comprehensible build structure, faster builds, automatic avoidance of unnecessary work, quicker detection of improper configuration (such as unnecessary dependencies), and easy upgrades to future Gradle versions.
