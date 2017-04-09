package games.strategy.triplea.ui.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteCalculator {

  public final boolean isInfiniteY;
  public final boolean isInfiniteX;

  private Point endPoint;
  private final int mapWidth;
  private final int mapHeight;

  public RouteCalculator(boolean isInfiniteX, boolean isInfiniteY, int mapWidth, int mapHeight) {
    this.isInfiniteX = isInfiniteX;
    this.isInfiniteY = isInfiniteY;
    this.mapWidth = mapWidth;
    this.mapHeight = mapHeight;
  }

  /**
   * Algorithm for finding the shortest path for the given Route.
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
   * Returns the Closest Point out of the given Pool.
   * 
   * @param source the reference Point
   * @param pool Point List with all possible options
   * @return the closest point in the Pool to the source
   */
  public static Point getClosestPoint(Point source, List<Point> pool) {
    double closestDistance = Double.MAX_VALUE;
    Point closestPoint = null;
    for (Point possibleClosestPoint : pool) {
      if (closestPoint == null) {
        closestDistance = source.distance(possibleClosestPoint);
        closestPoint = possibleClosestPoint;
      } else {
        double distance = source.distance(possibleClosestPoint);
        if (closestDistance > distance) {
          closestPoint = possibleClosestPoint;
          closestDistance = distance;
        }
      }
    }
    return closestPoint;
  }

  /**
   * Method for getting Points, which are a mapHeight/Width away from the actual Point
   * Used to display routes with higher offsets than the map width/height.
   * 
   * @param point The Point to "clone"
   * @return A List of all possible Points depending in map Properties
   *         size may vary
   */
  public List<Point> getPossiblePoints(Point point) {
    List<Point> result = new ArrayList<>();
    result.add(point);
    if (isInfiniteX && isInfiniteY) {
      result.addAll(Arrays.asList(
          new Point(point.getX() - mapWidth, point.getY() - mapHeight),
          new Point(point.getX() - mapWidth, point.getY() + mapHeight),
          new Point(point.getX() + mapWidth, point.getY() - mapHeight),
          new Point(point.getX() + mapWidth, point.getY() + mapHeight)));
    }
    if (isInfiniteX) {
      result.addAll(Arrays.asList(
          new Point(point.getX() - mapWidth, point.getY()),
          new Point(point.getX() + mapWidth, point.getY())));

    }
    if (isInfiniteY) {
      result.addAll(Arrays.asList(
          new Point(point.getX(), point.getY() - mapHeight),
          new Point(point.getX(), point.getY() + mapHeight)));

    }
    return result;
  }

  public Point getLastEndPoint() {
    return endPoint;
  }

  /**
   * Matrix Transpose method to transpose the 2dimensional point list.
   * 
   * @param points A Point array
   * @return Offset Point Arrays including points
   */
  public List<Point[]> getAllPoints(Point... points) {
    List<Point[]> allPoints = new ArrayList<>();
    for (int i = 0; i < points.length; i++) {
      List<Point> subPoints = getPossiblePoints(points[i]);
      for (int y = 0; y < subPoints.size(); y++) {
        if (i == 0) {
          allPoints.add(new Point[points.length]);
        }
        allPoints.get(y)[i] = (subPoints.get(y));
      }
    }
    return allPoints;
  }

  /**
   * Generates a List of Lines which represent "normalized forms" of the given arrays.
   * 
   * @param xcoords an array of xCoordinates
   * @param ycoords an array of yCoordinates
   * @return a List of corresponding Lines
   */
  private List<Line> getNormalizedLines(double[] xcoords, double[] ycoords) {
    List<Line> lines = new ArrayList<>();
    Point previousPoint = null;
    for (int i = 0; i < xcoords.length; i++) {
      Point trimmedPoint = new Point(xcoords[i], ycoords[i]);
      if (previousPoint != null) {
        lines.add(new Line(previousPoint, trimmedPoint));
      }
      previousPoint = trimmedPoint;
    }
    return lines;
  }

  /**
   * A List of Lines which represent all possible lines on multiple screens size may vary.
   * 
   * @param xcoords an array of xCoordinates
   * @param ycoords an array of yCoordinates
   * @return a List of corresponding Lines on every possible screen
   */
  public List<Line> getAllNormalizedLines(double[] xcoords, double[] ycoords) {
    List<Line> centerLines = getNormalizedLines(xcoords, ycoords);
    List<Line> result = new ArrayList<>();
    for (Line line : centerLines) {
      List<Point[]> allPoints = getAllPoints(line.getP1(), line.getP2());
      for (Point[] points : allPoints) {
        result.add(new Line(points[0], points[1]));
      }
    }
    return result;
  }
}
