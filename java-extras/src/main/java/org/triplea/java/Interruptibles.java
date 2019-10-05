package org.triplea.java;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import javax.annotation.concurrent.Immutable;
import org.triplea.java.function.ThrowingRunnable;
import org.triplea.java.function.ThrowingSupplier;

/**
 * A collection of methods that assist working with operations that may be interrupted but it is
 * typically awkward to deal with {@link InterruptedException} in the calling context.
 *
 * <p>The methods of this class will always set the interrupted status of the calling thread after
 * catching an {@link InterruptedException} per best practices:
 *
 * <ul>
 *   <li>http://www.yegor256.com/2015/10/20/interrupted-exception.html
 *   <li>http://stackoverflow.com/questions/3976344/handling-interruptedexception-in-java
 * </ul>
 */
public final class Interruptibles {
  private Interruptibles() {}

  /**
   * Executes and awaits the completion of the specified operation that produces no result. If the
   * current thread is interrupted before the operation completes, the thread will be
   * re-interrupted, and this method will return {@code false}. This method re-throws any unchecked
   * exception thrown by {@code runnable}.
   *
   * @param runnable The operation to execute and await.
   * @return {@code true} if the operation completed without interruption; otherwise {@code false}
   *     if the current thread was interrupted while waiting for the operation to complete.
   */
  public static boolean await(final ThrowingRunnable<InterruptedException> runnable) {
    checkNotNull(runnable);

    return awaitResult(
            () -> {
              runnable.run();
              return null;
            })
        .completed;
  }

  /**
   * Causes the current thread to wait for the specified latch to count down to zero or until the
   * thread is interrupted.
   *
   * @param latch The latch to await.
   * @return {@code true} if the latch counted down to zero; otherwise {@code false} if the current
   *     thread was interrupted before the latch counted down to zero.
   */
  public static boolean await(final CountDownLatch latch) {
    checkNotNull(latch);

    return await(latch::await);
  }

  /**
   * Executes and awaits the completion of the specified operation that produces a result. If the
   * current thread is interrupted before the operation completes, the thread will be
   * re-interrupted, and this method will return a {@link Result} whose {@code interrupted} field is
   * {@code true}. This method re-throws any unchecked exception thrown by {@code supplier}.
   *
   * @param supplier The operation to execute and await.
   * @return If the operation completed without interruption, {@code completed} will be {@code true}
   *     and {@code result} will contain the operation's result (a {@code null} result is modeled as
   *     an empty result); if the operation was interrupted, {@code completed} will be {@code false}
   *     and {@code result} will be empty.
   */
  public static <T> Result<T> awaitResult(
      final ThrowingSupplier</* @Nullable */ T, InterruptedException> supplier) {
    checkNotNull(supplier);

    try {
      return new Result<>(true, Optional.ofNullable(supplier.get()));
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return new Result<>(false, Optional.empty());
    }
  }

  /**
   * Waits for the specified thread to die.
   *
   * @param thread The thread to await.
   * @return {@code true} if the thread died; otherwise {@code false} if the current thread was
   *     interrupted before the thread died.
   */
  public static boolean join(final Thread thread) {
    checkNotNull(thread);

    return await(thread::join);
  }

  /**
   * Causes the currently executing thread to sleep for the specified number of milliseconds.
   *
   * @param millis The length of time to sleep in milliseconds.
   * @return {@code true} if the current thread slept for the entire length of time without
   *     interruption; otherwise {@code false} if the current thread was interrupted before waking
   *     up from the sleep.
   * @throws IllegalArgumentException If {@code millis} is negative.
   */
  public static boolean sleep(final long millis) {
    return await(() -> Thread.sleep(millis));
  }

  /**
   * Causes the currently executing thread to sleep for the specified number of milliseconds plus
   * the specified number of nanoseconds.
   *
   * @param millis The length of time to sleep in milliseconds.
   * @param nanos [0, 999999] additional nanoseconds to sleep.
   * @throws IllegalArgumentException If {@code millis} is negative, or {@code nanos} is not in the
   *     range [0, 999999].
   */
  public static void sleep(final long millis, final int nanos) {
    await(() -> Thread.sleep(millis, nanos));
  }

  /**
   * The result of an interruptible operation that returns a result.
   *
   * @param <T> The result of the operation.
   */
  @Immutable
  public static final class Result<T> {
    /**
     * {@code true} if the operation was completed without interruption; otherwise {@code false} if
     * the operation was interrupted before it was complete.
     */
    public final boolean completed;

    /**
     * If {@code completed} is {@code true}, contains the result of the operation or empty if the
     * operation did not supply a result. If {@code completed} is {@code false}, always empty and
     * effectively meaningless.
     */
    public final Optional<T> result;

    private Result(final boolean completed, final Optional<T> result) {
      this.completed = completed;
      this.result = result;
    }
  }
}
