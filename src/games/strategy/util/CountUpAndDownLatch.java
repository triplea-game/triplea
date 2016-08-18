package games.strategy.util;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * This synchronization aid is very similar to {@link CountDownLatch},
 * except that you can increment the latch.
 * Implements AQS behind the scenes similar to CountDownLatch.
 * Class is hobbled together from various learnings and tickets on stackexchange/stackoverflow.
 */
public class CountUpAndDownLatch implements Serializable {
  private static final long serialVersionUID = -1656388212821764097L;
  private final Sync sync;
  private final int originalCount;

  /**
   * Constructs a {@link CountUpAndDownLatch} initialized with zero.
   */
  public CountUpAndDownLatch() {
    sync = new Sync();
    originalCount = 0;
  }


  /**
   * Increment the count by one.
   */
  public void increment() {
    sync.releaseShared(1);
  }

  /**
   * @see CountDownLatch#countDown()
   */
  public void countDown() {
    sync.releaseShared(-1);
  }

  /**
   * @see CountDownLatch#countDown()
   * @param delta
   *        the amount to increment (or if negative, decrement countDown)
   */
  public void applyDelta(final int delta) {
    sync.releaseShared(delta);
  }

  /**
   * countDown to zero.
   */
  public void releaseAll() {
    applyDelta(Integer.MIN_VALUE);
  }

  /**
   * @see CountDownLatch#getCount()
   */
  public int getCount() {
    return sync.getCount();
  }

  /**
   * @see CountDownLatch#await()
   */
  public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
  }

  /**
   * @see CountDownLatch#await(long,TimeUnit)
   */
  public boolean await(final long timeout, final TimeUnit unit) throws InterruptedException {
    return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
  }

  /**
   * Returns a string identifying this latch, as well as its state.
   * The state, in brackets, includes the String "Count =" followed by the current count.
   */
  @Override
  public String toString() {
    return super.toString() + "[Count = " + sync.getCount() + "]";
  }

  /**
   * Synchronization control for CountingLatch.
   * Uses AQS state to represent count.
   */
  private static final class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = -7639904478060101736L;

    private Sync() {}

    private Sync(final int initialState) {
      setState(initialState);
    }

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
      for (;;) {
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
