package games.strategy.ui;

import static org.mockito.Mockito.verify;

import java.beans.PropertyChangeEvent;

import javax.swing.SwingWorker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class SwingWorkerCompletionWaiterTest {
  @InjectMocks
  private SwingWorkerCompletionWaiter waiter;

  @Mock
  private SwingWorkerCompletionWaiter.ProgressWindow progressWindow;

  @Test
  public void testShouldOpenProgressWindowWhenWorkerStarted() {
    waiter.propertyChange(newSwingWorkerStateEvent(SwingWorker.StateValue.STARTED));

    verify(progressWindow).open();
  }

  @Test
  public void testShouldCloseProgressWindowWhenWorkerDone() {
    waiter.propertyChange(newSwingWorkerStateEvent(SwingWorker.StateValue.DONE));

    verify(progressWindow).close();
  }

  private static PropertyChangeEvent newSwingWorkerStateEvent(final SwingWorker.StateValue stateValue) {
    return new PropertyChangeEvent(
        new Object(),
        SwingWorkerCompletionWaiter.SWING_WORKER_STATE_PROPERTY_NAME,
        null,
        stateValue);
  }
}
