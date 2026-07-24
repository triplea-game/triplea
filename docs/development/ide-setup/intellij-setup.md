# *IntelliJ* Setup

## Tasks to be performed

- open the TripleA project from GitHub source code (path to your fork like `https://github.com/<username>/triplea`)
- install plugins, configure associated settings
- download and import IDEA settings file

Approximate time to complete: 10 - 30 minutes

### Open Project

- In IDEA, `File > Open...`  and select the build.gradle file that is located at
  the top level of the project (example `\\wsl.localhost\Ubuntu\home\<your user>\triplea`) and run it. (**Tip** by
  selecting Gradle file, IDEA will
  preconfigure the project as a Gradle project)
- Ensure to have the right project settings for the Java SDK (`File > Project Structure...` and
  `Project Settings > Project` ) and the bytecode version (`File > Settings...` and
  `Build, Execution, Deployment > Compiler > Java Compiler`)
- Ensure to have the same SDK selected in the settings as Gradle JVM (`File > Settings...` and
  `Build, Execution, Deployment > Build Tools > Gradle`)
- Note that the JVM version used for the Gradle command (you can view in the terminal via `./gradlew -version`) is using
  the operating system's JVM and therefore the environment variable `JAVA_HOME`. Hence, make sure the version to which
  this points match the selected JDK.

### Plugins:

Note: **Settings** menu is accessible via, `File > Settings...`  or (on macOS): `IntelliJ IDEA > Preferences`

Plugin installation can be initiated from the JetBrains Marketplace web page, by clicking "Install to IDE" link from
each plugin's page.

1. *google-java-format* [plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format)
    1. **Settings* > google-java-format Settings**
    2. Check 'enable'
      <img src="https://user-images.githubusercontent.com/12397753/62746114-07cc2b80-ba03-11e9-9ac0-0b1e6e1e8788.png" alt="Screenshot" height="200">
    4.
   Check [IntelliJ JRE config from google-java-format]([url](https://github.com/google/google-java-format/blob/master/README.md#intellij-jre-config))
   or do it manually as below.
2. [OUT-DATED] *checkstyle-IDEA* [plugin](https://github.com/jshiell/checkstyle-idea)
    1. after install finish configuration in: **Other Settings > Checkstyle**
        1. load checkstyle file by clicking on the "plus" and navigating to the file
           `.\triplea\config\checkstyle` (If you can't find it, you can download it from
           [the repository](https://github.com/triplea-game/triplea/blob/master/.build/checkstyle.xml))
        2. set checkstyle version
        3. set to scan all sources
3. *Lombok*
    1. **Settings > Annotation Processors**
    2. Turn on annotation processing.
       ![annotationprocessing2](https://user-images.githubusercontent.com/54828470/95939758-6da00a00-0da2-11eb-9c7a-823040578c4e.png)
4. *PlantUML Integration*
    1. GraphViz also needs to be installed: <https://graphviz.org/download/>

## Download and import IDEA settings file

Import IDEA settings file via `File > Manage IDE Settings > Import Settings...`.
*TODO: Identify file to be imported!*

## google-java-format Plugin Fix (if above settings do not work)

Find your IDEA vmoptions file with: `find ~ -name "*vmoptions"`
(eg: ./.config/JetBrains/IntelliJIdea2022.3/idea64.vmoptions)

Add the following to your vmoptions file:

```
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

### Further IDEA settings

Note: **Settings** menu is accessible via, `File > Settings...`  or (on macOS): `IntelliJ IDEA > Preferences`

1. No `*` imports and import order
Path: `Editor > Code Style > Java`  in  tab `Imports`
<img width="560" height="705" alt="image" src="https://github.com/user-attachments/assets/f646ec0a-4a33-42ee-a001-57844c62dea6" />

2. Default annotations `@NotNull` / `@Nullable`
Path: `Editor > Inspections > Revert changes`  in  tree entry `Java > Probably bugs > Nullability problems > @NotNull / @Nullable problems`
Button: `Configure Annotations ...`
- Nullable
<img width="656" height="201" alt="image" src="https://github.com/user-attachments/assets/9334e3b8-1e84-43e0-be89-a369b5c2d720" />

- NotNull
<img width="656" height="197" alt="image" src="https://github.com/user-attachments/assets/5227f499-39c4-444c-8d0f-24c5ea02bde1" />



