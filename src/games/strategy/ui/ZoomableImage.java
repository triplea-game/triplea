package games.strategy.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;

import javax.swing.JComponent;

/**
 * An image that can be zoomed in and out.
 * Warning not finished. Thats why its not public.
 * Development was abandoned when it turned out when it appeared that zooming would be
 * too slow.
 */
class ZoomableImage extends JComponent {
  private static final long serialVersionUID = 3324558973854619020L;
  private double m_zoom = 1.0;
  private final Image m_original;
  private Image m_current;

  /** Creates new MapPanel */
  public ZoomableImage(final Image anImage) {
    ensureImageLoaded(anImage);
    m_original = anImage;
    m_current = anImage;
    final Dimension dim = new Dimension(m_current.getWidth(this), m_current.getHeight(this));
    this.setPreferredSize(dim);
  }

  private void ensureImageLoaded(final Image anImage) {
    if (anImage.getWidth(this) != -1) {
      return;
    }
    final MediaTracker tracker = new MediaTracker(this);
    tracker.addImage(anImage, 1);
    try {
      tracker.waitForAll(1);
    } catch (final InterruptedException e) {
      // ignore interrupted exception
    }
  }

  public double getZoom() {
    return m_zoom;
  }

  public void setZoom(final double newZoom) {
    if (newZoom <= 1.0) {
      throw new IllegalArgumentException("Zoom must be > 1.  Got:" + newZoom);
    }
    m_zoom = newZoom;
    stretchImage();
    final Dimension dim = new Dimension(m_current.getWidth(this), m_current.getHeight(this));
    this.setPreferredSize(dim);
    this.invalidate();
  }

  private void stretchImage() {
    final int width = (int) (m_original.getWidth(this) * m_zoom);
    final int height = (int) (m_original.getHeight(this) * m_zoom);
    m_current = m_original.getScaledInstance(width, height, Image.SCALE_FAST);
  }

  @Override
  public void paint(final Graphics g) {
    // draw the image
    g.drawImage(m_current, 0, 0, this);
    final Rectangle rect = g.getClipBounds();
    if (rect.getWidth() > m_current.getWidth(this) || rect.getHeight() > m_current.getHeight(this)) {
      // we are being asked to draw on a canvas bigger than the image
      // clear the rest of the canvas
      // TODO deal with this.
    }
  }
}
