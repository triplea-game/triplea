package org.triplea.thread;

import java.awt.EventQueue;
import lombok.experimental.UtilityClass;

/** Assertions about whether the caller runs on the AWT Event Dispatch Thread. */
@UtilityClass
public final class EventThreadChecks {

  public static void ensureOnEventDispatchThread() {
    if (!EventQueue.isDispatchThread()) {
      throw new IllegalStateException("Wrong thread (not Event Dispatch Thread)");
    }
  }

  public static void ensureNotOnEventDispatchThread() {
    if (EventQueue.isDispatchThread()) {
      throw new IllegalStateException("This method should not called from Event Dispatch Thread");
    }
  }
}
