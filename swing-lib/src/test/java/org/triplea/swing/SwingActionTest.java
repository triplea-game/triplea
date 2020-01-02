package org.triplea.swing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SwingActionTest {
  private static final Object VALUE = new Object();

  private static Object throwException() {
    throw new IllegalStateException();
  }

  @Mock private Runnable action;
  @Mock private ActionEvent event;
  @Mock private ActionListener listener;
  @Mock private Consumer<KeyEvent> consumer;

  @Test
  void testActionOf() {
    final Action action = SwingAction.of("Name1234", listener);
    assertEquals("Name1234", action.getValue(Action.NAME));
    action.actionPerformed(event);
    verify(listener).actionPerformed(event);
  }

  @Test
  void testKeyReleaseListener() {
    final KeyEvent event = mock(KeyEvent.class);
    final KeyListener action = SwingAction.keyReleaseListener(consumer);
    action.keyReleased(event);
    verify(consumer).accept(event);
  }

  @Test
  void testInvokeAndWait_ShouldInvokeActionWhenCalledOffEdt() throws Exception {
    SwingAction.invokeAndWait(action);

    verify(action).run();
  }

  @Test
  void testInvokeAndWait_ShouldInvokeActionWhenCalledOnEdt() throws Exception {
    SwingUtilities.invokeAndWait(() -> assertDoesNotThrow(() -> SwingAction.invokeAndWait(action)));

    verify(action).run();
  }

  @Test
  void testInvokeAndWait_ShouldRethrowActionUncheckedExceptionWhenCalledOffEdt() {
    assertThrows(
        IllegalStateException.class,
        () -> SwingAction.invokeAndWait(SwingActionTest::throwException));
  }

  @Test
  void testInvokeAndWait_ShouldRethrowActionUncheckedExceptionWhenCalledOnEdt() throws Exception {
    SwingUtilities.invokeAndWait(
        () ->
            assertThrows(
                IllegalStateException.class,
                () -> SwingAction.invokeAndWait(SwingActionTest::throwException)));
  }

  @Test
  void testInvokeAndWaitResult_ShouldReturnActionResultWhenCalledOffEdt() throws Exception {
    assertEquals(VALUE, SwingAction.invokeAndWaitResult(() -> VALUE));
  }

  @Test
  void testInvokeAndWaitResult_ShouldReturnActionResultWhenCalledOnEdt() throws Exception {
    final AtomicReference<Object> actualValueRef = new AtomicReference<>();

    SwingUtilities.invokeAndWait(
        () ->
            assertDoesNotThrow(
                () -> actualValueRef.set(SwingAction.invokeAndWaitResult(() -> VALUE))));

    assertEquals(VALUE, actualValueRef.get());
  }

  @Test
  void testInvokeAndWaitResult_ShouldRethrowActionUncheckedExceptionWhenCalledOffEdt() {
    assertThrows(
        IllegalStateException.class,
        () -> SwingAction.invokeAndWaitResult(SwingActionTest::throwException));
  }

  @Test
  void testInvokeAndWaitResult_ShouldRethrowActionUncheckedExceptionWhenCalledOnEdt()
      throws Exception {
    SwingUtilities.invokeAndWait(
        () ->
            assertThrows(
                IllegalStateException.class,
                () -> SwingAction.invokeAndWaitResult(SwingActionTest::throwException)));
  }

  @Test
  void testInvokeNowOrLater() {
    final CountDownLatch latch = new CountDownLatch(1);

    SwingAction.invokeNowOrLater(latch::countDown);

    assertTimeoutPreemptively(Duration.ofSeconds(5L), (Executable) latch::await);
  }
}
