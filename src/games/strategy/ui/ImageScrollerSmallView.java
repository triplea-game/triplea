package games.strategy.ui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * A small image that tracks a selection area within a small image. Generally
 * used in conjunction with a ImageScrollerLarrgeView.
 */
public class ImageScrollerSmallView extends Pane {
  private final ImageScrollModel m_model;
  private BufferedImage m_image;
  private GraphicsContext g;

  public ImageScrollerSmallView(final Image image, final ImageScrollModel model) {
    m_model = model;
    Util.ensureImageLoaded(image);
    m_image = (BufferedImage) image;
    Canvas canvas = new Canvas();
    getChildren().add(canvas);
    g = canvas.getGraphicsContext2D();
    // this.setBorder(new EtchedBorder());TODO with CSS
    final int prefWidth = getInsetsWidth() + m_image.getWidth();
    final int prefHeight = getInsetsHeight() + m_image.getHeight();
    setPrefSize(prefWidth, prefHeight);
    setMinSize(prefWidth, prefHeight);
    setMaxSize(prefWidth, prefHeight);
    addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
      // try to center around the click
      final int x = (int) (e.getX() / getRatioX()) - (m_model.getBoxWidth() / 2);
      final int y = (int) (e.getY() / getRatioY()) - (m_model.getBoxHeight() / 2);
      m_model.set(x, y);
    });
    addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
      final long now = System.currentTimeMillis();
      final long MIN_UPDATE_DELAY = 30;
      if (now < mLastUpdate + MIN_UPDATE_DELAY) {
        return;
      }
      mLastUpdate = now;
      final Bounds localBounds = getBoundsInLocal();
      // if the mouse is a little off the screen, allow it to still scroll
      // the screen
      BoundingBox bounds = new BoundingBox(localBounds.getMinX() - 30, localBounds.getMinY(),
          localBounds.getMaxX() + 30, localBounds.getMaxY());
      if (!bounds.contains(new Point2D(e.getX(), e.getY()))) {
        return;
      }
      // try to center around the click
      final int x = (int) (e.getX() / getRatioX()) - (m_model.getBoxWidth() / 2);
      final int y = (int) (e.getY() / getRatioY()) - (m_model.getBoxHeight() / 2);
      setSelection(x, y);
    });
    // model.addObserver((o, arg) -> repaint());
  }

  public void changeImage(final Image image) {
    Util.ensureImageLoaded(image);
    m_image.flush();
    m_image = (BufferedImage) image;
    final int prefWidth = getInsetsWidth() + m_image.getWidth();
    final int prefHeight = getInsetsHeight() + m_image.getHeight();
    setPrefSize(prefWidth, prefHeight);
    setMinSize(prefWidth, prefHeight);
    setMaxSize(prefWidth, prefHeight);
  }

  private int getInsetsWidth() {
    return (int) (getInsets().getLeft() + getInsets().getRight());
  }

  private int getInsetsHeight() {
    return (int) (getInsets().getTop() + getInsets().getBottom());
  }

  void setCoords(final int x, final int y) {
    m_model.set(x, y);
  }

  public Dimension getImageDimensions() {
    return Util.getDimension(m_image);
  }

  // TODO make drawing every frame
  public void paintComponent() {
    g.drawImage(SwingFXUtils.toFXImage(m_image, null), 0, 0);
    g.setFill(Color.LIGHTGRAY);
    drawViewBox(g);
  }

  private void drawViewBox(final GraphicsContext g2) {
    if (m_model.getBoxWidth() > m_model.getMaxWidth() && m_model.getBoxHeight() > m_model.getMaxHeight()) {
      return;
    }
    final double ratioX = getRatioX();
    final double ratioY = getRatioY();
    double x = m_model.getX() * ratioX;
    final double y = m_model.getY() * ratioY;
    final double width = m_model.getBoxWidth() * ratioX;
    final double height = m_model.getBoxHeight() * ratioY;
    g2.strokeRect(x, y, width, height);
    if (m_model.getScrollX()) {
      final double mapWidth = m_model.getMaxWidth() * ratioX;
      x += mapWidth;
      g2.strokeRect(x, y, width, height);
      x -= 2 * mapWidth;
      g2.strokeRect(x, y, width, height);
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
