plugins {
    id("triplea-java-library")
}

dependencies {
    implementation(rootProject.libs.okhttp)
    implementation(project(":java-extras"))
    implementation(project(":lobby-client-data"))
}
