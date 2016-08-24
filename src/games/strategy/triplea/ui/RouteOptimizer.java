package games.strategy.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import games.strategy.triplea.ui.mapdata.MapData;

public class RouteOptimizer {

  public final boolean isInfiniteY;
  public final boolean isInfiniteX;

  private final MapPanel mapPanel;

  private Point endPoint;
  private int mapWidth;
  private int mapHeight;

  public RouteOptimizer(MapData mapData, MapPanel mapPanel) {
    checkNotNull(mapData);
    this.mapPanel = checkNotNull(mapPanel);
    isInfiniteY = mapData.scrollWrapY();
    isInfiniteX = mapData.scrollWrapX();
  }

  public Point[] getTranslatedRoute(Point... route) {
    if (route == null) {
      return route;
    }
    if (!isInfiniteX && !isInfiniteY || route.length == 0) {
      // If the Map is not infinite scrolling, we can safely return the given Points
      // Or the array is too small
      return route;
    }
    mapWidth = mapPanel.getImageWidth();
    mapHeight = mapPanel.getImageHeight();
    List<Point> result = new ArrayList<>();
    Point previousPoint = null;
    for (Point point : route) {
      if (previousPoint == null) {
        previousPoint = point;
        result.add(point);
        continue;
      }
      previousPoint = normalizePoint(previousPoint);
      double closestDistance = Double.MAX_VALUE;
      Point closestPoint = null;
      for (Point2D possibleClosestPoint : getPossiblePoints(point)) {
        if (closestPoint == null) {
          closestDistance = previousPoint.distance(point);
          closestPoint = normalizePoint(point);
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
    endPoint = result.get(result.size() - 1);
    return result.toArray(new Point[result.size()]);
  }

  private Point normalizePoint(Point point) {
    return new Point(point.x % mapWidth, point.y % mapHeight);
  }

  private List<Point2D> getPossiblePoints(Point2D point) {
    List<Point2D> result = new ArrayList<>();
    result.add(point);
    if (isInfiniteX && isInfiniteY) {
      result.addAll(Arrays.asList(
          new Point2D.Double(point.getX() - mapWidth, point.getY() - mapHeight),
          new Point2D.Double(point.getX() - mapWidth, point.getY() + mapHeight),
          new Point2D.Double(point.getX() + mapWidth, point.getY() - mapHeight),
          new Point2D.Double(point.getX() + mapWidth, point.getY() + mapHeight)));
    }
    if (isInfiniteX) {
      result.addAll(Arrays.asList(
          new Point2D.Double(point.getX() - mapWidth, point.getY()),
          new Point2D.Double(point.getX() + mapWidth, point.getY())));

    }
    if (isInfiniteY) {
      result.addAll(Arrays.asList(
          new Point2D.Double(point.getX(), point.getY() - mapHeight),
          new Point2D.Double(point.getX(), point.getY() + mapHeight)));

    }
    return result;
  }

  public static Point getPoint(Point2D point) {
    return new Point((int) point.getX(), (int) point.getY());
  }

  public Point getLastEndPoint() {
    return endPoint;
  }

  private List<Point[]> getAlternativePoints(Point... points) {
    List<Point[]> alternativePoints = new ArrayList<>();
    if (isInfiniteX || isInfiniteY) {
      int altArrayCount = getAlternativePointArrayCount();
      for (int i = 0; i < altArrayCount; i++) {
        alternativePoints.add(new Point[points.length]);
      }
      int counter = 0;
      for (Point point : points) {
        Point normalizedPoint = normalizePoint(point);
        if (isInfiniteX) {
          alternativePoints.get(0)[counter] = new Point(normalizedPoint.x - mapWidth, normalizedPoint.y);
          alternativePoints.get(1)[counter] = new Point(normalizedPoint.x + mapWidth, normalizedPoint.y);
        }
        if (isInfiniteY) {
          int index = altArrayCount == 8 ? 2 : 0;
          alternativePoints.get(index)[counter] = new Point(normalizedPoint.x, normalizedPoint.y - mapHeight);
          alternativePoints.get(index + 1)[counter] = new Point(normalizedPoint.x, normalizedPoint.y + mapHeight);
        }
        if (isInfiniteX && isInfiniteY) {
          alternativePoints.get(4)[counter] = new Point(normalizedPoint.x - mapWidth, normalizedPoint.y - mapHeight);
          alternativePoints.get(5)[counter] = new Point(normalizedPoint.x - mapWidth, normalizedPoint.y + mapHeight);
          alternativePoints.get(6)[counter] = new Point(normalizedPoint.x + mapWidth, normalizedPoint.y - mapHeight);
          alternativePoints.get(7)[counter] = new Point(normalizedPoint.x + mapWidth, normalizedPoint.y + mapHeight);
        }
        counter++;
      }
    }
    return alternativePoints;
  }

  public List<Point[]> getAllPoints(Point... points) {
    List<Point[]> allPoints = getAlternativePoints(points);
    Point[] normalizedPoints = new Point[points.length];
    for (int i = 0; i < points.length; i++) {
      normalizedPoints[i] = normalizePoint(points[i]);
    }
    allPoints.add(normalizedPoints);
    return allPoints;
  }

  private int getAlternativePointArrayCount() {
    if (isInfiniteX && isInfiniteY) {
      return 8;
    } else if (isInfiniteX || isInfiniteY) {
      return 2;
    }
    return 0;
  }

  private List<Line2D> getTrimmedLines(double[] xcoords, double[] ycoords) {
    List<Line2D> lines = new ArrayList<>();
    Point2D previousPoint = null;
    for (int i = 0; i < xcoords.length; i++) {
      Point2D point = new Point2D.Double(xcoords[i], ycoords[i]);
      Point2D trimmedPoint = normalizePoint(getPoint(point));
      if (previousPoint != null) {
        lines.add(new Line2D.Double(normalizePoint(getPoint(previousPoint)), trimmedPoint));
      }
      previousPoint = point;
    }
    return lines;
  }

  public List<Line2D> getAllTrimmedLines(double[] xcoords, double[] ycoords) {
    List<Line2D> centerLines = getTrimmedLines(xcoords, ycoords);
    List<Line2D> result = new ArrayList<>();
    for (Line2D line : centerLines) {
      List<Point[]> allPoints = getAllPoints(getPoint(line.getP1()), getPoint(line.getP2()));
      for (Point[] points : allPoints) {
        result.add(new Line2D.Double(points[0], points[1]));
      }
    }
    return result;
  }
}
