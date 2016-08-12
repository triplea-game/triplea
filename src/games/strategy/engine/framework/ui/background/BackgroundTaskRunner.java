package games.strategy.engine.framework.ui.background;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

public class BackgroundTaskRunner {
  /** Non-blocking */
  public static void runInBackground(final String waitMessage, final Runnable r) {
    SwingUtilities.invokeLater(() -> {
      final WaitDialog window = new WaitDialog(null, waitMessage);
      // this will center the window
      window.setLocationRelativeTo(null);
      final AtomicBoolean doneWait = new AtomicBoolean(false);
      final Thread t = new Thread(() -> {
        try {
          r.run();
        } finally {
          // clean up the window
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
        window.setVisible(true);
      }
    });
  }
}
