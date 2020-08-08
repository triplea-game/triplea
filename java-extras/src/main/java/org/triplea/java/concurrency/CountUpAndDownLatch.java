package org.triplea.java.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * This synchronization aid is very similar to {@link CountDownLatch}, except that you can increment
 * the latch. Implements AQS behind the scenes similar to CountDownLatch. Class is hobbled together
 * from various learnings and tickets on stackexchange/stackoverflow.
 */
public class CountUpAndDownLatch {
  private final Sync sync = new Sync();

  /** Increment the count by one. */
  public void increment() {
    sync.releaseShared(1);
  }

  /**
   * Decrements the count of the latch, releasing all waiting threads if the count reaches zero.
   *
   * @see CountDownLatch#countDown()
   */
  public void countDown() {
    sync.releaseShared(-1);
  }

  /**
   * Causes the current thread to wait until the latch has counted down to zero, unless the thread
   * is interrupted.
   *
   * @see CountDownLatch#await()
   */
  public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
  }

  /**
   * Returns a string identifying this latch, as well as its state. The state, in brackets, includes
   * the String "Count =" followed by the current count.
   */
  @Override
  public String toString() {
    return super.toString() + "[Count = " + sync.getCount() + "]";
  }

  /** Synchronization control for CountingLatch. Uses AQS state to represent count. */
  private static final class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = -7639904478060101736L;

    int getCount() {
      return getState();
    }

    @Override
    protected int tryAcquireShared(final int acquires) {
      return getState() == 0 ? 1 : -1;
    }

    @Override
    protected boolean tryReleaseShared(final int delta) {
      if (delta == 0) {
        return false;
      }
      // Decrement count; signal when transition to zero
      while (true) {
        final int c = getState();
        int nextc = c + delta;
        if (c <= 0 && nextc <= 0) {
          return false;
        }
        if (nextc < 0) {
          nextc = 0;
        }
        if (compareAndSetState(c, nextc)) {
          return nextc == 0;
        }
      }
    }
  }
}
