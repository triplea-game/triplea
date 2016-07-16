package games.strategy.engine.framework;

import javax.swing.JOptionPane;

import games.strategy.performance.PerfTimer;

/**
 * This class starts and runs the game.
 * <p>
 * This class is compiled to run under older jdks (1.3 at least), and should not do anything more than check the java
 * version
 * number, and then delegate to GameRunner2
 * <p>
 */
public class GameRunner {
  public static boolean isWindows() {
    return System.getProperties().getProperty("os.name").toLowerCase().contains("windows");
  }

  public static boolean isMac() {
    return System.getProperties().getProperty("os.name").toLowerCase().contains("mac");
  }

  /**
   * Get version number of Java VM.
   */
  private static void checkJavaVersion() {
    // note - this method should not use any new language features (this includes string concatention using +
    // since this method must run on older vms.
    final String version = System.getProperties().getProperty("java.version");
    final boolean v12 = version.contains("1.2");
    final boolean v13 = version.contains("1.3");
    final boolean v14 = version.contains("1.4");
    final boolean v15 = version.contains("1.5");
    if (v15 || v14 || v13 || v12) {
      JOptionPane.showMessageDialog(null,
          "TripleA requires a java runtime greater than or equal to 8 [ie: Java 8]. \nPlease download a newer version of java from http://www.java.com/",
          "ERROR", JOptionPane.ERROR_MESSAGE);
      System.exit(-1);
    }
  }

  public static void main(final String[] args) {
    PerfTimer timer = PerfTimer.startTimer("GameRunner1 Total Launch");
    // we want this class to be executable in older jvm's
    // since we require jdk 1.8, this class delegates to GameRunner2
    // and all we do is check the java version
    new Thread(() -> checkJavaVersion()).start();
    // do the other interesting stuff here
    GameRunner2.main(args);
    timer.stop();
  }
}
