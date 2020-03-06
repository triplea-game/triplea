package games.strategy.engine.framework.ui.background;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import lombok.Builder;
import org.triplea.java.function.ThrowingRunnable;
import org.triplea.swing.SwingComponents;

/**
 * Runs a background task with a wait dialog in the foreground. Similar to {@see
 * BackgroundTaskRunner} except:
 *
 * <ul>
 *   <li>Wait dialog can be closed.
 *   <li>Does not throw exceptions, instead invokes an exception handler
 * </ul>
 *
 * @param <E> Exception type that can be thrown by the background task.
 */
@Builder
public class TaskRunner<E extends Exception> {
  /**
   * Error callback handler. The background task encounters an exception, the background task is
   * cancelled and this is invoked with the exception that occurred.
   */
  @Nonnull private final Consumer<E> exceptionHandler;
  /**
   * Title of the wait spinner window that is displayed to user while a background action is being
   * executed.
   */
  @Nonnull private final String waitDialogTitle;

  /**
   * Runs a task with a closeable wait spinner dialog in the foreground. THe wait spinner closes
   * when the task completes. If the spinner dialog is closed, then the background task is
   * cancelled. If there are any exceptions encountered, the spinner dialog is closed and the {@see
   * exceptionHandler} {@code Consumer} property is called.
   *
   * @param backgroundAction The action to be executed.
   */
  public void run(final ThrowingRunnable<E> backgroundAction) {
    Preconditions.checkNotNull(backgroundAction);
    final AtomicReference<Exception> exceptionRef = new AtomicReference<>();

    // TODO: after https://github.com/triplea-game/triplea/pull/6001 is merged
    //   change the 'null' parent window reference to main frame.
    final WaitDialog waitDialog = new WaitDialog(null, waitDialogTitle);
    waitDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    final SwingWorker<Void, Void> worker =
        buildBackgroundJobRunnerWithWaitDialog(backgroundAction, exceptionRef, waitDialog);
    // If wait dialog is closed, then cancel the worker background job.
    SwingComponents.addWindowClosedListener(waitDialog, () -> worker.cancel(true));

    worker.execute();
    waitDialog.setVisible(true);

    try {
      @SuppressWarnings("unchecked")
      final @Nullable E exception = (E) exceptionRef.get();
      if (exception != null) {
        exceptionHandler.accept(exception);
      }
    } catch (final ClassCastException e) {
      throw new AssertionError("Unexpected exception thrown", e);
    }
  }

  private SwingWorker<Void, Void> buildBackgroundJobRunnerWithWaitDialog(
      final ThrowingRunnable<E> backgroundAction,
      final AtomicReference<Exception> exceptionRef,
      final WaitDialog waitDialog) {
    return new SwingWorker<>() {
      @Override
      protected Void doInBackground() {
        try {
          backgroundAction.run();
        } catch (final Exception e) {
          exceptionRef.set(e);
        }
        return null;
      }

      @Override
      protected void done() {
        waitDialog.setVisible(false);
        waitDialog.dispose();
      }
    };
  }
}
