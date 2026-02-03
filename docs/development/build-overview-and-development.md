# Build Overview

Currently, the build uses a mix of Gradle tasks and bash scripts.
Gradle plugins used by the build are located in `/gradle/build-logic`.

## Convention Plugins

The TripleA build defines Gradle [Convention Plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html#header) to avoid cross-project configuration and duplication of configuration.
There are currently the following types of projects:

### `triplea-java-library`

This is a standard "vanilla" java library type.
It applies the `java-library` plugin and applies universal configuration, code conventions, and sets up static analysis. 

# Future Work

To continue to improve build speeds and make the build structure more idiomatic, some near future work should:

- Convert all build scripts to Kotlin.
This will make the build easier to maintain by increasing the completion and refactoring assistance the IDE is able to provide.
- Remove the use of `subprojects` and `allprojects` and replace these with Gradle [Convention Plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html#header).
This will make the build easier to maintain by avoiding the pitfalls of cross-project configuration, it will prevent difficulties updating to future Gradle versions, and it will prepare the build to take advantage of future Gradle features like [Isolated Projects](https://docs.gradle.org/current/userguide/isolated_projects.html#header) that will further increase build speed.
- Avoid creating empty projects in folders such as `game-app`.
This slows and complicates the build unnecessarily.
- Remove the need for the `version.gradle` script by moving these tasks to a single proper locations and wiring them with task dependencies to only run when needed.
- Remove the need for bash scripts like those in `/game-app/run/` or `/verify` that exist only to launch Gradle builds with Gradle lifecycle tasks. 
- Improve Dependency hygiene by enabling Gradle Dependabot alerts on GitHub, pruning unused project dependencies, properly using the `api` and `implementation` configurations to export dependencies only when necessary, etc. 
- Properly implement [Test Fixtures](https://docs.gradle.org/current/userguide/java_testing.html#producing_and_using_test_fixtures_within_a_single_project) for sharing common testing code between projects, such as in `:game-app:ai` and in `:lib:test-common`.
- Remove all/most configuration from the root `build.gradle(.kts)` file.
- Describe projects using the `description` field, especially any unique components of a project's build.
- Describe all tasks using the `description` and `group` field, use custom task types rather than ad-hoc tasks.
- Define the necessary Java version using [JVM Toolchains](https://docs.gradle.org/current/userguide/toolchains.html#sec:consuming) and daemon toolchains.
This will remove the need for any specific version of Java to be installed to run the build.
- Conform to other Gradle [Best Practices](https://docs.gradle.org/current/userguide/best_practices.html).

The end result of this should be a simpler more comprehensible build structure, faster builds, automatic avoidance of unnecessary work, quicker detection of improper configuration (such as unnecessary dependencies), and easy upgrades to future Gradle versions.
