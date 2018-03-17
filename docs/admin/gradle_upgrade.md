## How to upgrade gradle

- Gradle distributions can be found at: https://services.gradle.org/distributions/ (we use a '-all' version)
- Update the gradle version in: https://github.com/triplea-game/triplea/blob/master/gradle/wrapper/gradle-wrapper.properties
- Execute: `./gradlew wrapper`
- Change the gradle zip back to '-all' from '-bin' in gradle-wrapper.properties
- Commit everything
- Do some smoke testing and submit a PR
