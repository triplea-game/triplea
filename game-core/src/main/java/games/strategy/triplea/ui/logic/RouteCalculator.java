package games.strategy.triplea.ui.logic;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Builder;

/** Utility class to calculate a smooth route to draw on the map. */
@Builder
public final class RouteCalculator {
  private final boolean isInfiniteX;
  private final boolean isInfiniteY;
  private final int mapWidth;
  private final int mapHeight;

  /**
   * Algorithm for finding the shortest path for the given Route.
   *
   * @param route The joints on the Map
   * @return A Point array which goes through Map Borders if necessary
   */
  public Point2D[] getTranslatedRoute(final Point2D... route) {
    checkNotNull(route);
    if (!isInfiniteX && !isInfiniteY) {
      return route;
    }
    final Point2D[] result = new Point2D[route.length];
    for (int i = 0; i < route.length; i++) {
      final Point2D point = route[i];
      result[i] = i == 0 ? point : getClosestPoint(result[i - 1], getPossiblePoints(point));
    }
    return result;
  }

  /**
   * Returns the Closest Point out of the given Pool.
   *
   * @param source the reference Point
   * @param pool Point List with all possible options
   * @return the closest point in the Pool to the source or null if the pool is empty
   */
  @Nullable
  public static Point2D getClosestPoint(final Point2D source, final List<Point2D> pool) {
    return pool.stream().min(Comparator.comparingDouble(source::distance)).orElse(null);
  }

  /**
   * Method for getting Points, which are a mapHeight/Width away from the actual Point Used to
   * display routes with higher offsets than the map width/height.
   *
   * @param point The Point to "clone"
   * @return A List of all possible Points depending in map Properties. Size may vary.
   */
  public List<Point2D> getPossiblePoints(final Point2D point) {
    return MapScrollUtil.getPossibleTranslations(isInfiniteX, isInfiniteY, mapWidth, mapHeight)
        .stream()
        .map(t -> t.transform(point, null))
        .collect(Collectors.toList());
  }

  /**
   * Matrix Transpose method to transpose the 2dimensional point list.
   *
   * @param points A Point array
   * @return Offset Point Arrays including points
   */
  public List<Point2D[]> getAllPoints(final Point2D... points) {
    checkNotNull(points);
    final List<Point2D[]> allPoints = new ArrayList<>();
    for (int i = 0; i < points.length; i++) {
      final List<Point2D> subPoints = getPossiblePoints(points[i]);
      for (int y = 0; y < subPoints.size(); y++) {
        if (i == 0) {
          allPoints.add(new Point2D[points.length]);
        }
        allPoints.get(y)[i] = subPoints.get(y);
      }
    }
    return allPoints;
  }

  /**
   * Generates a Path which represent "normalized forms" of the given arrays.
   *
   * @param xcoords an array of xCoordinates
   * @param ycoords an array of yCoordinates
   * @return a Path representing the Route to be drawn
   */
  private static Path2D getNormalizedLines(final double[] xcoords, final double[] ycoords) {
    checkNotNull(xcoords);
    checkNotNull(ycoords);
    checkArgument(xcoords.length > 0, "X-Coordinates must at least contain a single element.");
    checkArgument(ycoords.length > 0, "Y-Coordinates must at least contain a single element.");
    final Path2D path = new Path2D.Double();
    path.moveTo(xcoords[0], ycoords[0]);
    for (int i = 1; i < xcoords.length; i++) {
      path.lineTo(xcoords[i], ycoords[i]);
    }
    return path;
  }

  /**
   * A List of Lines which represent all possible lines on multiple screens size may vary.
   *
   * @param xcoords an array of xCoordinates
   * @param ycoords an array of yCoordinates
   * @return a List of corresponding Lines on every possible screen
   */
  public List<Path2D> getAllNormalizedLines(final double[] xcoords, final double[] ycoords) {
    final Path2D path = getNormalizedLines(xcoords, ycoords);
    return MapScrollUtil.getPossibleTranslations(isInfiniteX, isInfiniteY, mapWidth, mapHeight)
        .stream()
        .map(t -> new Path2D.Double(path, t))
        .collect(Collectors.toList());
  }
}
