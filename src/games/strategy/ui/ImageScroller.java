package games.strategy.ui;

import javax.swing.JPanel;
import javax.swing.OverlayLayout;

/**
 * Overlays the small view with the large view
 */
public class ImageScroller extends JPanel {
  private static final long serialVersionUID = -794229118989828922L;

  /**
   * Creates new ImageScroller
   */
  public ImageScroller(final ImageScrollerLargeView large, final ImageScrollerSmallView small) {
    final OverlayLayout overlay = new OverlayLayout(this);
    this.setLayout(overlay);
    this.add(small);
    this.add(large);
    small.setAlignmentX(1);
    small.setAlignmentY(0);
  }
}
