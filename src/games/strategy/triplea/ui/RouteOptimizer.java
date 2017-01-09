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

  private Point endPoint;
  private final int mapWidth;
  private final int mapHeight;

  public RouteOptimizer(MapData mapData, MapPanel mapPanel) {
    checkNotNull(mapData);
    checkNotNull(mapPanel);
    isInfiniteY = mapData.scrollWrapY();
    isInfiniteX = mapData.scrollWrapX();
    mapWidth = mapPanel.getImageWidth();
    mapHeight = mapPanel.getImageHeight();
  }

  /**
   * Algorithm for finding the shortest path for the given Route
   * 
   * @param route The joints on the Map
   * @return A Point array which goes through Map Borders if necessary
   */
  public Point[] getTranslatedRoute(Point... route) {
    if (route == null || route.length == 0) {
      // Or the array is too small
      return route;
    }
    if (!isInfiniteX && !isInfiniteY) {
      // If the Map is not infinite scrolling, we can safely return the given Points
      endPoint = route[route.length - 1];
      return route;
    }
    List<Point> result = new ArrayList<>();
    Point previousPoint = null;
    for (Point point : route) {
      if (previousPoint == null) {
        previousPoint = point;
        result.add(point);
        continue;
      }
      Point closestPoint = getClosestPoint(previousPoint, getPossiblePoints(point));
      result.add(closestPoint);
      previousPoint = closestPoint;
    }
    endPoint = result.get(result.size() - 1);
    return result.toArray(new Point[result.size()]);
  }

  /**
   * Returns the Closest Point out of the given Pool
   * 
   * @param source the reference Point
   * @param pool Point2D List with all possible options
   * @return the closest point in the Pool to the source
   */
  public static Point getClosestPoint(Point source, List<Point2D> pool) {
    double closestDistance = Double.MAX_VALUE;
    Point closestPoint = null;
    for (Point2D possibleClosestPoint : pool) {
      if (closestPoint == null) {
        closestDistance = source.distance(possibleClosestPoint);
        closestPoint = getPoint(possibleClosestPoint);
      } else {
        double distance = source.distance(possibleClosestPoint);
        if (closestDistance > distance) {
          closestPoint = getPoint(possibleClosestPoint);
          closestDistance = distance;
        }
      }
    }
    return closestPoint;
  }

  /**
   * Method for getting Points, which are a mapHeight/Width away from the actual Point
   * Used to display routes with higher offsets than the map width/height
   * 
   * @param point The Point to "clone"
   * @return A List of all possible Points depending in map Properties
   *         size may vary
   */
  public List<Point2D> getPossiblePoints(Point2D point) {
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

  /**
   * Helper Method to convert a {@linkplain Point2D} to a {@linkplain Point}
   * 
   * @param point a {@linkplain Point2D} object
   * @return a {@linkplain Point} object
   */
  public static Point getPoint(Point2D point) {
    return new Point((int) point.getX(), (int) point.getY());
  }

  public Point getLastEndPoint() {
    return endPoint;
  }

  /**
   * Matrix Transpose method to transpose the 2dimensional point list
   * 
   * @param points A Point array
   * @return Offset Point Arrays including points
   */
  public List<Point[]> getAllPoints(Point... points) {
    List<Point[]> allPoints = new ArrayList<>();
    for (int i = 0; i < points.length; i++) {
      List<Point2D> subPoints = getPossiblePoints(points[i]);
      for (int y = 0; y < subPoints.size(); y++) {
        if (i == 0) {
          allPoints.add(new Point[points.length]);
        }
        allPoints.get(y)[i] = getPoint((subPoints.get(y)));
      }
    }
    return allPoints;
  }

  /**
   * Generates a List of Line2Ds which represent "normalized forms" of the given arrays
   * 
   * @param xcoords an array of xCoordinates
   * @param ycoords an array of yCoordinates
   * @return a List of corresponding Line2Ds
   */
  private List<Line2D> getNormalizedLines(double[] xcoords, double[] ycoords) {
    List<Line2D> lines = new ArrayList<>();
    Point2D previousPoint = null;
    for (int i = 0; i < xcoords.length; i++) {
      Point2D trimmedPoint = new Point2D.Double(xcoords[i], ycoords[i]);
      if (previousPoint != null) {
        lines.add(new Line2D.Double(previousPoint, trimmedPoint));
      }
      previousPoint = trimmedPoint;
    }
    return lines;
  }

  /**
   * A List of Line2Ds which represent all possible lines on multiple screens size may vary
   * 
   * @param xcoords an array of xCoordinates
   * @param ycoords an array of yCoordinates
   * @return a List of corresponding Line2Ds on every possible screen
   */
  public List<Line2D> getAllNormalizedLines(double[] xcoords, double[] ycoords) {
    List<Line2D> centerLines = getNormalizedLines(xcoords, ycoords);
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
