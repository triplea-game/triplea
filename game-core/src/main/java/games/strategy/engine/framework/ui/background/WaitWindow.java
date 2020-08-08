package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

/**
 * A window that is displayed while loading a game to provide visual feedback to the user during
 * this potentially long-running operation.
 */
public final class WaitWindow extends JWindow {
  private static final long serialVersionUID = -8134956690669346954L;

  public WaitWindow() {
    setAlwaysOnTop(true);
    setLocationRelativeTo(null);

    final WaitPanel mainPanel = new WaitPanel("Loading game, please wait...");
    mainPanel.setBorder(new LineBorder(Color.BLACK));
    setLayout(new BorderLayout());
    add(mainPanel, BorderLayout.CENTER);

    pack();
    setSize(getSize().width, 80);
  }

  /** Shows the wait window. */
  public void showWait() {
    SwingUtilities.invokeLater(() -> setVisible(true));
  }

  /** Hides the wait window. */
  public void doneWait() {
    SwingUtilities.invokeLater(
        () -> {
          setVisible(false);
          removeAll();
          dispose();
        });
  }
}
