package games.strategy.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import com.example.mockito.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SwingActionTest {

  @Test
  public void testActionOf(@Mock final ActionEvent event, @Mock final ActionListener listener) {
    final Action action = SwingAction.of("Name1234", listener);
    assertEquals("Name1234", action.getValue(Action.NAME));
    action.actionPerformed(event);
    verify(listener).actionPerformed(event);
  }

  @Test
  public void testKeyReleaseListener(@Mock final Consumer<KeyEvent> listener) {
    final KeyEvent event = mock(KeyEvent.class);
    final KeyListener action = SwingAction.keyReleaseListener(listener);
    action.keyReleased(event);
    verify(listener).accept(event);
  }

  @Test
  public void testInvokeAndWaitWithRunnable(@Mock final Runnable action) throws Exception {
    SwingAction.invokeAndWait(action);
    verify(action).run();
    SwingUtilities.invokeAndWait(() -> {
      SwingAction.invokeAndWait(action);
      verify(action, times(2)).run();
    });
  }

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldReturnActionResultWhenCalledOffEdt() throws Exception {
    final Object value = new Object();

    assertEquals(value, SwingAction.invokeAndWait(() -> value));
  }

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldReturnActionResultWhenCalledOnEdt() throws Exception {
    final Object value = new Object();

    SwingUtilities.invokeAndWait(() -> {
      try {
        assertEquals(value, SwingAction.invokeAndWait(() -> value));
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        fail("unexpected interruption");
      }
    });
  }

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldRethrowActionUncheckedExceptionWhenCalledOffEdt() {
    final Supplier<?> action = () -> {
      throw new IllegalStateException();
    };

    assertThrows(IllegalStateException.class, () -> SwingAction.invokeAndWait(action));
  }

  @Test
  public void testInvokeAndWaitWithSupplier_ShouldRethrowActionUncheckedExceptionWhenCalledOnEdt() throws Exception {
    final Supplier<?> action = () -> {
      throw new IllegalStateException();
    };

    SwingUtilities.invokeAndWait(() -> {
      assertThrows(IllegalStateException.class, () -> SwingAction.invokeAndWait(action));
    });
  }

  @Test
  public void testInvokeNowOrLater() {
    final CountDownLatch latch = new CountDownLatch(1);

    SwingAction.invokeNowOrLater(latch::countDown);

    assertTimeoutPreemptively(Duration.ofSeconds(5L), () -> latch.await());
  }
}
