package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

/**
 * A window that is displayed while loading a game to provide visual feedback to the user during this potentially
 * long-running operation.
 */
public final class WaitWindow extends JWindow {
  private static final long serialVersionUID = -8134956690669346954L;

  private final Object mutex = new Object();
  private Timer timer = new Timer();

  public WaitWindow() {
    final WaitPanel mainPanel = new WaitPanel("Loading game, please wait...");
    setLocationRelativeTo(null);
    mainPanel.setBorder(new LineBorder(Color.BLACK));
    setLayout(new BorderLayout());
    add(mainPanel, BorderLayout.CENTER);

    pack();
    setSize(getSize().width, 80);
  }

  /**
   * Shows the wait window.
   */
  public void showWait() {
    final TimerTask task = new TimerTask() {
      @Override
      public void run() {
        SwingUtilities.invokeLater(() -> toFront());
      }
    };

    synchronized (mutex) {
      if (timer != null) {
        timer.schedule(task, 15, 15);
      }
    }
  }

  /**
   * Hides the wait window.
   */
  public void doneWait() {
    synchronized (mutex) {
      if (timer != null) {
        timer.cancel();
        timer = null;
      }
    }
    SwingUtilities.invokeLater(() -> {
      setVisible(false);
      removeAll();
      dispose();
    });
  }
}
