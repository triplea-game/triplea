# *IntelliJ* Setup

## Tasks to be performed

- open the TripleA project from GitHub source code (path to your fork like `https://github.com/<username>/triplea`)
- install plugins, configure associated settings
- download and import IDEA settings file

Approximate time to complete: 10 - 30 minutes

### Open Project
- In IDEA, `File > Open...`  and select the build.gradle file that is located at
  the top level of the project and run it. (**Tip** by selecting gradle file, IDEA will
  preconfigure the project as a gradle project)
- Ensure to have the right project settings for the SDK (`File > Project Structure...` and `Project Settings > Project` ) and the bytecode version (`File > Settings...` and `Build, Execution, Deployment > Compiler > Java Compiler`)

### Plugins:

Note: **Settings** menu is accessible via, `File > Settings...`  or (on Mac OS): `IntelliJ IDEA > Preferences`

Plugin installation can be initiated from the JetBrains Marketplace web page, by clicking "Install to IDE" link from each plugin's page.
  1. *google-java-format* [plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format)
        1. **Settings* > google-java-format Settings**
        1.  Check 'enable'
      ![Screenshot from 2019-08-08 17-35-52
      ](https://user-images.githubusercontent.com/12397753/62746114-07cc2b80-ba03-11e9-9ac0-0b1e6e1e8788.png)*
        1.  Check [Intellij JRE config from google-java-format]([url](https://github.com/google/google-java-format/blob/master/README.md#intellij-jre-config)) or do it manually as below.
  1. *checkstyle-IDEA* [plugin](https://github.com/jshiell/checkstyle-idea)
        1. after install finish configuration in: **Other Settings > Checkstyle**
            1. load checkstyle file by clicking on the "plus" and navigating to the file
            `.\IdeaProjects\triplea\config\checkstyle` (If you can't find it, you can download it from
             [the repository](https://github.com/triplea-game/triplea/blob/master/.build/checkstyle.xml))
            1. set checkstyle version
            1. set to scan all sources
      ![Screenshot from 2020-10-18 19-18-46
      ](setupcheckstyle.png)
  1. *Lombok*
        1. **Settings > Annotation Processors**
        1. Turn on annotation processing.
        ![annotationprocessing2](https://user-images.githubusercontent.com/54828470/95939758-6da00a00-0da2-11eb-9c7a-823040578c4e.png)
  1. *PlantUML Integration*
        1. GraphViz also needs to be installed: <https://graphviz.org/download/>

## Download and import IDEA settings file

Import IDEA settings file via `File > Manage IDE Settings > Import Settings...`.
TODO: Identify file to be imported!

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

