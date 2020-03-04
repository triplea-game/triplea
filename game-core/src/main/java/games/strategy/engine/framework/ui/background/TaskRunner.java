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

@Builder
public class TaskRunner<E extends Exception> {
  @Nonnull private final Consumer<E> exceptionHandler;
  @Nonnull private final String waitDialogTitle;

  public void run(final ThrowingRunnable<E> backgroundAction) {
    Preconditions.checkNotNull(backgroundAction);
    final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();

    // TODO: after https://github.com/triplea-game/triplea/pull/6001 is merged
    //   change the 'null' parent window reference to main frame.
    final WaitDialog waitDialog = new WaitDialog(null, waitDialogTitle);
    waitDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    final SwingWorker<Void, Void> worker =
        new SwingWorker<>() {
          @Override
          protected Void doInBackground() {
            try {
              backgroundAction.run();
            } catch (final Throwable e) {
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
      throw new AssertionError("unexpected checked exception", e);
    }
  }
}
