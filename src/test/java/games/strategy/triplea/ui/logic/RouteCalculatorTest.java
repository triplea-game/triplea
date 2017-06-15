package games.strategy.triplea.ui.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RouteCalculatorTest {

  @Test
  public void testRouteTranslation() {
    final Point[] inputArray = new Point[] {point(1, 4), point(1001, 1001), point(600, 600)};
    assertArrayEquals(new Point[] {point(1, 4), point(1, 1), point(-400, -400)},
        new RouteCalculator(true, true, 1000, 1000).getTranslatedRoute(inputArray));
    assertArrayEquals(new Point[] {point(1, 4), point(1, 1001), point(-400, 600)},
        new RouteCalculator(true, false, 1000, 1000).getTranslatedRoute(inputArray));
    assertArrayEquals(new Point[] {point(1, 4), point(1001, 1), point(600, -400)},
        new RouteCalculator(false, true, 1000, 1000).getTranslatedRoute(inputArray));
    assertArrayEquals(inputArray, new RouteCalculator(false, false, 1000, 1000).getTranslatedRoute(inputArray));
  }

  private static Point point(final double x, final double y) {
    return new Point(x, y);
  }

  @Test
  public void testClosestPoint() {
    final Point origin = new Point();
    final Point closestPoint = new Point(1, 1);
    final List<Point> pool = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      pool.add(point((int) (Math.random() * 1000 + 1), (int) (Math.random() * 1000 + 1)));
    }
    pool.add(closestPoint);
    assertEquals(closestPoint, RouteCalculator.getClosestPoint(origin, pool));
  }

  @Test
  public void testPossiblePoints() {
    final List<Point> possiblePoints = new ArrayList<>();
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

  private static void checkPoints(final int offset, final List<Point> expected, final boolean isInfiniteX,
      final boolean isInfiniteY) {
    final List<Point> calculatedPoints =
        new RouteCalculator(isInfiniteX, isInfiniteY, 1000, 1000).getPossiblePoints(new Point());
    assertEquals(expected.size(), calculatedPoints.size() + offset);
    for (final Point point : calculatedPoints) {
      assertTrue(expected.contains(point));
    }
  }

  @Test
  public void testMatrixTransposal() {
    final Point[] input = new Point[] {point(0, 0), point(1, 1)};
    final Point[] nw = new Point[] {point(-1000, -1000), point(-999, -999)};
    final Point[] n = new Point[] {point(0, -1000), point(1, -999)};
    final Point[] ne = new Point[] {point(1000, -1000), point(1001, -999)};
    final Point[] w = new Point[] {point(-1000, 0), point(-999, 1)};
    final Point[] e = new Point[] {point(1000, 0), point(1001, 1)};
    final Point[] sw = new Point[] {point(-1000, 1000), point(-999, 1001)};
    final Point[] s = new Point[] {point(0, 1000), point(1, 1001)};
    final Point[] se = new Point[] {point(1000, 1000), point(1001, 1001)};

    final List<Point[]> points = new RouteCalculator(true, true, 1000, 1000).getAllPoints(input);
    // This may be changed along with the RouteOptimizer#getPossiblePoints method
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
}
