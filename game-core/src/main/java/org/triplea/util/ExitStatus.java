package org.triplea.util;

import java.util.Collection;
import java.util.HashSet;
import lombok.AllArgsConstructor;

/** A process exit status. */
@AllArgsConstructor
@SuppressWarnings("ImmutableEnumChecker")
public enum ExitStatus {
  /** The process exited successfully (0). */
  SUCCESS(0),

  /** The process exited due to a failure (1). */
  FAILURE(1);

  private static final Collection<Runnable> exitActions = new HashSet<>();
  private final int status;

  public static void addExitAction(final Runnable runnable) {
    exitActions.add(runnable);
  }

  /** Exits the host process with this status. */
  public void exit() {
    exitActions.forEach(Runnable::run);
    System.exit(status);
  }
}
