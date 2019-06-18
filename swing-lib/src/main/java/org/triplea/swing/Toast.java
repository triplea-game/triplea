package org.triplea.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.time.Duration;

import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import lombok.Builder;

/**
 * Toast message is a popup window that disappears slowly.
 * Code is based on: https://www.geeksforgeeks.org/java-swing-creating-toast-message/
 */
@Builder
public class Toast {

  @Nonnull
  private final String message;
  @Nonnull
  private final Duration sleepTime;
  @Nonnull
  private final JFrame parent;

  private final int windowHeight = 75;
  private final int windowWidth = 400;

  public void showtoast() {
    SwingUtilities.invokeLater(this::showInBackground);
  }

  private void showInBackground() {
    final JWindow window = new JWindow(parent);

    window.setLocationRelativeTo(parent);
    // make the background transparent
    window.setBackground(Color.DARK_GRAY);

    // create a panel
    final JPanel panel = new JPanel() {
      @Override
      public void paintComponent(final Graphics g) {
        final int width = g.getFontMetrics().stringWidth(message);
        final int height = g.getFontMetrics().getHeight();

        // draw the boundary of the toast and fill it
        g.setColor(Color.DARK_GRAY);
        g.fillRect(10, 10, width + 30, height + 10);
        // g.setColor(Color.black);
        g.drawRect(10, 10, width + 30, height + 10);

        // set the color of text
        g.setColor(new Color(255, 255, 255, 240));
        g.drawString(message, 25, 27);
        int t = 250;

        // draw the shadow of the toast
        for (int i = 0; i < 4; i++) {
          t -= 60;
          g.setColor(new Color(0, 0, 0, t));
          g.drawRect(10 - i, 10 - i, width + 30 + i * 2,
              height + 10 + i * 2);
        }
      }
    };

    window.add(panel);
    window.setSize(windowWidth, windowHeight);

    window.setOpacity(1);
    window.setVisible(true);

    new Thread(() -> {
      // wait for some time
      try {
        Thread.sleep(sleepTime.toMillis());
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      // make the message disappear slowly
      for (double d = 1.0; d > 0.2; d -= 0.1) {
        try {
          Thread.sleep(100);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        final float newOpacity = (float) d;
        SwingUtilities.invokeLater(() -> window.setOpacity(newOpacity));
      }

      SwingUtilities.invokeLater(window::dispose);
    }).start();
  }
}
