package games.strategy.engine.delegate;

import static com.google.common.base.Preconditions.checkState;

import games.strategy.engine.GameOverException;
import games.strategy.engine.message.MessengerException;
import games.strategy.engine.random.IRandomSource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages when delegates are allowed to execute.
 *
 * <p>When saving a game, we want to ensure that no delegate is executing, otherwise the delegate
 * could modify the state of the game while the game is being saved, resulting in an invalid save
 * game.
 *
 * <p>This class effectively keeps a count of how many threads are executing in the delegates, and
 * provides a way of blocking further threads from starting execution in a delegate.
 */
public class DelegateExecutionManager {
  /*
   * Delegate execution can be thought of as a read/write lock.
   * Many delegates can be executing at one time (to execute you acquire the read lock), but
   * only 1 block can be held (the block is equivalent to the read lock).
   */
  private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final ThreadLocal<Boolean> currentThreadHasReadLock =
      ThreadLocal.withInitial(() -> Boolean.FALSE);
  private volatile boolean isGameOver = false;

  public void setGameOver() {
    isGameOver = true;
  }

  /**
   * When this method returns true, threads will not be able to enter delegates until a call to
   * resumeDelegateExecution is made.
   *
   * <p>When delegateExecution is blocked, it also blocks subsequent calls to
   * blockDelegateExecution(...)
   *
   * <p>If timeToWaitMs is > 0, we will give up trying to block delegate execution after
   * timeToWaitMs has elapsed.
   */
  public boolean blockDelegateExecution(final int timeToWaitMs) throws InterruptedException {
    return readWriteLock.writeLock().tryLock(timeToWaitMs, TimeUnit.MILLISECONDS);
  }

  /** Allow delegate execution to resume. */
  public void resumeDelegateExecution() {
    readWriteLock.writeLock().unlock();
  }

  private boolean currentThreadHasReadLock() {
    return currentThreadHasReadLock.get();
  }

  /**
   * Wraps a random source so that a blocking dice call (e.g. a networked/PBEM dice server) releases
   * the delegate-execution read lock for the duration of the call and re-acquires it afterward, so
   * a save can proceed while a delegate is blocked waiting for dice. This preserves the bracketing
   * the reflective outbound proxy used to provide.
   */
  public IRandomSource newDelegateRandomSource(final IRandomSource randomSource) {
    return new IRandomSource() {
      @Override
      public int getRandom(final int max, final String annotation) {
        return outbound(() -> randomSource.getRandom(max, annotation));
      }

      @Override
      public int[] getRandom(final int max, final int count, final String annotation) {
        return outbound(() -> randomSource.getRandom(max, count, annotation));
      }
    };
  }

  private <T> T outbound(final java.util.function.Supplier<T> call) {
    assertGameNotOver();
    final boolean threadLocks = currentThreadHasReadLock();
    if (threadLocks) {
      leaveDelegateExecution();
    }
    try {
      return call.get();
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof MessengerException) {
        throw new GameOverException("Game Over!");
      }
      assertGameNotOver();
      throw e;
    } finally {
      if (threadLocks) {
        enterDelegateExecution();
      }
    }
  }

  private void assertGameNotOver() {
    if (isGameOver) {
      throw new GameOverException("Game Over");
    }
  }

  /** Invoke immediately after executing a delegate. */
  public void leaveDelegateExecution() {
    readWriteLock.readLock().unlock();
    currentThreadHasReadLock.set(Boolean.FALSE);
  }

  /**
   * Invoke immediately before executing a delegate. Upon completion of delegate execution, you must
   * invoke {@link #leaveDelegateExecution()} in order to execute another delegate on the current
   * thread.
   *
   * <p>This method will block if delegate execution is currently blocked due to a call to {@link
   * #blockDelegateExecution(int)} and will not resume until {@link #resumeDelegateExecution()} is
   * called.
   *
   * @throws IllegalStateException If a delegate is currently executing on the current thread.
   */
  public void enterDelegateExecution() {
    checkState(!currentThreadHasReadLock(), "Already locked?");

    readWriteLock.readLock().lock();
    currentThreadHasReadLock.set(Boolean.TRUE);
  }
}
