# gradle/build-logic

Custom Gradle convention plugins that standardize project configuration across the build.
This is a [composite build](https://docs.gradle.org/current/userguide/composite_builds.html) included via `settings.gradle.kts`.

## Subprojects

### triplea-java-library

Pre-compiled script plugin (`triplea-java-library.gradle.kts`) applied by nearly every subproject via `plugins { id("triplea-java-library") }`. It provides:

- **Java 21** source/target compatibility
- **Spotless** formatting: Google Java Format, unused import removal, trailing whitespace cleanup, leading tabs → spaces, newline at EOF
- Sets `group = "triplea"`

When adding a new Java subproject, apply this plugin instead of configuring `java-library` and Spotless manually.

### failure-summary-plugin

Settings plugin (`org.triplea.failure-summary-plugin`) applied in the root `settings.gradle.kts`. It collects test failures across all subprojects during a build and prints a sorted summary at the end.

Key classes (in `org.triplea.build.plugins`):

| Class | Role |
|---|---|
| `FailureSummaryPlugin` | Settings plugin entry point; registers `FailedTestsService`, attaches `TestFailureLoggingListener` to every `Test` task, schedules `FailureReporter` via `FlowScope` |
| `FailedTestsService` | `BuildService` holding a `MapProperty<String, List<String>>` of project name → failed test names |
| `TestFailureLoggingListener` | `TestListener` that records failures into `FailedTestsService` |
| `FailureReporter` | `FlowAction` that prints the failure map sorted by project at build end |

## Build Configuration

- `settings.gradle.kts` — declares both subprojects and imports the version catalog from `gradle/libs.versions.toml`
- Repository resolution is set to `FAIL_ON_PROJECT_REPOS` — all dependencies must come from `gradlePluginPortal()` or `mavenCentral()`
