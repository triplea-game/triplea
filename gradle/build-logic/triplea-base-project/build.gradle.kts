plugins {
    `kotlin-dsl`
    checkstyle
    pmd
}

dependencies {
    implementation(project(":triplea-test-conventions"))
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:none,-processing")
    options.encoding = "UTF-8"
    options.setIncremental(true)
}

extensions.getByType<CheckstyleExtension>().apply {
    toolVersion = rootProject.libs.versions.checkstyle.get()
    configFile = rootProject.file(".build/checkstyle.xml")
    configProperties = mapOf("samedir" to configFile.parent)
}

tasks.named("checkstyleMain", Checkstyle::class.java) {
    maxWarnings = 0
    source(sourceSets.main.get().output.resourcesDir)
}

tasks.named("checkstyleTest", Checkstyle::class.java) {
    maxWarnings = 0
    source(sourceSets.test.get().output.resourcesDir)
    exclude("**/map-xmls/*.xml")
}

pmd {
    setConsoleOutput(true)
    ruleSetFiles = files(rootProject.file(".build/pmd.xml"))
    ruleSets = listOf()
    incrementalAnalysis = true
    toolVersion = rootProject.libs.versions.pmd.get()
}

description = """This project creates a pre-compiled script plugin that defines basic conventions used by all projects in the build.  
This avoids the need for `subprojects` and `allprojects`, and keeps project configuration local."""
