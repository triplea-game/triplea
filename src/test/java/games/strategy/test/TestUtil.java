package games.strategy.test;

import games.strategy.ui.SwingAction;

/**
 * A Utility class for test classes.
 */
public final class TestUtil {
  private TestUtil() {}

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
