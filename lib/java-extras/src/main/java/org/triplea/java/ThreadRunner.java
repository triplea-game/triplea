package org.triplea.java;

import lombok.experimental.UtilityClass;

/** Utility class to run code in a new thread. */
@UtilityClass
public class ThreadRunner {
  /** Executes the 'Runnable' parameter in a new thread. This method is not blocking. */
  public static void runInNewThread(final Runnable runnable) {
    new Thread(runnable).start();
  }

  public static void runInNewThread(final Runnable runnable, final String name) {
    new Thread(runnable, name).start();
  }
}
