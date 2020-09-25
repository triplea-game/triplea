package org.triplea.java;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class RetryableTest {

  /**
   * Increment the invocation count and return true if the invocation count matches the input
   * parameter. In other words, we can say "return true on the 3rd invocation."
   */
  @RequiredArgsConstructor
  private static final class Task implements Supplier<Optional<Boolean>> {
    private final int successIteration;
    private int invocationCount;

    @Override
    public Optional<Boolean> get() {
      invocationCount++;
      return invocationCount == successIteration ? Optional.of(true) : Optional.empty();
    }
  }

  private int sleepCount;
  private final Consumer<Duration> threadSleeper = duration -> sleepCount++;

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void maxAttemptOfZeroOrOneThrows(final int invalidMaxAttempts) {
    assertThrows(
        IllegalArgumentException.class,
        () -> Retryable.builder().withMaxAttempts(invalidMaxAttempts));
  }

  @ParameterizedTest
  @MethodSource
  void durationMustBeAtLeastOneMilli(final Duration invalidDuration) {
    assertThrows(
        IllegalArgumentException.class,
        () -> Retryable.builder().withMaxAttempts(2).withFixedBackOff(invalidDuration));
  }

  @SuppressWarnings("unused")
  private static List<Duration> durationMustBeAtLeastOneMilli() {
    return List.of(
        Duration.ZERO,
        Duration.ofNanos(10),
        Duration.ofNanos(10_000), // 10 microseconds
        Duration.ofNanos(999_000) // 999 microseconds, one micro less than a milli
        );
  }

  @Test
  void retryIfTaskReturnsFalseAndReturnFalseIfAllFail() {
    final Task task = new Task(3);
    final Optional<Boolean> result =
        Retryable.<Boolean>builder()
            .withMaxAttempts(2)
            .withFixedBackOff(Duration.ofMillis(1))
            .withTask(task)
            .buildAndExecute();

    assertThat(result, isEmpty());
    assertThat(task.invocationCount, is(2));
  }

  @Test
  void retriedTaskSucceedsOnSecondInvocation() {
    final Task task = new Task(2);

    final Optional<Boolean> result =
        Retryable.<Boolean>builder()
            .withMaxAttempts(2)
            .withFixedBackOff(Duration.ofMillis(1))
            .withTask(task)
            .buildAndExecute();

    assertThat(result, is(isPresent()));
    assertThat(task.invocationCount, is(2));
  }

  @Test
  void successOnFirstIsNotRetried() {
    final Task task = new Task(1);

    final Optional<Boolean> result =
        Retryable.<Boolean>builder()
            .withMaxAttempts(3)
            .withFixedBackOff(Duration.ofMillis(1))
            .withTask(task)
            .buildAndExecute();

    assertThat(result, is(isPresent()));
    assertThat(task.invocationCount, is(1));
  }

  @Test
  @DisplayName("We should only sleep between retries, do not sleep after the last attempt")
  void noSleepAfterLastRetry() {
    final int maxAttempts = 3;
    final Task task = new Task(maxAttempts + 1);

    final Duration backOff = Duration.ofMillis(1);
    Retryable.<Boolean>builder(threadSleeper)
        .withMaxAttempts(maxAttempts)
        .withFixedBackOff(backOff)
        .withTask(task)
        .buildAndExecute();

    assertThat(sleepCount, is(maxAttempts - 1));
  }
}
