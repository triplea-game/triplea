package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetryableTest {

  @Mock private Supplier<Boolean> task;
  @Mock private Consumer<Duration> threadSleeper;

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
    final boolean result =
        Retryable.builder()
            .withMaxAttempts(2)
            .withFixedBackOff(Duration.ofMillis(1))
            .withTask(task)
            .buildAndExecute();

    assertThat(result, is(false));
    verify(task, times(2)).get();
  }

  @Test
  void retriedTaskIsInvokedMultipleTimes() {
    Retryable.builder()
        .withMaxAttempts(3)
        .withFixedBackOff(Duration.ofMillis(1))
        .withTask(task)
        .buildAndExecute();

    verify(task, times(3)).get();
  }

  @Test
  void successOnFirstIsNotRetried() {
    when(task.get()).thenReturn(true);

    final boolean result =
        Retryable.builder()
            .withMaxAttempts(3)
            .withFixedBackOff(Duration.ofMillis(1))
            .withTask(task)
            .buildAndExecute();

    assertThat(result, is(true));
    verify(task).get();
  }

  @Test
  @DisplayName("We should only sleep between retries, do not sleep after the last attempt")
  void noSleepAfterLastRetry() {
    final int maxAttempts = 3;
    final Duration backOff = Duration.ofMillis(1);
    Retryable.builder(threadSleeper)
        .withMaxAttempts(maxAttempts)
        .withFixedBackOff(backOff)
        .withTask(() -> false)
        .buildAndExecute();
    verify(threadSleeper, times(maxAttempts - 1)).accept(backOff);
  }

  @Test
  void returnsTrueIfRetryIsSuccess() {
    when(task.get()).thenReturn(false).thenReturn(true);

    final boolean result =
        Retryable.builder()
            .withMaxAttempts(3)
            .withFixedBackOff(Duration.ofMillis(1))
            .withTask(task)
            .buildAndExecute();

    assertThat(result, is(true));
    verify(task, times(2)).get();
  }
}
