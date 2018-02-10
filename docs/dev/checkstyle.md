
This project uses [Checkstyle](http://checkstyle.sourceforge.net) to enforce code guidelines and standards.  The build is configured to fail under the following conditions:

* The error count is greater than zero
* The net warning count increases over the current baseline on `master`

The following sections describe how to use Checkstyle in various environments.

## Gradle

During a Gradle build, Checkstyle runs as part of the `check` task:

```
$ ./gradlew clean check
```

To run Checkstyle on its own, use the `checkstyleMain`, `checkstyleTest`, and `checkstyleIntegTest` tasks:

```
$ ./gradlew clean checkstyleMain checkstyleTest checkstyleIntegTest
```

Checkstyle will generate HTML and XML reports for the main, unit test, and integration test code.  These reports can be found within the folder `build/reports/checkstyle`.

You are **strongly encouraged** to run the `check` task before submitting a PR to avoid adding unnecessary commits for Checkstyle violations you may have introduced.

## Eclipse

The [Eclipse Checkstyle plugin](http://eclipse-cs.sourceforge.net) integrates Checkstyle into the Eclipse IDE and notifies you of violations in real time.
Installation instructions are available at the above website.  You should install the version of the plugin that matches the Checkstyle version used in the Gradle build (see the `checkstyle` configuration in `build.gradle`).

Once installed, configure your TripleA workspace as follows:

1. Open the Preferences window from the main menu: **Window > Preferences**
1. Select the **Checkstyle** node
1. Under **Global Check Configurations** select **New...** to open the Check Configuration Properties window
1. Under **Type** select **Project Relative Configuration**
1. Under **Name** enter **TripleA**
1. Under **Location** click the **Browse...** button and select the file `/config/checkstyle/checkstyle.xml` relative to the project root
1. Click **OK** to close the Check Configuration Properties window
1. Click **OK** to close the Preferences window

Finally, configure the TripleA project as follows:

