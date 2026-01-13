plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.spotless)
}

description = """This project creates Gradle pre-compiled script plugins that define conventional project types used in the build.  
This avoids the need for `subprojects` and `allprojects`, and keeps project configuration local."""
