package games.strategy.triplea.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import games.strategy.triplea.ui.mapdata.MapData;

@RunWith(MockitoJUnitRunner.class)
public class TestRouteOptimizer {

  @Mock
  private MapPanel mapPanel;
  @Mock
  private MapData mapData;

  @Before
  public void setUp() {
    when(mapPanel.getImageWidth()).thenReturn(1000);
    when(mapPanel.getImageHeight()).thenReturn(1000);
    when(mapData.scrollWrapX()).thenReturn(true, true, false, false);
    when(mapData.scrollWrapY()).thenReturn(true, false, true, false);
  }

  @Test
  public void testRouteTranslation() {
    Point[] inputArray = new Point[] {p(1, 4), p(1001, 1001), p(600, 600)};
    assertArrayEquals(new Point[] {p(1, 4), p(1, 1), p(-400, -400)},
        new RouteOptimizer(mapData, mapPanel).getTranslatedRoute(inputArray));
    assertArrayEquals(new Point[] {p(1, 4), p(1, 1001), p(-400, 600)},
        new RouteOptimizer(mapData, mapPanel).getTranslatedRoute(inputArray));
    assertArrayEquals(new Point[] {p(1, 4), p(1001, 1), p(600, -400)},
        new RouteOptimizer(mapData, mapPanel).getTranslatedRoute(inputArray));
    assertArrayEquals(inputArray, new RouteOptimizer(mapData, mapPanel).getTranslatedRoute(inputArray));
  }

  private static Point p(int x, int y) {
    return new Point(x, y);
  }

  @Test
  public void testClosestPoint() {
    Point origin = new Point();
    Point closestPoint = new Point(1, 1);
    List<Point2D> pool = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      pool.add(p((int) (Math.random() * 1000 + 1), (int) (Math.random() * 1000 + 1)));
    }
    pool.add(closestPoint);
    assertEquals(closestPoint, RouteOptimizer.getClosestPoint(origin, pool));
  }

  @Test
  public void testPossiblePoints() {
    List<Point2D> possiblePoints = new ArrayList<>();
    // The values below must be all combinations of
    // x and y values 0, -mapWidth/height, +mapWidth/Height
    possiblePoints.add(p(-1000, -1000));
    possiblePoints.add(p(-1000, 0));
    possiblePoints.add(p(-1000, 1000));
    possiblePoints.add(p(0, -1000));
    possiblePoints.add(p(0, 0));
    possiblePoints.add(p(0, 1000));
    possiblePoints.add(p(1000, -1000));
    possiblePoints.add(p(1000, 0));
    possiblePoints.add(p(1000, 1000));
    checkPoints(0, possiblePoints);
    checkPoints(6, possiblePoints);
    checkPoints(6, possiblePoints);
    checkPoints(8, possiblePoints);
  }

  private void checkPoints(int offset, List<Point2D> expected) {
    List<Point2D> calculatedPoints = new RouteOptimizer(mapData, mapPanel).getPossiblePoints(new Point());
    assertEquals(expected.size(), calculatedPoints.size() + offset);
    for (Point2D point : calculatedPoints) {
      assertTrue(expected.contains(point));
    }
  }

  @Test
  public void testMatrixTransposal() {
    Point[] input = new Point[] {p(0, 0), p(1, 1)};
    Point[] nw = new Point[] {p(-1000, -1000), p(-999, -999)};
    Point[] n = new Point[] {p(0, -1000), p(1, -999)};
    Point[] ne = new Point[] {p(1000, -1000), p(1001, -999)};
    Point[] w = new Point[] {p(-1000, 0), p(-999, 1)};
    Point[] e = new Point[] {p(1000, 0), p(1001, 1)};
    Point[] sw = new Point[] {p(-1000, 1000), p(-999, 1001)};
    Point[] s = new Point[] {p(0, 1000), p(1, 1001)};
    Point[] se = new Point[] {p(1000, 1000), p(1001, 1001)};

    List<Point[]> points = new RouteOptimizer(mapData, mapPanel).getAllPoints(input);
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
