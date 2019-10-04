package org.triplea.util;

import lombok.AllArgsConstructor;

/** A process exit status. */
@AllArgsConstructor
public enum ExitStatus {
  /** The process exited successfully (0). */
  SUCCESS(0),

  /** The process exited due to a failure (1). */
  FAILURE(1);

  private final int status;

  /** Exits the host process with this status. */
  public void exit() {
    System.exit(status);
  }
}
