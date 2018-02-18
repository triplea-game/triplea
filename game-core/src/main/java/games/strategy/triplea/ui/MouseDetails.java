package games.strategy.triplea.ui;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

public class MouseDetails {
  private final MouseEvent mouseEvent;
  // the x position of the event on the map
  // this is in absolute pixels of the unscaled map
  private final double x;
  // the x position of the event on the map
  // this is in absolute pixels of the unscaled map
  private final double y;

  MouseDetails(final MouseEvent mouseEvent, final double x, final double y) {
    super();
    this.mouseEvent = mouseEvent;
    this.x = x;
    this.y = y;
  }

  public MouseEvent getMouseEvent() {
    return mouseEvent;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public boolean isRightButton() {
    return SwingUtilities.isRightMouseButton(mouseEvent);
  }

  public boolean isControlDown() {
    return mouseEvent.isControlDown();
  }

  public boolean isShiftDown() {
    return mouseEvent.isShiftDown();
  }

  public boolean isAltDown() {
    return mouseEvent.isAltDown();
  }

  /**
   * @return this point is in the map co-ordinates, unscaled.
   */
  public Point getMapPoint() {
    return new Point((int) x, (int) y);
  }

  public int getButton() {
    return mouseEvent.getButton();
  }
}
