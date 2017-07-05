package games.strategy.engine.framework.ui.background;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import games.strategy.debug.ClientLogger;

/**
 * Provides methods for running tasks in the background to avoid blocking the UI.
 */
public final class BackgroundTaskRunner {
  private BackgroundTaskRunner() {}

  /**
   * Runs the specified action in the background without blocking the UI.
   *
   * <p>
   * {@code backgroundAction} is run on a background (non-UI) thread. While the background action is running, a dialog
   * is displayed with the specified message. When the background action is complete, the dialog is removed and
   * {@code completionAction} is run on the UI thread. {@code completionAction} will consume the value supplied by
   * {@code backgroundAction}.
   * </p>
   *
   * @param <T> The type of value supplied by the background action.
   *
   * @param message The message displayed to the user while the background action is running; may be {@code null}.
   * @param backgroundAction The action to run in the background; must not be {@code null}.
   * @param completionAction The action to run in the foreground upon completion of {@code backgroundAction}; must not
   *        be {@code null}.
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   */
  public static <T> void runInBackground(
      final String message,
      final Supplier<T> backgroundAction,
      final Consumer<T> completionAction) {
    checkState(SwingUtilities.isEventDispatchThread());
    checkNotNull(backgroundAction);
    checkNotNull(completionAction);

    final WaitDialog waitDialog = new WaitDialog(null, message);
    final SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
      @Override
      protected T doInBackground() {
        return backgroundAction.get();
      }

      @Override
      protected void done() {
        waitDialog.setVisible(false);
        waitDialog.dispose();

        try {
          completionAction.accept(get());
        } catch (final ExecutionException e) {
          ClientLogger.logError(e.getCause());
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    };
    worker.execute();

    waitDialog.setLocationRelativeTo(null);
    waitDialog.pack();
    waitDialog.setVisible(true);
  }
}
