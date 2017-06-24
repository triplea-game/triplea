---
layout: longpage
title: Checkstyle
permalink: /dev_docs/dev/checkstyle/
---

This project uses [Checkstyle](http://checkstyle.sourceforge.net) to enforce code guidelines and standards.  The build is configured to fail under the following conditions:

* The error count is greater than zero
* The net warning count increases over the current baseline on `master`

The following sections describe how to use Checkstyle in various environments.

## Gradle

During a Gradle build, Checkstyle runs as part of the `check` task:

```
$ ./gradlew clean check
```

To run Checkstyle on its own, use the `checkstyleMain` and `checkstyleTest` tasks:

```
$ ./gradlew clean checkstyleMain checkstyleTest
```

Checkstyle will generate HTML and XML reports for both the main and test code.  These reports can be found within the folder `build/reports/checkstyle`.

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

1. Open the Properties window for the TripleA project
    1. From the main menu: **Project > Properties**
    1. From the Package Explorer: Right click the project and select **Properties**
1. Select the **Checkstyle** node
1. Select the **Main** tab
1. Ensure the **Checkstyle active for this project** and **Use simple configuration** boxes are checked
1. Under **Simple** select the **TripleA** configuration you added in the workspace Preferences window above
1. Click **OK** to close the Properties window
1. Answer **Yes** to rebuild the project

## IntelliJ

The [CheckStyle-IDEA plugin](https://github.com/jshiell/checkstyle-idea) integrates Checkstyle into the IntelliJ IDE and notifies you of violations in real time.

Installation is performed through the IntelliJ plugin settings:

1. Open the Settings window from the main menu: **File > Settings...**
1. Select the **Plugins** node
1. Click **Browse Repositories...** to open the Browse Repositories window
1. Enter **checkstyle** in the search box
1. Select **CheckStyle-IDEA**
1. Click **Install**
1. Wait for the plugin to be installed
1. Click **Restart IntelliJ IDEA** to close the Browse Repositories window
1. Click **OK** to close the Settings window
1. Answer **Restart** when prompted to restart IntelliJ

Configure the TripleA project as follows:

1. Open the Settings window from the main menu: **File > Settings...**
1. Select the **Other Settings > Checkstyle** node
1. Change **Checkstyle version** to match the version used in the Gradle build (see the `checkstyle` configuration in `build.gradle`)
1. Change **Scan Scope** to **All sources (including tests)**
1. Under **Configuration File** click the **Add** (+) button to open the Configuration File window
1. Under **Description** enter **TripleA**
1. Select **Use a local Checkstyle file**
1. Under **File** click the **Browse** button and select the file `/config/checkstyle/checkstyle.xml` relative to the repo root
1. Click **Next**
1. Click **Finish** to close the Configuration File window
1. Make the **TripleA** configuration file added above the active configuration
1. Click **OK** to close the Settings window

---

Up to: [Dev Documentation]({{ "/dev_docs/dev/" | prepend: site.baseurl }})
