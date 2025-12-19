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
