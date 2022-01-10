package org.triplea.java;

import lombok.experimental.UtilityClass;

/** Utility class to run code in a new thread. */
@UtilityClass
public class ThreadRunner {
  /** Executes the 'Runnable' parameter in a new thread. This method is not blocking. */
  public static void runInNewThread(final Runnable runnable) {
    new Thread(runnable).start();
  }

  /**
   * Executes the 'Runnable' parameter in a new thread and gives that thread the provided 'name'.
   * This method is not blocking.
   */

  public static void runInNewThread(final String name, final Runnable runnable) {
    new Thread(runnable, name).start();
  }

}
