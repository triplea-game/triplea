package org.triplea.java.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A handler for CountDownLatch's with methods to release latches being waited on from outside of
 * their threads.
 */
@ThreadSafe
public class CountDownLatchHandler {
  @GuardedBy("this")
  private final List<CountDownLatch> latchesToCloseOnShutdown = new ArrayList<>();

  @GuardedBy("this")
  private boolean isShutDown = false;

  private final boolean releaseLatchOnInterrupt;

  public CountDownLatchHandler() {
    this(true);
  }

  public CountDownLatchHandler(final boolean releaseLatchOnInterrupt) {
    this.releaseLatchOnInterrupt = releaseLatchOnInterrupt;
  }

  /**
   * If {@code releaseLatchOnInterrupt} was set to true upon construction of this handler, then this
   * method will release and remove the latch; otherwise it does nothing.
   */
  public void interruptLatch(final CountDownLatch latch) {
    if (releaseLatchOnInterrupt) {
      removeShutdownLatch(latch);
    }
  }

  /**
   * Shuts down this handler by releasing all latches and clearing the list of latches being
   * handled.
   */
  public void shutDown() {
    synchronized (this) {
      if (isShutDown) {
        return;
      }
      isShutDown = true;

      for (final CountDownLatch latch : latchesToCloseOnShutdown) {
        releaseLatch(latch);
      }
      latchesToCloseOnShutdown.clear();
    }
  }

  /** Utility method to fully release any CountDownLatch. */
  private static void releaseLatch(final CountDownLatch latch) {
    if (latch == null) {
      return;
    }
    while (latch.getCount() > 0) {
      latch.countDown();
    }
  }

  /**
   * Add a latch that will be released when this handler shuts down. If this handler is already
   * shutdown, then we will release the latch immediately.
   */
  public void addShutdownLatch(final CountDownLatch latch) {
    synchronized (this) {
      if (isShutDown) {
        releaseLatch(latch);
        return;
      }
      latchesToCloseOnShutdown.add(latch);
    }
  }

  /** Releases the latch and removes it from the latches being handled by this handler. */
  public void removeShutdownLatch(final CountDownLatch latch) {
    synchronized (this) {
      releaseLatch(latch);
      latchesToCloseOnShutdown.remove(latch);
    }
  }
}
