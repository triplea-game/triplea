package games.strategy.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class EventThreadJOptionPaneTest {
  @Rule
  public final Timeout globalTimeout = new Timeout(5, TimeUnit.SECONDS);

  @Spy
  private final CountDownLatchHandler latchHandler = new CountDownLatchHandler(true);

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldRunSupplierOnEDTWhenNotCalledFromEDT() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean runOnEDT = new AtomicBoolean(false);

    new Thread(() -> {
      EventThreadJOptionPane.invokeAndWait(latchHandler, () -> {
        runOnEDT.set(SwingUtilities.isEventDispatchThread());
        return Optional.empty();
      });
      latch.countDown();
    }).start();
    latch.await();

    assertThat(runOnEDT.get(), is(true));
  }

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldNotDeadlockWhenCalledFromEDT() throws Exception {
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
  }

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldReturnSupplierResult() {
    final Object expectedResult = new Object();

    final Object actualResult =
        EventThreadJOptionPane.invokeAndWait(latchHandler, () -> Optional.of(expectedResult)).get();

    assertThat(actualResult, is(expectedResult));
  }

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldRegisterAndUnregisterLatchWithLatchHandler() {
    EventThreadJOptionPane.invokeAndWait(latchHandler, () -> Optional.empty());

    verify(latchHandler, times(1)).addShutdownLatch(any(CountDownLatch.class));
    verify(latchHandler, times(1)).removeShutdownLatch(any(CountDownLatch.class));
  }

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldRunSuccessfullyWhenLatchHandlerIsNull() {
    final Object expectedResult = new Object();

    final Object actualResult = EventThreadJOptionPane.invokeAndWait(null, () -> Optional.of(expectedResult)).get();

    assertThat(actualResult, is(expectedResult));
  }

  @Test
  public void testInvokeAndWaitWithIntSupplier_ShouldReturnIntSupplierResult() {
    final int expectedResult = 42;

    final int actualResult = EventThreadJOptionPane.invokeAndWait(latchHandler, () -> expectedResult);

    assertThat(actualResult, is(expectedResult));
  }
}
