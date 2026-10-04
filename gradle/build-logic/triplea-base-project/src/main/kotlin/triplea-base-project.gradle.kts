plugins {
    id("triplea-test-conventions")
    pmd
}

// Absent while Gradle generates accessors for plugins that apply this one.
val libs = extensions.getByType<VersionCatalogsExtension>().find("libs").orElse(null)

pmd {
    isConsoleOutput = true
    ruleSetFiles = files(rootProject.file(".build/pmd.xml"))
    ruleSets = listOf()
    incrementalAnalysis = true
    libs?.let { toolVersion = it.findVersion("pmd").get().requiredVersion }
}
