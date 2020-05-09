package org.triplea.swing;

import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

/**
 * A JPanel implementing Scrollable, with the behavior similar to a JTextArea for resizing. That is,
 * to have the panel's width updated based on the size of the outer scroll area (so if a window is
 * resized down, so is the panel), via getScrollableTracksViewportWidth() returning true. This is
 * especially when used in conjunction with WrapLayout (either as the layout of the panel itself or
 * on some of its children), so that the content is reflowed when the size is updated.
 */
public class ScrollableJPanel extends JPanel implements Scrollable {
  private static final long serialVersionUID = 1L;

  @Override
  public Dimension getPreferredSize() {
    final Dimension d = super.getPreferredSize();
    if (getParent() instanceof JViewport) {
      // When directly inside a viewport, preferred height is the greater of the underlying
      // preferred or the viewport's size - so that we take the available space.
      d.height = Math.max(d.height, ((JViewport) getParent()).getHeight());
    }
    return d;
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  @Override
  public int getScrollableBlockIncrement(
      final Rectangle visibleRect, final int orientation, final int direction) {
    return 10;
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  @Override
  public int getScrollableUnitIncrement(
      final Rectangle visibleRect, final int orientation, final int direction) {
    return 10;
  }
}
