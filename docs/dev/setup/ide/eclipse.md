## *Eclipse* Setup
 Plugins:
  - [Buildship](https://github.com/eclipse/buildship/blob/master/docs/user/Installation.md)
  - [Gradle integration](https://marketplace.eclipse.org/content/buildship-gradle-integration)

Useful:
  - [EGit in Eclipse](http://www.eclipse.org/egit/) - with [tutorial](http://www.vogella.com/tutorials/EclipseGit/article.html)

Lombok:
Install Plugin ([reference](https://projectlombok.org/downloads/lombok.jar)):
- Download 1.16.20 (Feb 24, 2018; check gradle build file for most recent) from: https://projectlombok.org/all-versions
- Execute it: `java -jar lombok.jar`
- If the installer does not work, there is a manual install option: https://groups.google.com/forum/#!topic/project-lombok/3rVS0eXVl5U
- On version upgrades these steps may need to be repeated
Enable annotation processing: 
- https://stackoverflow.com/questions/43404891/how-to-configure-java-annotation-processors-in-eclipse

  
Checkstyle:
 
The [Eclipse Checkstyle plugin](http://eclipse-cs.sourceforge.net) integrates Checkstyle into the Eclipse IDE and notifies you of violations in real time.
Installation instructions are available at the above website.  You should install the version of the plugin that matches the Checkstyle version used in the Gradle build (see the `checkstyle` configuration in `build.gradle`).

1. Open the Properties window for the TripleA project
    1. From the main menu: **Project > Properties**
    1. From the Package Explorer: Right click the project and select **Properties**
1. Select the **Checkstyle** node
1. Select the **Main** tab
1. Ensure the **Checkstyle active for this project** and **Use simple configuration** boxes are checked
1. Under **Simple** select the **TripleA** configuration you added in the workspace Preferences window above
1. Click **OK** to close the Properties window
1. Answer **Yes** to rebuild the project
