package games.strategy.engine.framework.map.download;

import java.awt.Rectangle;
import java.io.Serial;
import javax.swing.SwingConstants;
import org.triplea.swing.ScrollableJPanel;

/**
 * The scrolling content of the maps window's list and details. {@link ScrollableJPanel} scrolls 10
 * pixels per step, too slow for map rows about 100 pixels tall: here a mouse wheel notch (three
 * steps) scrolls about one row, and Page Up/Down or a click on the scroll bar a whole page.
 */
final class MapScrollPanel extends ScrollableJPanel {
  @Serial private static final long serialVersionUID = 4113853205276094742L;
  // about two lines of text
  private static final int UNIT_INCREMENT = 40;

  @Override
  public int getScrollableUnitIncrement(
      final Rectangle visibleRect, final int orientation, final int direction) {
    return UNIT_INCREMENT;
  }

  @Override
  public int getScrollableBlockIncrement(
      final Rectangle visibleRect, final int orientation, final int direction) {
    return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
  }
}
