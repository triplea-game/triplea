package org.triplea.java;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Module to execute a task with retries. Provides a builder interface to specify number of max
 * attempts (max number of times the task will be executed) and backoff.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Retryable {
  private static final Consumer<Duration> DEFAULT_THREAD_SLEEP =
      duration -> Interruptibles.sleep(duration.toMillis());

  private final Consumer<Duration> threadSleeper;
  private final int maxAttempts;
  private final Duration fixedBackOff;
  private final Supplier<Boolean> taskRunner;

  public static MaxAttemptsBuilder builder() {
    return new MaxAttemptsBuilder(DEFAULT_THREAD_SLEEP);
  }

  @VisibleForTesting
  static MaxAttemptsBuilder builder(final Consumer<Duration> threadSleeper) {
    return new MaxAttemptsBuilder(threadSleeper);
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class MaxAttemptsBuilder {
    private final Consumer<Duration> threadSleeper;

    public BackOffBuilder withMaxAttempts(final int maxAttempts) {
      Preconditions.checkArgument(
          maxAttempts > 1,
          "Max attempt count must be greater than 1, if max attempt is 1, "
              + "just invoke your task directly without the retry mechanism");
      return new BackOffBuilder(threadSleeper, maxAttempts);
    }
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class BackOffBuilder {
    private final Consumer<Duration> threadSleeper;
    private final int maxAttempts;

    public TaskBuilder withFixedBackOff(final Duration duration) {
      Preconditions.checkArgument(duration.toMillis() > 0, "Minimum backoff is 1ms");
      return new TaskBuilder(threadSleeper, maxAttempts, duration);
    }
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class TaskBuilder {
    private final Consumer<Duration> threadSleeper;
    private final int maxAttempts;
    private final Duration backOff;

    public RetryableBuilder withTask(final Supplier<Boolean> taskRunner) {
      Preconditions.checkNotNull(taskRunner);
      return new RetryableBuilder(threadSleeper, maxAttempts, backOff, taskRunner);
    }
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class RetryableBuilder {
    private final Consumer<Duration> threadSleeper;
    private final int maxAttempts;
    private final Duration fixedBackOff;
    private final Supplier<Boolean> taskRunner;

    public Retryable build() {
      return new Retryable(threadSleeper, maxAttempts, fixedBackOff, taskRunner);
    }

    public boolean buildAndExecute() {
      return build().execute();
    }
  }

  /**
   * Executes the retryable task with retries. The result of the task is retried until either:
   *
   * <ul>
   *   <li>The task throws an uncaught exception
   *   <li>The task returns false and is max executions reaches max attempts
   *   <li>The task returns true
   * </ul>
   *
   * After any failures (the task returns false), the retry mechanism will sleep for the back-off
   * period and try again.
   */
  public boolean execute() {
    for (int i = 0; i < maxAttempts; i++) {
      final Boolean result = taskRunner.get();
      if (result != null && result) {
        return true;
      }

      final boolean wasLastAttempt = i + 1 == maxAttempts;
      // only sleep if there are more attempts left
      if (!wasLastAttempt) {
        threadSleeper.accept(fixedBackOff);
      }
    }
    return false;
  }
}
