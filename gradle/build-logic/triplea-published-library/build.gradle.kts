plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(project(":triplea-java-library"))
}

description = """This convention adds tasks to publish a project to Maven as a library."""
