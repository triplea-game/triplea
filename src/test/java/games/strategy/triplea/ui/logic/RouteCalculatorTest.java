package games.strategy.triplea.ui.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class RouteCalculatorTest {

  @Test
  public void testRouteTranslation() {
    final Point2D[] inputArray = new Point2D[] {point(1, 4), point(1001, 1001), point(600, 600)};
    assertArrayEquals(new Point2D[] {point(1, 4), point(1, 1), point(-400, -400)},
        new RouteCalculator(true, true, 1000, 1000).getTranslatedRoute(inputArray));
    assertArrayEquals(new Point2D[] {point(1, 4), point(1, 1001), point(-400, 600)},
        new RouteCalculator(true, false, 1000, 1000).getTranslatedRoute(inputArray));
    assertArrayEquals(new Point2D[] {point(1, 4), point(1001, 1), point(600, -400)},
        new RouteCalculator(false, true, 1000, 1000).getTranslatedRoute(inputArray));
    assertArrayEquals(inputArray, new RouteCalculator(false, false, 1000, 1000).getTranslatedRoute(inputArray));
  }

  private static Point2D point(final double x, final double y) {
    return new Point2D.Double(x, y);
  }

  @Test
  public void testClosestPoint() {
    final Point2D origin = new Point2D.Double();
    final Point2D closestPoint = new Point2D.Double(1, 1);
    final List<Point2D> pool = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      pool.add(point((int) (Math.random() * 1000 + 1), (int) (Math.random() * 1000 + 1)));
    }
    pool.add(closestPoint);
    assertEquals(closestPoint, RouteCalculator.getClosestPoint(origin, pool));
  }

  @Test
  public void testPossiblePoints() {
    final List<Point2D> possiblePoints = new ArrayList<>();
    // The values below must be all combinations of
    // x and y values 0, -mapWidth/height, +mapWidth/Height
    possiblePoints.add(point(-1000, -1000));
    possiblePoints.add(point(-1000, 0));
    possiblePoints.add(point(-1000, 1000));
    possiblePoints.add(point(0, -1000));
    possiblePoints.add(point(0, 0));
    possiblePoints.add(point(0, 1000));
    possiblePoints.add(point(1000, -1000));
    possiblePoints.add(point(1000, 0));
    possiblePoints.add(point(1000, 1000));
    checkPoints(0, possiblePoints, true, true);
    checkPoints(6, possiblePoints, true, false);
    checkPoints(6, possiblePoints, false, true);
    checkPoints(8, possiblePoints, false, false);
  }

  private static void checkPoints(final int offset, final List<Point2D> expected, final boolean isInfiniteX,
      final boolean isInfiniteY) {
    final List<Point2D> calculatedPoints =
        new RouteCalculator(isInfiniteX, isInfiniteY, 1000, 1000).getPossiblePoints(new Point2D.Double());
    assertEquals(expected.size(), calculatedPoints.size() + offset);
    for (final Point2D point : calculatedPoints) {
      assertTrue(expected.contains(point));
    }
  }

  @Test
  public void testMatrixTransposal() {
    final Point2D[] input = new Point2D[] {point(0, 0), point(1, 1)};
    final Point2D[] nw = new Point2D[] {point(-1000, -1000), point(-999, -999)};
    final Point2D[] n = new Point2D[] {point(0, -1000), point(1, -999)};
    final Point2D[] ne = new Point2D[] {point(1000, -1000), point(1001, -999)};
    final Point2D[] w = new Point2D[] {point(-1000, 0), point(-999, 1)};
    final Point2D[] e = new Point2D[] {point(1000, 0), point(1001, 1)};
    final Point2D[] sw = new Point2D[] {point(-1000, 1000), point(-999, 1001)};
    final Point2D[] s = new Point2D[] {point(0, 1000), point(1, 1001)};
    final Point2D[] se = new Point2D[] {point(1000, 1000), point(1001, 1001)};

    final List<Point2D[]> points = new RouteCalculator(true, true, 1000, 1000).getAllPoints(input);
    // This may be changed along with the RouteCalculator#getPossiblePoints method
    assertArrayEquals(input, points.get(0));
    assertArrayEquals(nw, points.get(1));
    assertArrayEquals(sw, points.get(2));
    assertArrayEquals(ne, points.get(3));
    assertArrayEquals(se, points.get(4));
    assertArrayEquals(w, points.get(5));
    assertArrayEquals(e, points.get(6));
    assertArrayEquals(n, points.get(7));
    assertArrayEquals(s, points.get(8));
  }

  @Test
  public void testGetAllNormalizedLines() {
    final RouteCalculator routeCalculator = new RouteCalculator(true, true, 1000, 1000);
    final double[] testData = new double[1000];
    Arrays.setAll(testData, Double::valueOf);
    final List<Path2D> paths = routeCalculator.getAllNormalizedLines(testData, testData);
    final Iterator<AffineTransform> transforms = routeCalculator.getPossibleTranslations().iterator();
    // This method looks more complicated than it actually is.
    // It checks whether all given points are contained in the returned paths
    // Unfortunately Path2D#contains does not work for whatever reason
    paths.forEach(path -> {
      try {
        final PathIterator iterator = path.getPathIterator(transforms.next().createInverse());
        Arrays.stream(testData).forEach(d -> {
          final int currentsegmentType = iterator.currentSegment(new double[] {d, d});
          assertTrue(currentsegmentType == PathIterator.SEG_LINETO || currentsegmentType == PathIterator.SEG_MOVETO,
              "(" + d + ", " + d + ") not contained");
          if (!iterator.isDone()) {
            iterator.next();
          }
        });
      } catch (final NoninvertibleTransformException e) {
        fail(e);
      }
    });
  }
}
