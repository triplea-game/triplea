plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(project(":triplea-base-project"))
}

description = """This project creates a pre-compiled script plugin that defines a conventional application project used in the build.  
This avoids the need for `subprojects` and `allprojects`, and keeps project configuration local."""
