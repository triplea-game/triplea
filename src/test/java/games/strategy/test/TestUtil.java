package games.strategy.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Files;

import games.strategy.ui.SwingAction;

/**
 * A Utility class for test classes.
 */
public final class TestUtil {
  private TestUtil() {}

  /** Create and returns a simple delete on exit temp file with contents equal to the String parameter. */
  public static File createTempFile(final String contents) {
    final File file;
    try {
      file = File.createTempFile("testFile", ".tmp");
      file.deleteOnExit();
      Files.asCharSink(file, StandardCharsets.UTF_8).write(contents);
      return file;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Blocks until all Swing event thread actions have completed.
   *
   * <p>
   * Task is accomplished by adding a do-nothing event with SwingUtilities
   * to the event thread and then blocking until the do-nothing event is done.
   * </p>
   */
  public static void waitForSwingThreads() {
    // add a no-op action to the end of the swing event queue, and then wait for it
    SwingAction.invokeAndWait(() -> {
    });
  }
}
