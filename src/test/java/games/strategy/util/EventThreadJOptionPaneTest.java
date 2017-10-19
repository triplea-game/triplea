package games.strategy.util;

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

import javax.swing.SwingUtilities;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
public final class EventThreadJOptionPaneTest {
  private static final Duration timeout = Duration.ofSeconds(5);

  @Spy
  private final CountDownLatchHandler latchHandler = new CountDownLatchHandler();

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldRunSupplierOnEventDispatchThreadWhenNotCalledFromEventDispatchThread()
      throws Exception {
    assertTimeoutPreemptively(timeout, () -> {
      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicBoolean runOnEventDispatchThread = new AtomicBoolean(false);

      new Thread(() -> {
        EventThreadJOptionPane.invokeAndWait(latchHandler, () -> {
          runOnEventDispatchThread.set(SwingUtilities.isEventDispatchThread());
          return Optional.empty();
        });
        latch.countDown();
      }).start();
      latch.await();

      assertThat(runOnEventDispatchThread.get(), is(true));
    });
  }

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldNotDeadlockWhenCalledFromEventDispatchThread() throws Exception {
    assertTimeoutPreemptively(timeout, () -> {
      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicBoolean run = new AtomicBoolean(false);

      SwingUtilities.invokeLater(() -> {
        EventThreadJOptionPane.invokeAndWait(latchHandler, () -> {
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
  public void testInvokeAndWaitWithSupplier_ShouldReturnSupplierResult() {
    assertTimeoutPreemptively(timeout, () -> {
      final Object expectedResult = new Object();

      final Object actualResult =
          EventThreadJOptionPane.invokeAndWait(latchHandler, () -> Optional.of(expectedResult)).get();

      assertThat(actualResult, is(expectedResult));
    });
  }

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldRegisterAndUnregisterLatchWithLatchHandler() {
    assertTimeoutPreemptively(timeout, () -> {
      EventThreadJOptionPane.invokeAndWait(latchHandler, () -> Optional.empty());

      verify(latchHandler, times(1)).addShutdownLatch(any(CountDownLatch.class));
      verify(latchHandler, times(1)).removeShutdownLatch(any(CountDownLatch.class));
    });
  }

  @Test
  public void testInvokeAndWaitWithIntSupplier_ShouldReturnIntSupplierResult() {
    assertTimeoutPreemptively(timeout, () -> {
      final int expectedResult = 42;

      final int actualResult = EventThreadJOptionPane.invokeAndWait(latchHandler, () -> expectedResult);

      assertThat(actualResult, is(expectedResult));
    });
  }
}
