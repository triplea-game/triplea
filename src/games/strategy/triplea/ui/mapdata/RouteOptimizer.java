package games.strategy.triplea.ui.mapdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import games.strategy.triplea.ui.MapPanel;

public class RouteOptimizer {

  private MapData mapData;
  private MapPanel mapPanel;
  private int xOffset;
  private int yOffset;
  private double scale;
  private int mapWidth;
  private int mapHeight;
  private Point endPoint;
  private boolean lastRouteHadMultipleParts = false;

  public RouteOptimizer(MapData mapData, MapPanel mapPanel) {
    this.mapData = checkNotNull(mapData);
    this.mapPanel = checkNotNull(mapPanel);
    mapWidth = mapPanel.getImageWidth();
    mapHeight = mapPanel.getImageHeight();
  }

  public Point[] getTranslatedRoute(Point... route) {
    if (route == null) {
      return route;
    }
    if (!mapData.scrollWrapX() && !mapData.scrollWrapY() || route.length == 0) {
      // If the Map is not infinite scrolling, we can safely return the given Points
      // Or the array is too small
      return route;
    }
    xOffset = mapPanel.getXOffset();
    yOffset = mapPanel.getYOffset();
    scale = mapPanel.getScale();
    List<Point> result = new ArrayList<>();
    Point previousPoint = null;
    for (Point point : route) {
      if (previousPoint == null) {
        previousPoint = point;
        result.add(getFirstPointVisible(point));
        continue;
      }
      previousPoint = normalizePoint(previousPoint);
      double closestDistance = Double.MAX_VALUE;
      Point closestPoint = null;
      for (Point2D possibleClosestPoint : getPossiblePoints(point)) {
        if (closestPoint == null) {
          closestDistance = previousPoint.distance(point);
          closestPoint = point;
        } else {
          double distance = previousPoint.distance(possibleClosestPoint);
          if (closestDistance > distance) {
            closestPoint = getPoint(possibleClosestPoint);
            closestDistance = distance;
          }
        }
      }
      result.add(closestPoint);
      previousPoint = closestPoint;
    }
    lastRouteHadMultipleParts = hasMultipleParts(route);
    endPoint = result.get(result.size() - 1);
    return result.toArray(new Point[result.size()]);
  }

  private boolean hasMultipleParts(Point[] route) {
    if(Math.abs(route[0].getX() - route[route.length - 1].getX()) > mapWidth
        || Math.abs(route[0].getY() - route[route.length - 1].getY()) > mapHeight){
      return true;
    }
    return false;
  }

  private Point normalizePoint(Point point) {
    return new Point((int) point.getX() % mapWidth, (int) point.getY() % mapHeight);
  }

  private boolean isVisible(final Point2D point) {
    final Point2D scaledPoint = new Point2D.Double((point.getX() - xOffset) * scale, (point.getY() - yOffset) * scale);
    if (mapPanel.getBounds().contains(scaledPoint)) {
      return true;
    }
    return false;
  }

  private Point getFirstPointVisible(Point point) {
    List<Point2D> possiblePoints = getPossiblePoints(point);
    for (Point2D possiblePoint : possiblePoints) {
      if (isVisible(possiblePoint)) {
        return getPoint(possiblePoint);
      }
    }
    return point;
  }

  private List<Point2D> getPossiblePoints(Point2D point) {
    List<Point2D> result = new ArrayList<>();
    result.add(point);
    if (mapData.scrollWrapX() && mapData.scrollWrapY()) {
      result.addAll(Arrays.asList(
          new Point2D.Double(point.getX() - mapWidth, point.getY() - mapHeight),
          new Point2D.Double(point.getX() - mapWidth, point.getY()),
          new Point2D.Double(point.getX() - mapWidth, point.getY() + mapHeight),
          new Point2D.Double(point.getX(), point.getY() - mapHeight),
          new Point2D.Double(point.getX(), point.getY() + mapHeight),
          new Point2D.Double(point.getX() + mapWidth, point.getY() - mapHeight),
          new Point2D.Double(point.getX() + mapWidth, point.getY()),
          new Point2D.Double(point.getX() + mapWidth, point.getY() + mapHeight)));
    } else if (mapData.scrollWrapX()) {
      result.addAll(Arrays.asList(
          new Point2D.Double(point.getX() - mapWidth, point.getY()),
          new Point2D.Double(point.getX() + mapWidth, point.getY())));

    } else if (mapData.scrollWrapY()) {
      result.addAll(Arrays.asList(
          new Point2D.Double(point.getX(), point.getY() - mapHeight),
          new Point2D.Double(point.getX(), point.getY() + mapHeight)));

    }
    return result;
  }

  private Point getPoint(Point2D point) {
    return new Point((int) point.getX(), (int) point.getY());
  }

  public Point getLastEndPoint() {
    return endPoint;
  }
  
  public boolean hadLastRouteMultipleParts(){
    return lastRouteHadMultipleParts;
  }
}
