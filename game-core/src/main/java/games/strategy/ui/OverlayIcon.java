package games.strategy.ui;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * Make one icon from two.
 */
public class OverlayIcon implements Icon {
  private final Icon back;
  private final Icon front;
  private final int offsetX;
  private final int offsetY;

  /**
   * Create a composite icon by overlaying the front icon over the back icon.
   *
   * @param back
   *        back icon
   * @param front
   *        front icon
   * @param x
   *        , y position of front icon relative to back icon.
   */
  public OverlayIcon(final Icon back, final Icon front, final int x, final int y) {
    this.back = back;
    this.front = front;
    offsetX = x;
    offsetY = y;
  }

  @Override
  public int getIconHeight() {
    return (back.getIconHeight() > (front.getIconHeight() + offsetY)) ? back.getIconHeight()
        : (front.getIconHeight() + offsetY);
  }

  @Override
  public int getIconWidth() {
    return (back.getIconWidth() > (front.getIconWidth() + offsetX)) ? back.getIconWidth()
        : (front.getIconWidth() + offsetX);
  }

  @Override
  public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
    back.paintIcon(c, g, x, y);
    front.paintIcon(c, g, x + offsetX, y + offsetY);
  }
}
