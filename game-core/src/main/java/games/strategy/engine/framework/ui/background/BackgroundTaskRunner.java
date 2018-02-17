package games.strategy.engine.framework.ui.background;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.google.common.base.Throwables;

/**
 * Provides methods for running tasks in the background to avoid blocking the UI.
 */
public final class BackgroundTaskRunner {
  private final JFrame frame;

  public BackgroundTaskRunner(final JFrame frame) {
    checkNotNull(frame);

    this.frame = frame;
  }

  /**
   * Runs the specified action in the background without blocking the UI.
   *
   * <p>
   * {@code backgroundAction} is run on a background (non-UI) thread. While the background action is running, a dialog
   * is displayed with the specified message. When the background action is complete, the dialog is removed.
   * </p>
   *
   * @param message The message displayed to the user while the background action is running.
   * @param backgroundAction The action to run in the background.
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   * @throws InterruptedException If the UI thread is interrupted while waiting for the background action to complete.
   */
  public void runInBackground(
      final String message,
      final Runnable backgroundAction)
      throws InterruptedException {
    runInBackgroundAndReturn(message, () -> {
      backgroundAction.run();
      return null;
    });
  }

  /**
   * Runs the specified action in the background without blocking the UI and returns the result of that action.
   *
   * <p>
   * {@code backgroundAction} is run on a background (non-UI) thread. While the background action is running, a dialog
   * is displayed with the specified message. When the background action is complete, the dialog is removed and
   * the value supplied by {@code backgroundAction} is returned.
   * </p>
   *
   * @param <T> The type of value supplied by the background action.
   *
   * @param message The message displayed to the user while the background action is running.
   * @param backgroundAction The action to run in the background.
   *
   * @return The value returned by {@code backgroundAction}.
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   * @throws InterruptedException If the UI thread is interrupted while waiting for the background action to complete.
   */
  public <T> T runInBackgroundAndReturn(
      final String message,
      final Supplier<T> backgroundAction)
      throws InterruptedException {
    return runInBackgroundAndReturn(message, () -> backgroundAction.get(), RuntimeException.class);
  }

  /**
   * Runs the specified action in the background without blocking the UI and returns the result of that action or throws
   * an exception if the background action fails.
   *
   * <p>
   * {@code backgroundAction} is run on a background (non-UI) thread. While the background action is running, a dialog
   * is displayed with the specified message. When the background action is complete, the dialog is removed and
   * the value supplied by {@code backgroundAction} is returned. If {@code backgroundAction} throws an exception, it
   * will be re-thrown on the UI thread.
   * </p>
   *
   * @param <T> The type of value supplied by the background action.
   * @param <E> The type of exception thrown by the background action.
   *
   * @param message The message displayed to the user while the background action is running.
   * @param backgroundAction The action to run in the background.
   * @param exceptionType The type of exception thrown by the background action.
   *
   * @return The value returned by {@code backgroundAction}.
   *
   * @throws IllegalStateException If this method is not called from the EDT.
   * @throws E If the background action fails.
   * @throws InterruptedException If the UI thread is interrupted while waiting for the background action to complete.
   */
  public <T, E extends Exception> T runInBackgroundAndReturn(
      final String message,
      final ThrowingSupplier<T, E> backgroundAction,
      final Class<E> exceptionType)
      throws E, InterruptedException {
    checkState(SwingUtilities.isEventDispatchThread());
    checkNotNull(message);
    checkNotNull(backgroundAction);
    checkNotNull(exceptionType);

    final AtomicReference<T> resultRef = new AtomicReference<>();
    final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
    final WaitDialog waitDialog = new WaitDialog(frame, message);
    final SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
      @Override
      protected T doInBackground() throws Exception {
        return backgroundAction.get();
      }

      @Override
      protected void done() {
        waitDialog.setVisible(false);
        waitDialog.dispose();

        try {
          resultRef.set(get());
        } catch (final ExecutionException e) {
          exceptionRef.set(e.getCause());
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          exceptionRef.set(e);
        }
      }
    };
    worker.execute();
    waitDialog.setVisible(true);

    final @Nullable Throwable exception = exceptionRef.get();
    if (exception != null) {
      Throwables.throwIfInstanceOf(exception, exceptionType);
      Throwables.throwIfInstanceOf(exception, InterruptedException.class);
      Throwables.throwIfUnchecked(exception);
      throw new AssertionError("unexpected checked exception", exception);
    }

    return resultRef.get();
  }

  /**
   * A supplier of results that may throw an exception.
   *
   * @param <T> The type of the supplied result.
   * @param <E> The type of exception that may be thrown by the supplier.
   */
  @FunctionalInterface
  public interface ThrowingSupplier<T, E extends Exception> {
    /**
     * Gets the result.
     *
     * @return The result.
     *
     * @throws E If an error occurs while getting the result.
     */
    T get() throws E;
  }
}
