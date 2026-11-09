package org.triplea.thread;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

final class EventThreadChecksTest {

  /**
   * @param run Runnable to be executed
   * @throws Throwable target exception raised in the Runnable executed in Event Dispatch Thread
   */
  static void runInEvenDispatchThreadAndRethrow(final Runnable run) throws Throwable {
    try {
      EventQueue.invokeAndWait(run);
    } catch (final InvocationTargetException e) {
      throw e.getTargetException();
    }
  }

  @Test
  void ensureOnEventDispatchThread() {
    assertDoesNotThrow(
        () -> runInEvenDispatchThreadAndRethrow(EventThreadChecks::ensureOnEventDispatchThread));
    assertThrows(
        IllegalStateException.class,
        EventThreadChecks::ensureOnEventDispatchThread); // negative test
  }

  @Test
  void ensureNotOnEventDispatchThread() {
    assertAll(EventThreadChecks::ensureNotOnEventDispatchThread); // positive test
    assertThrows(
        IllegalStateException.class,
        () -> runInEvenDispatchThreadAndRethrow(EventThreadChecks::ensureNotOnEventDispatchThread));
  }
}
