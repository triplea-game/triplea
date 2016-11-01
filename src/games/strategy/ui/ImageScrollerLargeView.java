package games.strategy.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.scrolling.ScrollSettings;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

/**
 * A large image that can be scrolled according to a ImageScrollModel.
 * Generally used in conjunction with a ImageScrollerSmallView.
 * We do not take care of drawing ourselves. All we do is keep track of
 * our location and size. Subclasses must take care of rendering
 */
public class ImageScrollerLargeView extends Pane {

  // bit flags for determining which way we are scrolling
  final static int NONE = 0;
  final static int LEFT = 1;
  final static int RIGHT = 2;
  final static int TOP = 4;
  final static int BOTTOM = 8;

  private final ScrollSettings scrollSettings;

  protected final ImageScrollModel m_model;
  protected double m_scale = 1;

  private int m_drag_scrolling_lastx;
  private int m_drag_scrolling_lasty;

  protected GraphicsContext graphics;

  private final ActionListener m_timerAction = new ActionListener() {
    @Override
    public final void actionPerformed(final ActionEvent e) {

      if (ImageScrollerLargeView.this.getScene().getFocusOwner() == null) {
        m_insideCount = 0;
        return;
      }
      if (m_inside && m_edge != NONE) {
        m_insideCount++;
        if (m_insideCount > 6) {
          // Scroll the map when the mouse has hovered inside the scroll zone for long enough
          SwingUtilities.invokeLater(new Scroller());
        }
      }
    }
  };
  // scrolling
  private final javax.swing.Timer m_timer = new javax.swing.Timer(50, m_timerAction);
  private boolean m_inside = false;
  private int m_insideCount = 0;
  private int m_edge = NONE;
  private final List<ScrollListener> m_scrollListeners = new ArrayList<>();

  public ImageScrollerLargeView(final Dimension dimension, final ImageScrollModel model) {
    super();
    Canvas canvas = new Canvas();
    this.getChildren().add(canvas);
    graphics = canvas.getGraphicsContext2D();
    scrollSettings = ClientContext.scrollSettings();
    m_model = model;
    m_model.setMaxBounds((int) dimension.getWidth(), (int) dimension.getHeight());
    Dimension imageDimensions = getImageDimensions();
    setPrefSize(imageDimensions.getWidth(), imageDimensions.getHeight());
    setMaxSize(imageDimensions.getWidth(), imageDimensions.getHeight());
    EventHandler<ScrollEvent> scrollEvent = e -> {
      if (!e.isAltDown()) {
        if (m_edge == NONE) {
          m_insideCount = 0;
        }
        // compute the amount to move
        int dx = 0;
        int dy = 0;
        if (e.isShiftDown()) {
          dx = (int) (e.getDeltaX() * scrollSettings.getWheelScrollAmount());
          dy = (int) (e.getDeltaY() * scrollSettings.getWheelScrollAmount());
        } else {
          dx = (int) (e.getDeltaY() * scrollSettings.getWheelScrollAmount());
          dy = (int) (e.getDeltaX() * scrollSettings.getWheelScrollAmount());
        }
        // move left and right and test for wrap
        int newX = (m_model.getX() + dx);
        if (newX > m_model.getMaxWidth() - getWidth()) {
          newX -= m_model.getMaxWidth();
        }
        if (newX < -getWidth()) {
          newX += m_model.getMaxWidth();
        }
        // move up and down and test for edges
        final int newY = m_model.getY() + dy;
        // update the map
        m_model.set(newX, newY);
      } else {
        double value = m_scale;
        int positive = 1;
        if (e.getTextDeltaX() > 0) {
          positive = -1;
        }
        if ((positive > 0 && value == 1) || (positive < 0 && value <= .21)) {
          return;
        }
        if (positive > 0) {
          if (value >= .79) {
            value = 1.0;
          } else if (value >= .59) {
            value = .8;
          } else if (value >= .39) {
            value = .6;
          } else if (value >= .19) {
            value = .4;
          } else {
            value = .2;
          }
        } else {
          if (value <= .41) {
            value = .2;
          } else if (value <= .61) {
            value = .4;
          } else if (value <= .81) {
            value = .6;
          } else if (value <= 1.0) {
            value = .8;
          } else {
            value = 1.0;
          }
        }
        setScale(value);
      }
    };
    setOnScroll(scrollEvent);
    addEventHandler(MouseEvent.MOUSE_ENTERED, e -> m_timer.start());
    addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
      m_inside = false;
      m_timer.stop();
    });
    addEventHandler(MouseEvent.MOUSE_CLICKED, e -> requestFocus());
    addEventHandler(MouseEvent.MOUSE_RELEASED, e -> requestFocus());
    addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
      // try to center around the click
      m_drag_scrolling_lastx = (int) e.getX();
      m_drag_scrolling_lasty = (int) e.getY();
    });
    addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
      m_inside = true;
      final int x = (int) e.getX();
      final int y = (int) e.getY();
      final int height = (int) getHeight();
      final int width = (int) getWidth();
      m_edge = getNewEdge(x, y, width, height);
      if (m_edge == NONE) {
        m_insideCount = 0;
      }
    });

    addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
      /*
       * this is used to detect drag scrolling
       */
      requestFocus();
      // the right button must be the one down
      if (e.getButton().equals(MouseButton.SECONDARY)) {
        m_inside = false;
        // read in location
        final int x = (int) e.getX();
        final int y = (int) e.getY();
        if (m_edge == NONE) {
          m_insideCount = 0;
        }
        // compute the amount to move
        final int dx = (m_drag_scrolling_lastx - x);
        final int dy = (m_drag_scrolling_lasty - y);
        // move left and right and test for wrap
        final int newX = (m_model.getX() + dx);
        // move up and down and test for edges
        final int newY = m_model.getY() + dy;
        // update the map
        m_model.set(newX, newY);
        // store the location of the mouse for the next move
        m_drag_scrolling_lastx = (int) e.getX();
        m_drag_scrolling_lasty = (int) e.getY();
      }
    });
    getScene().widthProperty().addListener((value, oldWidth, newWidth) -> refreshBoxSize());
    getScene().heightProperty().addListener((value, oldWidth, newWidth) -> refreshBoxSize());
    m_timer.start();
    m_model.addObserver((o, arg) -> {
      repaint();
      notifyScollListeners();
    });
  }

  /**
   * For subclasses needing to set the location of the image.
   */
  protected void setTopLeft(final int x, final int y) {
    m_model.set(x, y);
  }

  protected void repaint() {

  }

  protected void setTopLeftNoWrap(int x, int y) {
    if (x < 0) {
      x = 0;
    }
    if (y < 0) {
      y = 0;
    }
    m_model.set(x, y);
  }

  public int getImageWidth() {
    return m_model.getMaxWidth();
  }

  public int getImageHeight() {
    return m_model.getMaxHeight();
  }

  public void addScrollListener(final ScrollListener s) {
    m_scrollListeners.add(s);
  }

  public void removeScrollListener(final ScrollListener s) {
    m_scrollListeners.remove(s);
  }

  private void notifyScollListeners() {
    for (final ScrollListener element : new ArrayList<>(m_scrollListeners)) {
      element.scrolled(m_model.getX(), m_model.getY());
    }
  }

  private void scroll() {
    int dy = 0;
    if ((m_edge & TOP) != 0) {
      dy = -scrollSettings.getMapEdgeScrollSpeed();
    } else if ((m_edge & BOTTOM) != 0) {
      dy = scrollSettings.getMapEdgeScrollSpeed();
    }
    int dx = 0;
    if ((m_edge & LEFT) != 0) {
      dx = -scrollSettings.getMapEdgeScrollSpeed();
    } else if ((m_edge & RIGHT) != 0) {
      dx = scrollSettings.getMapEdgeScrollSpeed();
    }

    dx = (int) (dx / m_scale);
    dy = (int) (dy / m_scale);
    final int newX = (m_model.getX() + dx);
    final int newY = m_model.getY() + dy;
    m_model.set(newX, newY);
  }

  public Dimension getImageDimensions() {
    return new Dimension(m_model.getMaxWidth(), m_model.getMaxHeight());
  }

  private int getNewEdge(final int x, final int y, final int width, final int height) {
    int newEdge = NONE;
    if (x < scrollSettings.getMapEdgeScrollZoneSize()) {
      newEdge += LEFT;
    } else if (width - x < scrollSettings.getMapEdgeScrollZoneSize()) {
      newEdge += RIGHT;
    }
    if (y < scrollSettings.getMapEdgeScrollZoneSize()) {
      newEdge += TOP;
    } else if (height - y < scrollSettings.getMapEdgeScrollZoneSize()) {
      newEdge += BOTTOM;
    }
    return newEdge;
  }

  protected void refreshBoxSize() {
    m_model.setBoxDimensions((int) (getWidth() / m_scale), (int) (getHeight() / m_scale));
  }

  /**
   * @param value The new scale value. Constrained to the bounds of no less than 0.15 and no greater than 1.
   *        If out of bounds the nearest boundary value is used.
   */
  public void setScale(double value) {
    if (value < 0.15) {
      value = 0.15;
    }
    if (value > 1) {
      value = 1;
    }
    // we want the ratio to be a multiple of 1/256
    // so that the tiles have integer widths and heights
    value = ((int) (value * 256)) / ((double) 256);
    m_scale = value;
    refreshBoxSize();
  }

  /**
   * Update will not be seen until update is called. Resets the offscreen
   * image to the original.
   */
  public int getXOffset() {
    return m_model.getX();
  }

  public int getYOffset() {
    return m_model.getY();
  }

  private class Scroller implements Runnable {
    @Override
    public void run() {
      scroll();
    }
  }

  protected double getScaledWidth() {
    return getWidth() / m_scale;
  }

  protected double getScaledHeight() {
    return getHeight() / m_scale;
  }

  public void deactivate() {
    m_timer.stop();
    m_timer.removeActionListener(m_timerAction);
  }
}
