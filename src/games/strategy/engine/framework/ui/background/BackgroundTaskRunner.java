package games.strategy.engine.framework.ui.background;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

public class BackgroundTaskRunner {
  public static void runInBackground(final Component parent, final String waitMessage, final Runnable r) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    final WaitDialog window = new WaitDialog(parent, waitMessage);
    final AtomicBoolean doneWait = new AtomicBoolean(false);
    final Thread t = new Thread(() -> {
      try {
        r.run();
      } finally {
        SwingUtilities.invokeLater(() -> {
          doneWait.set(true);
          window.setVisible(false);
          window.dispose();
        });
      }
    });
    t.start();
    if (!doneWait.get()) {
      window.pack();
      window.setLocationRelativeTo(parent);
      window.setVisible(true);
    }
  }

  public static void runInBackground(final String waitMessage, final Runnable r) {
    if (SwingUtilities.isEventDispatchThread()) {
      runInBackground(null, waitMessage, r);
      return;
    }
    SwingUtilities.invokeLater(() -> runInBackground(null, waitMessage, r));
  }
}
