package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.concurrency.CountDownLatchHandler;

@ExtendWith(MockitoExtension.class)
final class EventThreadJOptionPaneTest {
  private static final Duration timeout = Duration.ofSeconds(5);

  @Spy private final CountDownLatchHandler latchHandler = new CountDownLatchHandler();

  @Test
  void testShouldRunSupplierOnEventDispatchThreadWhenNotCalledFromEventDispatchThread() {
    assertTimeoutPreemptively(
        timeout,
        () -> {
          final CountDownLatch latch = new CountDownLatch(1);
          final AtomicBoolean runOnEventDispatchThread = new AtomicBoolean(false);

          new Thread(
                  () -> {
                    EventThreadJOptionPane.invokeAndWait(
                        latchHandler,
                        () -> {
                          runOnEventDispatchThread.set(SwingUtilities.isEventDispatchThread());
                          return Optional.empty();
                        });
                    latch.countDown();
                  })
              .start();
          latch.await();

          assertThat(runOnEventDispatchThread.get(), is(true));
        });
  }

  @Test
  void testShouldNotDeadlockWhenCalledFromEventDispatchThread() {
    assertTimeoutPreemptively(
        timeout,
        () -> {
          final CountDownLatch latch = new CountDownLatch(1);
          final AtomicBoolean run = new AtomicBoolean(false);

          SwingUtilities.invokeLater(
              () -> {
                EventThreadJOptionPane.invokeAndWait(
                    latchHandler,
                    () -> {
                      run.set(true);
                      return Optional.empty();
                    });
                latch.countDown();
              });
          latch.await();

          assertThat(run.get(), is(true));
        });
  }

  @Test
  void testShouldReturnSupplierResult() {
    assertTimeoutPreemptively(
        timeout,
        () -> {
          final Object expectedResult = new Object();

          final Object actualResult =
              EventThreadJOptionPane.invokeAndWait(latchHandler, () -> Optional.of(expectedResult))
                  .get();

          assertThat(actualResult, is(expectedResult));
        });
  }

  @Test
  void testShouldRegisterAndUnregisterLatchWithLatchHandler() {
    assertTimeoutPreemptively(
        timeout,
        () -> {
          EventThreadJOptionPane.invokeAndWait(
              latchHandler, (Supplier<Optional<Object>>) Optional::empty);

          verify(latchHandler, times(1)).addShutdownLatch(any(CountDownLatch.class));
          verify(latchHandler, times(1)).removeShutdownLatch(any(CountDownLatch.class));
        });
  }

  @Test
  void testShouldReturnIntSupplierResult() {
    assertTimeoutPreemptively(
        timeout,
        () -> {
          final int expectedResult = 42;

          final int actualResult =
              EventThreadJOptionPane.invokeAndWait(latchHandler, () -> expectedResult);

          assertThat(actualResult, is(expectedResult));
        });
  }
}
