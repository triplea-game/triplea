package games.strategy.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.scrolling.ScrollSettings;

/**
 * A large image that can be scrolled according to a ImageScrollModel.
 * Generally used in conjunction with a ImageScrollerSmallView.
 * We do not take care of drawing ourselves. All we do is keep track of
 * our location and size. Subclasses must take care of rendering
 */
public class ImageScrollerLargeView extends JComponent {

  private static final long serialVersionUID = -7212817233833868483L;

  // bit flags for determining which way we are scrolling
  static final int NONE = 0;
  static final int LEFT = 1;
  static final int RIGHT = 2;
  static final int TOP = 4;
  static final int BOTTOM = 8;

  private final ScrollSettings scrollSettings;

  protected final ImageScrollModel m_model;
  protected double m_scale = 1;

  private int m_drag_scrolling_lastx;
  private int m_drag_scrolling_lasty;
  private boolean wasLastActionDragging = false;
  
  public boolean wasLastActionDraggingAndReset() {
    if (wasLastActionDragging) {
      wasLastActionDragging = false;
      return true;
    }
    return false;
  }

  private final ActionListener m_timerAction = new ActionListener() {
    @Override
    public final void actionPerformed(final ActionEvent e) {
      if (JOptionPane.getFrameForComponent(ImageScrollerLargeView.this).getFocusOwner() == null) {
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
    scrollSettings = ClientContext.scrollSettings();
    m_model = model;
    m_model.setMaxBounds((int) dimension.getWidth(), (int) dimension.getHeight());
    setPreferredSize(getImageDimensions());
    setMaximumSize(getImageDimensions());
    final MouseWheelListener MOUSE_WHEEL_LISTENER = e -> {
      if (!e.isAltDown()) {
        if (m_edge == NONE) {
          m_insideCount = 0;
        }
        // compute the amount to move
        int dx = 0;
        int dy = 0;
        if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK) {
          dx = e.getWheelRotation() * scrollSettings.getWheelScrollAmount();
        } else {
          dy = e.getWheelRotation() * scrollSettings.getWheelScrollAmount();
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
        if (e.getUnitsToScroll() > 0) {
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
    addMouseWheelListener(MOUSE_WHEEL_LISTENER);
    final MouseAdapter MOUSE_LISTENER = new MouseAdapter() {
      @Override
      public void mouseEntered(final MouseEvent e) {
        m_timer.start();
      }

      @Override
      public void mouseExited(final MouseEvent e) {
        m_inside = false;
        m_timer.stop();
      }

      @Override
      public void mouseClicked(final MouseEvent e) {
        requestFocusInWindow();
      }

      @Override
      public void mouseReleased(final MouseEvent e) {
        requestFocusInWindow();
      }
    };
    addMouseListener(MOUSE_LISTENER);
    final MouseAdapter MOUSE_LISTENER_DRAG_SCROLLING = new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        // try to center around the click
        m_drag_scrolling_lastx = e.getX();
        m_drag_scrolling_lasty = e.getY();
      }
    };
    addMouseListener(MOUSE_LISTENER_DRAG_SCROLLING);
    final MouseMotionListener MOUSE_MOTION_LISTENER = new MouseMotionAdapter() {
      @Override
      public void mouseMoved(final MouseEvent e) {
        m_inside = true;
        final int x = e.getX();
        final int y = e.getY();
        final int height = getHeight();
        final int width = getWidth();
        m_edge = getNewEdge(x, y, width, height);
        if (m_edge == NONE) {
          m_insideCount = 0;
        }
      }
    };
    addMouseMotionListener(MOUSE_MOTION_LISTENER);
    /*
     * this is used to detect drag scrolling
     */
    final MouseMotionListener MOUSE_DRAG_LISTENER = new MouseMotionAdapter() {
      @Override
      public void mouseDragged(final MouseEvent e) {
        requestFocusInWindow();
        // the right button must be the one down
        if ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0 || (e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0) {
          wasLastActionDragging = true;
          m_inside = false;
          // read in location
          final int x = e.getX();
          final int y = e.getY();
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
          m_drag_scrolling_lastx = e.getX();
          m_drag_scrolling_lasty = e.getY();
        }
      }
    };
    addMouseMotionListener(MOUSE_DRAG_LISTENER);
    final ComponentListener COMPONENT_LISTENER = new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        refreshBoxSize();
      }
    };
    addComponentListener(COMPONENT_LISTENER);
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
