package org.triplea.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SwingActionTest {
  private static final Object VALUE = new Object();

  private static final Runnable RUNNABLE_THROWING_EXCEPTION = () -> {
    throw new IllegalStateException();
  };

  private static final Supplier<?> SUPPLIER_THROWING_EXCEPTION = () -> {
    throw new IllegalStateException();
  };
  @Mock
  private Runnable action;
  @Mock
  private ActionEvent event;
  @Mock
  private ActionListener listener;
  @Mock
  private Consumer<KeyEvent> consumer;

  @Test
  public void testActionOf() {
    final Action action = SwingAction.of("Name1234", listener);
    assertEquals("Name1234", action.getValue(Action.NAME));
    action.actionPerformed(event);
    verify(listener).actionPerformed(event);
  }

  @Test
  public void testKeyReleaseListener() {
    final KeyEvent event = mock(KeyEvent.class);
    final KeyListener action = SwingAction.keyReleaseListener(consumer);
    action.keyReleased(event);
    verify(consumer).accept(event);
  }

  @Test
  public void testInvokeAndWait_ShouldInvokeActionWhenCalledOffEdt()
      throws Exception {
    SwingAction.invokeAndWait(action);

    verify(action).run();
  }

  @Test
  public void testInvokeAndWait_ShouldInvokeActionWhenCalledOnEdt()
      throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        SwingAction.invokeAndWait(action);
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    verify(action).run();
  }

  @Test
  public void testInvokeAndWait_ShouldRethrowActionUncheckedExceptionWhenCalledOffEdt() {
    assertThrows(IllegalStateException.class, () -> SwingAction.invokeAndWait(RUNNABLE_THROWING_EXCEPTION));
  }

  @Test
  public void testInvokeAndWait_ShouldRethrowActionUncheckedExceptionWhenCalledOnEdt() throws Exception {
    SwingUtilities.invokeAndWait(() -> assertThrows(
        IllegalStateException.class,
        () -> SwingAction.invokeAndWait(RUNNABLE_THROWING_EXCEPTION)));
  }

  @Test
  public void testInvokeAndWaitResult_ShouldReturnActionResultWhenCalledOffEdt() throws Exception {
    assertEquals(VALUE, SwingAction.invokeAndWaitResult(() -> VALUE));
  }

  @Test
  public void testInvokeAndWaitResult_ShouldReturnActionResultWhenCalledOnEdt() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        SwingAction.invokeAndWaitResult(() -> VALUE);
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testInvokeAndWaitResult_ShouldRethrowActionUncheckedExceptionWhenCalledOffEdt() {
    assertThrows(IllegalStateException.class, () -> SwingAction.invokeAndWaitResult(SUPPLIER_THROWING_EXCEPTION));
  }

  @Test
  public void testInvokeAndWaitResult_ShouldRethrowActionUncheckedExceptionWhenCalledOnEdt() throws Exception {
    SwingUtilities.invokeAndWait(() -> assertThrows(
        IllegalStateException.class,
        () -> SwingAction.invokeAndWaitResult(SUPPLIER_THROWING_EXCEPTION)));
  }

  @Test
  public void testInvokeNowOrLater() {
    final CountDownLatch latch = new CountDownLatch(1);

    SwingAction.invokeNowOrLater(latch::countDown);

    assertTimeoutPreemptively(Duration.ofSeconds(5L), () -> latch.await());
  }
}
