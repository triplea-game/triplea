# *IntelliJ* Setup

## Tasks to be performed

- Open the TripleA project from source code
- Install plugins, configure associated settings
- Download and import IDEA settings file

Approximate time to complete: 10 - 30 minutes

### Open Project
- In IDEA, **File > Open** and select the build.gradle file that is located at
  the top level of the project. (**Tip**: by selecting gradle file, IDEA will
  preconfigure the project as a gradle project)

### Install Plugins

Note: **Settings** menu is accessible via `File > Settings...`  or (on Mac OS): `IntelliJ IDEA > Preferences`

Plugin installation can be initiated from the JetBrains Marketplace web page, by clicking "Install to IDE" link from each plugin's page.
  1. *[Google Java Format Plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format)*
        1. **Settings > Other Settings > google-java-format Settings**
        1.  Check "Enable google-java-format"
      ![Screenshot from 2019-08-08 17-35-52
      ](https://user-images.githubusercontent.com/12397753/62746114-07cc2b80-ba03-11e9-9ac0-0b1e6e1e8788.png)
  1. *[Checkstyle IDEA Plugin](https://github.com/jshiell/checkstyle-idea)*
        1. After installation finish configuration in: **Settings > Tools > Checkstyle**
            1. Load checkstyle file by clicking on the "plus" and navigating to the file .\IdeaProjects\triplea\config\checkstyle (If you can't find it, you can download it from [the repository](https://github.com/triplea-game/triplea/blob/master/config/checkstyle/checkstyle.xml))
            1. Set checkstyle version
            1. Set to scan all sources
      ![Screenshot from 2020-10-18 19-18-46
      ](https://user-images.githubusercontent.com/12397753/96394543-271e2700-1177-11eb-9460-24e2e235d60d.png)
  1. *'final' for generated Variables (optional)*
        1. **Settings > Editor > Code Style > Java** 
        2. Go to tab "Code Generation" subsection "Final Modifier"
            1. Check "Make generated local variables final"
            2. Check "Make generated parameters final"
![Screenshot_IntelliJ_MakeFinal_Generated](https://user-images.githubusercontent.com/10353640/147354665-a2a96231-86f8-4b6f-97b0-e4c6f729cdd0.png)
  1. *Save Actions (optional)*
        1. **Settings > Editor > Inspections**
            1. **Java > Code style issues**
            2. Check "Local variable or parameter can be final"
![Screenshot_IntelliJ_MakeFinal](https://user-images.githubusercontent.com/10353640/147356129-3f3e4877-43de-49a7-ab5b-73c4363f8b45.png)
        2. **Settings > Tool > Actions on Save**
            1. Check "Reformat code" (adapt to be only affected on "Changed lines")
            2. Check "Optimize imports"
            3. Check "Run code cleanup"
![Screenshot_IntelliJ_ActionsOnSave](https://user-images.githubusercontent.com/10353640/147353953-2e6f161f-0d9f-4912-91b6-0aa200d2d126.png)
  1. *Lombok*
        1. **Settings > Annotation Processors**
            1. Check "Enable annotation processing"
        ![annotationprocessing2](https://user-images.githubusercontent.com/54828470/95939758-6da00a00-0da2-11eb-9c7a-823040578c4e.png)

### Configure IDEA Settings
  1. **File > Manage IDE Settings > Import Settings**
  1. Select file: [.ide-intellij/intellij-settings.zip
   ](https://github.com/triplea-game/triplea/blob/master/.ide-intellij/intellij-settings.zip)
