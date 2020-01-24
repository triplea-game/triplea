package games.strategy.triplea.ui;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

public class MouseDetails {
  private final MouseEvent mouseEvent;
  // the x position of the event on the map
  // this is in absolute pixels of the unscaled map
  @SuppressWarnings("checkstyle:MemberName")
  private final double x;
  // the y position of the event on the map
  // this is in absolute pixels of the unscaled map
  @SuppressWarnings("checkstyle:MemberName")
  private final double y;

  public MouseDetails(final MouseEvent mouseEvent, final double x, final double y) {
    this.mouseEvent = mouseEvent;
    this.x = x;
    this.y = y;
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

  /** Returns this point is in the map coordinates, unscaled. */
  public Point getMapPoint() {
    return new Point((int) x, (int) y);
  }

  public int getButton() {
    return mouseEvent.getButton();
  }
}
