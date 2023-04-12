# Eclipse Setup

**These instructions may be out of date!!!**

**If you use eclipse, please update this document**

##  Plugins:
  - ***Buildship Gradle Integration***
     1. [Install Plugin](https://marketplace.eclipse.org/content/buildship-gradle-integration)
  - ***Checkstyle***
     1. [Install Plugin](http://eclipse-cs.sourceforge.net)
     1. Follow: https://checkstyle.org/eclipse-cs/#!/project-setup
     1. After step 3 "Activate Checkstyle for your project", select the TripleA configuration file
      instead of "Sun Checks". Checkstyle file: [triplea/.build/checkstyle.xml
     ](https://github.com/triplea-game/triplea/blob/master/.build/checkstyle.xml)
  - ***Lombok***
    1. Download [jar file](https://projectlombok.org/downloads/lombok.jar) and execute it: `java -jar lombok.jar`
       - [manual install](https://groups.google.com/forum/#!topic/project-lombok/3rVS0eXVl5U)
         if that does not work
       - Version to use: Same version as Gradle (see `lombok` configuration in the top-level [Gradle buildscript](https://github.com/triplea-game/triplea/blob/master/build.gradle))
    2. Enable annotation processing: [how-to-configure-java-annotation-processors-in-eclipse
      ](https://stackoverflow.com/questions/43404891/how-to-configure-java-annotation-processors-in-eclipse)

## Formatter
1. [Install Google Java Format Plugin](https://github.com/google/google-java-format#eclipse)
   * ***Important*** Use the unofficial 1.7 version available here: https://github.com/google/google-java-format/files/2774507/google-java-format-eclipse-plugin-1.7.0.jar.zip
   * Download the 1.7 version, unzip it, and move the extracted 'jar' file to the eclipse 'dropins' folder.
1. *Project > Java Code Style > Formatter*
   1. Import profile, select the Triplea formatter file: [triplea/.build/eclipse/triplea-eclipse-java-google-style.xml
     ](https://github.com/triplea-game/triplea/blob/master/.build/eclipse/triplea-eclipse-java-google-style.xml)
   1. Change Formatter implementation to 'google-java-format'
![Screenshot from 2019-08-08 16-46-41
](https://user-images.githubusercontent.com/12397753/62744601-224ed680-b9fc-11e9-8922-df3564c4207f.png)

## Cleanup
1. *Project > Java Code Style > Cleanup*
1. Select the Triplea cleanup file: [triplea/.build/eclipse/triplea_java_eclipse_cleanup.xml
   ](https://github.com/triplea-game/triplea/blob/master/.build/eclipse/triplea_java_eclipse_cleanup.xml)

![screenshot from 2019-01-10 15-07-46
  ](https://user-images.githubusercontent.com/12397753/51002909-acc46b80-14e9-11e9-8a49-80281769f81a.png)

## Import Order
1. *Project > Java Code Style > Organize Imports*
1. Click 'Import' and select the import file: [triplea/.build/eclipse/triplea.importorder
](https://github.com/triplea-game/triplea/blob/master/.build/eclipse/triplea.importorder)

Should look like this when configured: <br />
![Screenshot from 2019-08-08 16-45-04
](https://user-images.githubusercontent.com/12397753/62744560-e7e53980-b9fb-11e9-815c-2a6432d77e42.png)

## Useful:
  - [EGit in Eclipse](http://www.eclipse.org/egit/) - with [tutorial
      ](http://www.vogella.com/tutorials/EclipseGit/article.html)
  - Run Configurations are checked in
  - Run `./gradlew downloadAssets` one time to get images downloaded

