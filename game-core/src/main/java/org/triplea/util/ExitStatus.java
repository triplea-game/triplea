package org.triplea.util;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

/** A process exit status. */
@AllArgsConstructor
@SuppressWarnings("ImmutableEnumChecker")
public enum ExitStatus {
  /** The process exited successfully (0). */
  SUCCESS(0),

  /** The process exited due to a failure (1). */
  FAILURE(1);

  private final int status;
  private final List<Runnable> exitActions = new ArrayList<>();

  public void addExitAction(final Runnable runnable) {
    exitActions.add(runnable);
  }

  /** Exits the host process with this status. */
  public void exit() {
    exitActions.forEach(Runnable::run);
    System.exit(status);
  }
}
