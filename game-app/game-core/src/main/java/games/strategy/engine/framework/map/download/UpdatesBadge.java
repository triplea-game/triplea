package games.strategy.engine.framework.map.download;

import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.Serial;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.UIManager;

/**
 * A small pill next to the title of the Installed tab with the number of maps that have updates, eg
 * "3 updates"; hidden if there are none. It has the colors of the Update button it points to.
 */
final class UpdatesBadge extends JLabel {
  @Serial private static final long serialVersionUID = 2969414101549021166L;

  UpdatesBadge() {
    setFont(getFont().deriveFont(Font.BOLD, getFont().getSize2D() - 1));
    setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
  }

  void showUpdates(final int updateCount) {
    setText(updateCount == 1 ? "1 update" : updateCount + " updates");
    setVisible(updateCount > 0);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    setForeground(themeColor("Button.default.foreground"));
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D graphics = (Graphics2D) g.create();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setColor(themeColor("Button.default.background"));
    graphics.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
    graphics.dispose();
    super.paintComponent(g);
  }

  private static Color themeColor(final String key) {
    return Optional.ofNullable(UIManager.getColor(key)).orElseGet(LookAndFeel::getAccentColor);
  }
}
