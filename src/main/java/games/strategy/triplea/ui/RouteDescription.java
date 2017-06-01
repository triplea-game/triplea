package games.strategy.triplea.ui;

import java.awt.Image;
import java.awt.Point;

import games.strategy.engine.data.Route;

public class RouteDescription {
  private final Route m_route;
  // this point is in map co-ordinates, un scaled
  private final Point m_start;
  // this point is in map co-ordinates, un scaled
  private final Point m_end;
  private final Image m_cursorImage;

  RouteDescription(final Route route, final Point start, final Point end, final Image cursorImage) {
    m_route = route;
    m_start = start;
    m_end = end;
    m_cursorImage = cursorImage;
  }

  @Override
  public int hashCode() {
    return m_route.hashCode() + m_cursorImage.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (o == null) {
      return false;
    }
    final RouteDescription other = (RouteDescription) o;
    if (m_start == null && other.m_start != null || other.m_start == null && m_start != null
        || (m_start != other.m_start && !m_start.equals(other.m_start))) {
      return false;
    }
    if (m_route == null && other.m_route != null || other.m_route == null && m_route != null
        || (m_route != other.m_route && !m_route.equals(other.m_route))) {
      return false;
    }
    if (m_end == null && other.m_end != null || other.m_end == null && m_end != null) {
      return false;
    }
    if (m_cursorImage != other.m_cursorImage) {
      return false;
    }
    // we dont want to be updating for every small change,
    // if the end points are close enough, they are close enough
    if (other.m_end == null && this.m_end == null) {
      return true;
    }
    int xDiff = m_end.x - other.m_end.x;
    xDiff *= xDiff;
    int yDiff = m_end.y - other.m_end.y;
    yDiff *= yDiff;
    final int endDiff = (int) Math.sqrt(xDiff + yDiff);
    return endDiff < 6;
  }

  public Route getRoute() {
    return m_route;
  }

  public Point getStart() {
    return m_start;
  }

  public Point getEnd() {
    return m_end;
  }

  public Image getCursorImage() {
    return m_cursorImage;
  }
}
