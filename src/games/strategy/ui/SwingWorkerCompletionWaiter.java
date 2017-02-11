package games.strategy.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingWorker;

import com.google.common.annotations.VisibleForTesting;

/**
 * Manages the display of a window during the execution of a {@code SwingWorker}.
 *
 * <p>
 * Use this class when it is necessary to display progress during a potentially long-running operation. If the <i>Event
 * Dispatch Thread</i> must be blocked until the operation is complete (i.e. the user should not be allowed to perform
 * any other actions in the UI), it is recommended you use a modal dialog.
 * </p>
 *
 * <p>
 * For example:
 * </p>
 *
 * <pre>
 * JDialog dialog = new JDialog(owner, true);
 * swingWorker.addPropertyChangeListener(
 *     new SwingWorkerCompletionWaiter(dialog));
 * swingWorker.execute();
 * // the dialog will be visible between the
 * // SwingWorker STARTED and DONE states
 * </pre>
 */
public final class SwingWorkerCompletionWaiter implements PropertyChangeListener {
  @VisibleForTesting
  static final String SWING_WORKER_STATE_PROPERTY_NAME = "state";

  private final ProgressWindow progressWindow;

  /**
   * Initializes a new instance of the {@code SwingWorkerCompletionWaiter} class.
   *
   * @param window The window to display while the operation is in progress; must not be {@code null}.
   */
  public SwingWorkerCompletionWaiter(final Window window) {
    this.progressWindow = ProgressWindow.fromWindow(checkNotNull(window));
  }

  @VisibleForTesting
  SwingWorkerCompletionWaiter(final ProgressWindow progressWindow) {
    this.progressWindow = progressWindow;
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    if (SWING_WORKER_STATE_PROPERTY_NAME.equals(event.getPropertyName())) {
      if (SwingWorker.StateValue.STARTED == event.getNewValue()) {
        progressWindow.open();
      } else if (SwingWorker.StateValue.DONE == event.getNewValue()) {
        progressWindow.close();
      }
    }
  }

  @VisibleForTesting
  static interface ProgressWindow {
    void close();

    void open();

    static ProgressWindow fromWindow(final Window window) {
      return new ProgressWindow() {
        @Override
        public void close() {
          window.setVisible(false);
          window.dispose();
        }

        @Override
        public void open() {
          window.setVisible(true);
        }
      };
    }
  }
}
