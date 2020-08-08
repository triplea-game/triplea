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
 * Toast message is a popup window that disappears slowly. Code is based on:
 * https://www.geeksforgeeks.org/java-swing-creating-toast-message/
 */
@Builder
public class Toast {

  @Nonnull private final String message;
  @Builder.Default private final Duration sleepTime = Duration.ofSeconds(1);
  private final JFrame parent;

  private final int windowHeight = 75;
  private final int windowWidth = 400;

  public static void showToast(final JFrame parent, final String message) {
    builder().parent(parent).message(message).build().showToast();
  }

  public static void showToast(final String message) {
    showToast(null, message);
  }

  public void showToast() {
    SwingUtilities.invokeLater(this::showInBackground);
  }

  private void showInBackground() {
    final JWindow window = new JWindow(parent);

    window.setLocationRelativeTo(parent);
    window.setBackground(Color.DARK_GRAY);

    final JPanel panel =
        new JPanel() {
          @Override
          public void paintComponent(final Graphics g) {
            final int width = g.getFontMetrics().stringWidth(message);
            final int height = g.getFontMetrics().getHeight();

            // draw the boundary of the toast and fill it
            g.setColor(Color.DARK_GRAY);
            g.drawRect(10, 10, width + 30, height + 10);

            // set the color of text
            g.setColor(new Color(255, 255, 255, 240));
            g.drawString(message, 25, 27);
          }
        };

    window.add(panel);
    window.setSize(windowWidth, windowHeight);

    window.setOpacity(1);
    window.setVisible(true);

    new Thread(
            () -> {
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
            })
        .start();
  }
}
