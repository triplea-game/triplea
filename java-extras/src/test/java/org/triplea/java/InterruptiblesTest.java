package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.Runnables;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.function.ThrowingRunnable;

@ExtendWith(MockitoExtension.class)
final class InterruptiblesTest {
  @AfterEach
  @SuppressWarnings("static-method")
  void resetTestThreadInterruptionStatusSoItDoesNotCrossTestBoundaries() {
    Thread.interrupted();
  }

  @Nested
  final class AwaitTest {
    @Mock private ThrowingRunnable<InterruptedException> runnable;

    @Test
    void shouldReturnTrueWhenCompleted() throws Exception {
      final boolean completed = Interruptibles.await(runnable);

      verify(runnable).run();
      assertThat(completed, is(true));
    }

    @Test
    void shouldReturnFalseWhenInterrupted() {
      final boolean completed =
          Interruptibles.await(
              () -> {
                throw new InterruptedException();
              });

      assertThat(completed, is(false));
      assertThat(Thread.currentThread().isInterrupted(), is(true));
    }

    @Test
    void shouldRethrowRunnableUncheckedException() {
      assertThrows(
          IllegalStateException.class,
          () ->
              Interruptibles.await(
                  () -> {
                    throw new IllegalStateException();
                  }));
    }
  }

  @Nested
  final class AwaitResultTest {
    @Test
    void shouldReturnCompletedSupplierNonNullResultWhenCompleted() {
      final Object value = new Object();

      final Interruptibles.Result<Object> result = Interruptibles.awaitResult(() -> value);

      assertThat(result.completed, is(true));
      assertThat(result.result, is(Optional.of(value)));
    }

    @Test
    void shouldReturnCompletedSupplierNullResultWhenCompleted() {
      final Interruptibles.Result<Object> result = Interruptibles.awaitResult(() -> null);

      assertThat(result.completed, is(true));
      assertThat(result.result, is(Optional.empty()));
    }

    @Test
    void shouldReturnInterruptedEmptyResultWhenInterrupted() {
      final Interruptibles.Result<Object> result =
          Interruptibles.awaitResult(
              () -> {
                throw new InterruptedException();
              });

      assertThat(result.completed, is(false));
      assertThat(result.result, is(Optional.empty()));
      assertThat(Thread.currentThread().isInterrupted(), is(true));
    }

    @Test
    void shouldRethrowSupplierUncheckedException() {
      assertThrows(
          IllegalStateException.class,
          () ->
              Interruptibles.awaitResult(
                  () -> {
                    throw new IllegalStateException();
                  }));
    }
  }

  @Nested
  final class AwaitCountDownLatchTest {
    @Test
    void shouldWaitUntilLatchCountIsZero() {
      final CountDownLatch latch = new CountDownLatch(0);

      assertTimeoutPreemptively(
          Duration.ofSeconds(5L), () -> assertThat(Interruptibles.await(latch), is(true)));
    }
  }

  @Nested
  final class JoinTest {
    @Test
    void shouldWaitUntilThreadIsDead() {
      final Thread thread = new Thread(Runnables.doNothing());
      thread.start();

      assertTimeoutPreemptively(
          Duration.ofSeconds(5L), () -> assertThat(Interruptibles.join(thread), is(true)));
    }
  }

  @Nested
  final class SleepTest {
    @Test
    void shouldThrowExceptionWhenMillisIsNegative() {
      assertThrows(IllegalArgumentException.class, () -> Interruptibles.sleep(-1L));
    }
  }

  @Nested
  final class SleepWithNanosTest {
    @Test
    void shouldThrowExceptionWhenMillisIsNegative() {
      assertThrows(IllegalArgumentException.class, () -> Interruptibles.sleep(-1L, 0));
    }

    @Test
    void shouldThrowExceptionWhenNanosIsLessThanZero() {
      assertThrows(IllegalArgumentException.class, () -> Interruptibles.sleep(0L, -1));
    }

    @Test
    void shouldThrowExceptionWhenNanosIsGreaterThan999999() {
      assertThrows(IllegalArgumentException.class, () -> Interruptibles.sleep(0L, 1_000_000));
    }
  }
}
