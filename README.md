Build system
============

[Gradle](www.gradle.org) is used. The gradlew (on Windows gradlew.bat) file is a proxy to execute build commands. 
On first call these files will install the correct version of Gradle on your system. Most commonly used commands:

* creates a jar file from the project, dependencies are not added:
```
./gradlew jar
# creates into build/libs/triplea-<version>.jar
```

* creates a self contained jar file from the project, all JAR dependencies are included
```
./gradlew shadowJar
# creates into build/libs/triplea-<version>-all.jar
```
* run the application right from the source (no jar is created); this may be used from inside an IDE to debug
```
./gradlew run
# creates into build/libs/triplea-<version>-all.jar
```
  
Tooling and IDE setup
=====================
 
With the help of the Gradle system any modern IDE support is provided out of box, such as:

* Eclipse - use the [Buildship plugin](https://github.com/eclipse/buildship/blob/master/docs/user/Installation.md)
* Intellij IDEA - out of box integration support (just import project, and specify the settings.gradle file)
* Netbeans - use the [Gradle plugin](http://plugins.netbeans.org/plugin/44510/gradle-support)
 