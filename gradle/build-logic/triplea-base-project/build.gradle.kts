plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(project(":triplea-test-conventions"))
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:none,-processing")
    options.encoding = "UTF-8"
    options.setIncremental(true)
}

description = """This project creates a pre-compiled script plugin that defines basic conventions used by all projects in the build.
This avoids the need for `subprojects` and `allprojects`, and keeps project configuration local."""
