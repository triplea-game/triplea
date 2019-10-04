package org.triplea.swing;

import static org.mockito.Mockito.verify;

import java.beans.PropertyChangeEvent;
import javax.swing.SwingWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class SwingWorkerCompletionWaiterTest {

  private SwingWorkerCompletionWaiter waiter;

  @Mock private SwingWorkerCompletionWaiter.ProgressWindow progressWindow;

  @BeforeEach
  void setUp() {
    waiter = new SwingWorkerCompletionWaiter(progressWindow);
  }

  @Test
  void testShouldOpenProgressWindowWhenWorkerStarted() {
    waiter.propertyChange(newSwingWorkerStateEvent(SwingWorker.StateValue.STARTED));

    verify(progressWindow).open();
  }

  @Test
  void testShouldCloseProgressWindowWhenWorkerDone() {
    waiter.propertyChange(newSwingWorkerStateEvent(SwingWorker.StateValue.DONE));

    verify(progressWindow).close();
  }

  private static PropertyChangeEvent newSwingWorkerStateEvent(
      final SwingWorker.StateValue stateValue) {
    return new PropertyChangeEvent(
        new Object(),
        SwingWorkerCompletionWaiter.SWING_WORKER_STATE_PROPERTY_NAME,
        null,
        stateValue);
  }
}
