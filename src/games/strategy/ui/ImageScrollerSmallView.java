package games.strategy.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.border.EtchedBorder;

/**
 * A small image that tracks a selection area within a small image. Generally
 * used in conjunction with a ImageScrollerLarrgeView.
 */
public class ImageScrollerSmallView extends JComponent {
  private static final long serialVersionUID = 7010099211049677928L;
  private final ImageScrollModel m_model;
  private Image m_image;

  public ImageScrollerSmallView(final Image image, final ImageScrollModel model) {
    m_model = model;
    Util.ensureImageLoaded(image);
    setDoubleBuffered(false);
    m_image = image;
    this.setBorder(new EtchedBorder());
    final int prefWidth = getInsetsWidth() + m_image.getWidth(this);
    final int prefHeight = getInsetsHeight() + m_image.getHeight(this);
    final Dimension prefSize = new Dimension(prefWidth, prefHeight);
    setPreferredSize(prefSize);
    setMinimumSize(prefSize);
    setMaximumSize(prefSize);
    MouseAdapter MOUSE_LISTENER = new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        // try to center around the click
        final int x = (int) (e.getX() / getRatioX()) - (m_model.getBoxWidth() / 2);
        final int y = (int) (e.getY() / getRatioY()) - (m_model.getBoxHeight() / 2);
        m_model.set(x, y);
      }
    };
    this.addMouseListener(MOUSE_LISTENER);
    MouseMotionListener MOUSE_MOTION_LISTENER = new MouseMotionAdapter() {
      @Override
      public void mouseDragged(final MouseEvent e) {
        final long now = System.currentTimeMillis();
        long MIN_UPDATE_DELAY = 30;
        if (now < mLastUpdate + MIN_UPDATE_DELAY) {
          return;
        }
        mLastUpdate = now;
        final Rectangle bounds = (Rectangle) getBounds().clone();
        // if the mouse is a little off the screen, allow it to still scroll
        // the screen
        bounds.grow(30, 0);
        if (!bounds.contains(e.getPoint())) {
          return;
        }
        // try to center around the click
        final int x = (int) (e.getX() / getRatioX()) - (m_model.getBoxWidth() / 2);
        final int y = (int) (e.getY() / getRatioY()) - (m_model.getBoxHeight() / 2);
        setSelection(x, y);
      }
    };
    this.addMouseMotionListener(MOUSE_MOTION_LISTENER);
    model.addObserver(new Observer() {
      @Override
      public void update(final Observable o, final Object arg) {
        repaint();
      }
    });
  }

  public void changeImage(final Image image) {
    Util.ensureImageLoaded(image);
    setDoubleBuffered(false);
    m_image.flush();
    m_image = image;
    final int prefWidth = getInsetsWidth() + m_image.getWidth(this);
    final int prefHeight = getInsetsHeight() + m_image.getHeight(this);
    final Dimension prefSize = new Dimension(prefWidth, prefHeight);
    setPreferredSize(prefSize);
    setMinimumSize(prefSize);
    setMaximumSize(prefSize);
    this.validate();
    this.repaint();
  }

  private int getInsetsWidth() {
    return getInsets().left + getInsets().right;
  }

  private int getInsetsHeight() {
    return getInsets().top + getInsets().bottom;
  }

  void setCoords(final int x, final int y) {
    m_model.set(x, y);
  }

  public Dimension getImageDimensions() {
    return Util.getDimension(m_image, this);
  }

  @Override
  public void paintComponent(final Graphics g) {
    g.drawImage(m_image, 0, 0, this);
    g.setColor(Color.lightGray);
    drawViewBox((Graphics2D) g);
  }

  private void drawViewBox(final Graphics2D g) {
    if (m_model.getBoxWidth() > m_model.getMaxWidth() && m_model.getBoxHeight() > m_model.getMaxHeight()) {
      return;
    }
    final double ratioX = getRatioX();
    final double ratioY = getRatioY();
    final double x = m_model.getX() * ratioX;
    final double y = m_model.getY() * ratioY;
    final double width = m_model.getBoxWidth() * ratioX;
    final double height = m_model.getBoxHeight() * ratioY;
    final Rectangle2D.Double rect = new Rectangle2D.Double(x, y, width, height);
    g.draw(rect);
    if (m_model.getScrollX()) {
      final double mapWidth = m_model.getMaxWidth() * ratioX;
      rect.x += mapWidth;
      g.draw(rect);
      rect.x -= 2 * mapWidth;
      g.draw(rect);
    }
  }

  public Image getOffScreenImage() {
    return m_image;
  }

  private void setSelection(final int x, final int y) {
    m_model.set(x, y);
  }

  private long mLastUpdate = 0;

  public double getRatioY() {
    return m_image.getHeight(null) / (double) m_model.getMaxHeight();
  }

  public double getRatioX() {
    return m_image.getWidth(null) / (double) m_model.getMaxWidth();
  }
}
