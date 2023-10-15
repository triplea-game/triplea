package games.strategy.engine.framework.ui.background;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Throwables;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.triplea.java.Interruptibles;
import org.triplea.java.function.ThrowingSupplier;
import org.triplea.swing.SwingAction;

/** Provides methods for running tasks in the background to avoid blocking the UI. */
@UtilityClass
public final class BackgroundTaskRunner {
  @Getter private static JFrame mainFrame;

  public static void setMainFrame(final JFrame mainFrame) {
    checkState(BackgroundTaskRunner.mainFrame == null);
    BackgroundTaskRunner.mainFrame = mainFrame;
  }

  /**
   * Runs the specified action in the background without blocking the UI.
   *
   * <p>{@code backgroundAction} is run on a background (non-UI) thread. While the background action
   * is running, a dialog is displayed with the specified message. When the background action is
   * complete, the dialog is removed.
   *
   * @param message The message displayed to the user while the background action is running.
   * @param backgroundAction The action to run in the background.
   * @throws IllegalStateException If this method is not called from the EDT.
   * @throws InterruptedException If the UI thread is interrupted while waiting for the background
   *     action to complete.
   */
  public static void runInBackground(final String message, final Runnable backgroundAction) {
    Interruptibles.await(
        () ->
            SwingAction.invokeAndWait(
                () ->
                    Interruptibles.await(
                        () ->
                            runInBackgroundAndReturn(
                                message,
                                () -> {
                                  backgroundAction.run();
                                  return null;
                                }))));
  }

  /**
   * Runs the specified action in the background without blocking the UI and returns the result of
   * that action.
   *
   * <p>{@code backgroundAction} is run on a background (non-UI) thread. While the background action
   * is running, a dialog is displayed with the specified message. When the background action is
   * complete, the dialog is removed and the value supplied by {@code backgroundAction} is returned.
   *
   * @param <T> The type of value supplied by the background action.
   * @param message The message displayed to the user while the background action is running.
   * @param backgroundAction The action to run in the background.
   * @return The value returned by {@code backgroundAction}.
   * @throws IllegalStateException If this method is not called from the EDT.
   * @throws InterruptedException If the UI thread is interrupted while waiting for the background
   *     action to complete.
   */
  public static <T> T runInBackgroundAndReturn(
      final String message, final Supplier<T> backgroundAction) throws InterruptedException {
    return runInBackgroundAndReturn(message, backgroundAction::get, null, RuntimeException.class);
  }

  public static <T> T runInBackgroundAndReturn(
      Consumer<T> runOnEdtBeforeDialogClose, String message, Supplier<T> backgroundAction)
      throws InterruptedException {
    return runInBackgroundAndReturn(
        message, backgroundAction::get, runOnEdtBeforeDialogClose, RuntimeException.class);
  }

  /**
   * Runs the specified action in the background without blocking the UI and returns the result of
   * that action or throws an exception if the background action fails.
   *
   * <p>{@code backgroundAction} is run on a background (non-UI) thread. While the background action
   * is running, a dialog is displayed with the specified message. When the background action is
   * complete, the dialog is removed and the value supplied by {@code backgroundAction} is returned.
   * If {@code backgroundAction} throws an exception, it will be re-thrown on the UI thread.
   *
   * @param <T> The type of value supplied by the background action.
   * @param <E> The type of exception thrown by the background action.
   * @param message The message displayed to the user while the background action is running.
   * @param backgroundAction The action to run in the background.
   * @param runOnEdtBeforeDialogClose Work to be done on EDT with the result, before closing the
   *     dialog. Some Swing operations can be slow (e.g. layout of lots of HTML like unit help), so
   *     this can be used to ensure we only close the progress dialog once this work has been done.
   * @param exceptionType The type of exception thrown by the background action.
   * @return The value returned by {@code backgroundAction}.
   * @throws IllegalStateException If this method is not called from the EDT.
   * @throws E If the background action fails.
   * @throws InterruptedException If the UI thread is interrupted while waiting for the background
   *     action to complete.
   */
  public static <T, E extends Exception> T runInBackgroundAndReturn(
      final String message,
      final ThrowingSupplier<T, E> backgroundAction,
      final Consumer<T> runOnEdtBeforeDialogClose,
      final Class<E> exceptionType)
      throws E, InterruptedException {
    checkState(SwingUtilities.isEventDispatchThread());
    checkNotNull(message);
    checkNotNull(backgroundAction);
    checkNotNull(exceptionType);

    final AtomicReference<T> resultRef = new AtomicReference<>();
    final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
    final WaitDialog waitDialog = new WaitDialog(mainFrame, message);
    final SwingWorker<T, Void> worker =
        new SwingWorker<>() {
          @Override
          protected T doInBackground() throws Exception {
            return backgroundAction.get();
          }

          @Override
          protected void done() {
            try {
              T t = get();
              resultRef.set(t);
              Optional.ofNullable(runOnEdtBeforeDialogClose).ifPresent(c -> c.accept(t));
            } catch (final ExecutionException e) {
              exceptionRef.set(e.getCause());
            } catch (final InterruptedException e) {
              Thread.currentThread().interrupt();
              exceptionRef.set(e);
            } finally {
              waitDialog.setVisible(false);
              waitDialog.dispose();
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
}
