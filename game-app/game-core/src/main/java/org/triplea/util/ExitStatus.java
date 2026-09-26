package org.triplea.util;

import java.util.Collection;
import java.util.HashSet;
import lombok.AllArgsConstructor;

/** A process exit status. */
@AllArgsConstructor
public enum ExitStatus {
  /** The process exited successfully (0). */
  SUCCESS(0),

  /** The process exited due to a failure (1). */
  FAILURE(1);

  private static final Collection<Runnable> exitActions = new HashSet<>();
  private static volatile boolean shutdownInProgress = false;
  private final int status;

  public static void addExitAction(final Runnable runnable) {
    exitActions.add(runnable);
  }

  /**
   * Call first thing in a shutdown hook. From then on {@link #exit()} is a no-op: System.exit
   * called during JVM shutdown blocks forever, so a hook reaching it would hang the process.
   */
  public static void markShutdownInProgress() {
    shutdownInProgress = true;
  }

  /** Exits the host process with this status, unless JVM shutdown is already under way. */
  public void exit() {
    if (shutdownInProgress) {
      return;
    }
    exitActions.forEach(Runnable::run);
    System.exit(status);
  }
}
