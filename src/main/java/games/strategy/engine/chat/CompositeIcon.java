package games.strategy.engine.chat;

import java.awt.Component;
import java.awt.Graphics;
import java.util.List;

import javax.swing.Icon;

public class CompositeIcon implements Icon {
  private static final int GAP = 2;
  private final List<Icon> icons;

  CompositeIcon(final List<Icon> icons) {
    this.icons = icons;
  }

  @Override
  public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
    int dx = 0;
    for (final Icon icon : icons) {
      icon.paintIcon(c, g, x + dx, y);
      dx += GAP;
      dx += icon.getIconWidth();
    }
  }

  @Override
  public int getIconWidth() {
    int sum = 0;
    for (final Icon icon : icons) {
      sum += icon.getIconWidth();
      sum += GAP;
    }
    return sum;
  }

  @Override
  public int getIconHeight() {
    int max = 0;
    for (final Icon icon : icons) {
      max = Math.max(icon.getIconHeight(), max);
    }
    return max;
  }
}
