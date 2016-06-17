package games.strategy.test;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Throwables;
import com.google.common.io.Files;

import games.strategy.common.swing.SwingAction;

public class TestUtil {

  /** Create and returns a simple delete on exit temp file with contents equal to the String parameter. */
  public static File createTempFile(String contents) {
    File file;
    try {
      file = File.createTempFile("testFile", ".tmp");
      file.deleteOnExit();
      Files.write(contents, file, java.nio.charset.StandardCharsets.UTF_8);
      return file;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * A server socket has a time to live after it is closed in which it is still
   * bound to its port. For testing, we need to use a new port each time
   * to prevent socket already bound errors
   */
  public static int getUniquePort() {
    // store/get from SystemProperties
    // to get around junit reloading
    final String KEY = "triplea.test.port";
    String prop = System.getProperties().getProperty(KEY);
    if (prop == null) {
      // start off with something fairly random, between 12000 - 14000
      prop = Integer.toString(12000 + (int) (Math.random() % 2000));
    }
    int val = Integer.parseInt(prop);
    val++;
    if (val > 15000) {
      val = 12000;
    }
    System.getProperties().put(KEY, "" + val);
    return val;
  }

  /**
   * Blocks until all Swing event thread actions have completed.
   *
   * Task is accomplished by adding a do-nothing event with SwingUtilities
   * to the event thread and then blocking until the do-nothing event is done.
   */
  public static void waitForSwingThreads() {
    // add a no-op action to the end of the swing event queue, and then wait for it
    SwingAction.invokeAndWait(() -> {
    });
  }
}
