/*
  This convention defines a standard TripleA java library project.
  It applies the `java-library` plugin and applies universal configuration, code conventions, and sets up static analysis.
*/

plugins {
    `java-library`
    id("com.diffplug.spotless")
}

spotless {
    format("allFiles") {
        target("*")
        targetExclude("gradlew.bat")
        endWithNewline()
        leadingTabsToSpaces()
        trimTrailingWhitespace()
    }

    java {
        googleJavaFormat()
        removeUnusedImports()
    }
}
