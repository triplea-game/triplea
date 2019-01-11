# *Eclipse* Setup


##  Plugins:
  - ***Buildship Gradle Integration***
     1. [Install Plugin](https://marketplace.eclipse.org/content/buildship-gradle-integration)
  - ***Checkstyle***
     1. [Install Plugin](http://eclipse-cs.sourceforge.net)
     1. Follow: https://checkstyle.org/eclipse-cs/#!/project-setup
     1. After step 3 "Activate Checkstyle for your project", select the TripleA configuration file instead of "Sun Checks"
  - ***Lombok*** 
    1. Download [jar file](https://projectlombok.org/downloads/lombok.jar) and execute it: `java -jar lombok.jar`
       - [manual install](https://groups.google.com/forum/#!topic/project-lombok/3rVS0eXVl5U)
         if that does not work
       - Version to use: 1.16.20 
    2. Enable annotation processing: [how-to-configure-java-annotation-processors-in-eclipse
      ](https://stackoverflow.com/questions/43404891/how-to-configure-java-annotation-processors-in-eclipse)

## Formatter
1. *Project > Java Code Style > Formatter*
1. Select the Triplea formatter file: [triplea/.eclipse/format/triplea_java_eclipse_format_style.xml
   ](https://github.com/triplea-game/triplea/blob/master/.eclipse/format/triplea_java_eclipse_format_style.xml)
    ![screenshot from 2019-01-10 15-04-53](https://user-images.githubusercontent.com/12397753/51002741-1e4fea00-14e9-11e9-86b6-4314abfb1fcd.png)

## Cleanup

1. *Project > Java Code Style > Cleanup*
1. Select the Triplea cleanup file: [triplea/.eclipse/format/triplea_java_eclipse_format_style.xml
   ](https://github.com/triplea-game/triplea/blob/master/.eclipse/format/triplea_java_eclipse_format_style.xml)

![screenshot from 2019-01-10 15-07-46
  ](https://user-images.githubusercontent.com/12397753/51002909-acc46b80-14e9-11e9-8a49-80281769f81a.png)


## Import Order


1. *Project > Java Code Style > Organize Imports*
1. Click 'Import' and select the import file: [triplea/.eclipse/format/triplea.importorder
](https://github.com/triplea-game/triplea/blob/master/.eclipse/format/triplea.importorder)

Should look like this when configured: <br />
![screenshot from 2019-01-10 15-10-27
](https://user-images.githubusercontent.com/12397753/51002992-e7c69f00-14e9-11e9-9076-d05c4b6ce449.png)




## Useful:
  - [EGit in Eclipse](http://www.eclipse.org/egit/) - with [tutorial
      ](http://www.vogella.com/tutorials/EclipseGit/article.html)
  - Run Configurations are checked in
 

