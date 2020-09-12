# How to Upgrade Gradle

- Execute: `./gradlew wrapper --gradle-version=<version>`, where `<version>` is replaced with
the desired Gradle version (e.g. `4.8.1`, `4.9`, etc.).
- Verify _gradle/wrapper/gradle-wrapper.jar_ has been modified. If not, re-run the previous command.
- Commit everything.
- Do some smoke testing and submit a PR.

