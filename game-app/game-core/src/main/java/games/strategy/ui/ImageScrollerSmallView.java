package games.strategy.ui;

import games.strategy.triplea.ui.logic.MapScrollUtil;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.border.EtchedBorder;
import org.triplea.swing.SwingComponents;

/**
 * A small image that tracks a selection area within a small image. Generally used in conjunction
 * with a ImageScrollerLargeView.
 */
public class ImageScrollerSmallView extends JComponent {
  private static final long serialVersionUID = 7010099211049677928L;
  private final ImageScrollModel model;
  private Image image;
  private final MapData mapData;
  private long lastUpdate = 0;

  public ImageScrollerSmallView(
      final Image image, final ImageScrollModel model, final MapData mapData) {
    this.model = model;
    Util.ensureImageLoaded(image);
    setDoubleBuffered(false);
    this.image = image;
    this.mapData = mapData;
    this.setBorder(new EtchedBorder());
    final int prefWidth = getInsetsWidth() + this.image.getWidth(this);
    final int prefHeight = getInsetsHeight() + this.image.getHeight(this);
    final Dimension prefSize = new Dimension(prefWidth, prefHeight);
    setPreferredSize(prefSize);
    setMinimumSize(prefSize);
    setMaximumSize(prefSize);
    final MouseAdapter mouseListener =
        new MouseAdapter() {
          @Override
          public void mouseClicked(final MouseEvent e) {
            // try to center around the click
            final int x = (int) (e.getX() / getRatioX()) - (model.getBoxWidth() / 2);
            final int y = (int) (e.getY() / getRatioY()) - (model.getBoxHeight() / 2);
            model.set(x, y);
          }
        };
    this.addMouseListener(mouseListener);
    final MouseMotionListener mouseMotionListener =
        new MouseMotionAdapter() {
          @Override
          public void mouseDragged(final MouseEvent e) {
            final long now = System.currentTimeMillis();
            final long minUpdateDelay = 30;
            if (now < lastUpdate + minUpdateDelay) {
              return;
            }
            lastUpdate = now;
            final Rectangle bounds = (Rectangle) getBounds().clone();
            // if the mouse is a little off the screen, allow it to still scroll the screen
            bounds.grow(30, 0);
            if (!bounds.contains(e.getPoint())) {
              return;
            }
            // try to center around the click
            final int x = (int) (e.getX() / getRatioX()) - (model.getBoxWidth() / 2);
            final int y = (int) (e.getY() / getRatioY()) - (model.getBoxHeight() / 2);
            setSelection(x, y);
          }
        };
    this.addMouseMotionListener(mouseMotionListener);
    model.addListener(this::repaint);
  }

  /** Changes the image displayed in this view to {@code image}. */
  public void changeImage(final Image image) {
    Util.ensureImageLoaded(image);
    setDoubleBuffered(false);
    this.image.flush();
    this.image = image;
    final int prefWidth = getInsetsWidth() + this.image.getWidth(this);
    final int prefHeight = getInsetsHeight() + this.image.getHeight(this);
    final Dimension prefSize = new Dimension(prefWidth, prefHeight);
    setPreferredSize(prefSize);
    setMinimumSize(prefSize);
    setMaximumSize(prefSize);
    SwingComponents.redraw(this);
  }

  private int getInsetsWidth() {
    return getInsets().left + getInsets().right;
  }

  private int getInsetsHeight() {
    return getInsets().top + getInsets().bottom;
  }

  @Override
  public void paintComponent(final Graphics g) {
    g.drawImage(image, 0, 0, this);
    drawViewBox((Graphics2D) g);
  }

  private void drawViewBox(final Graphics2D g) {
    if (model.getBoxWidth() > model.getMaxWidth() && model.getBoxHeight() > model.getMaxHeight()) {
      return;
    }
    final double ratioX = getRatioX();
    final double ratioY = getRatioY();
    final double x = model.getX() * ratioX;
    final double y = model.getY() * ratioY;
    final double width = model.getBoxWidth() * ratioX;
    final double height = model.getBoxHeight() * ratioY;
    final Rectangle2D.Double mapBounds =
        new Rectangle2D.Double(0, 0, model.getMaxWidth() * ratioX, model.getMaxHeight() * ratioY);
    final Rectangle2D.Double rect =
        new Rectangle2D.Double(x % mapBounds.width, y % mapBounds.height, width, height);
    g.setClip(mapBounds);
    MapScrollUtil.getPossibleTranslations(
            model.getScrollX(),
            model.getScrollY(),
            (int) Math.round(mapBounds.width),
            (int) Math.round(mapBounds.height))
        .stream()
        .map(t -> t.createTransformedShape(rect))
        .map(Shape::getBounds2D)
        .forEach(r -> drawFilledRect(r, g));
  }

  private void drawFilledRect(final Rectangle2D rect, final Graphics2D g) {
    g.setComposite(
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, mapData.getSmallMapViewerFillAlpha()));
    g.setColor(mapData.getSmallMapViewerFillColor());
    g.fill(rect);
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    g.setColor(mapData.getSmallMapViewerBorderColor());
    g.draw(rect);
  }

  public Image getOffScreenImage() {
    return image;
  }

  private void setSelection(final int x, final int y) {
    model.set(x, y);
  }

  public double getRatioY() {
    return image.getHeight(null) / (double) model.getMaxHeight();
  }

  public double getRatioX() {
    return image.getWidth(null) / (double) model.getMaxWidth();
  }
}
