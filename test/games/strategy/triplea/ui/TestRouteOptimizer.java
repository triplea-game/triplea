package games.strategy.triplea.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.triplea.ui.mapdata.MapData;

public class TestRouteOptimizer {

  private MapPanel mapPanel;
  private MapData mapData;

  @Before
  public void setUp() {
    mapPanel = mock(MapPanel.class);
    when(mapPanel.getImageWidth()).thenReturn(1000);
    when(mapPanel.getImageHeight()).thenReturn(1000);
    mapData = mock(MapData.class);
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

  private Point p(int x, int y) {
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
}
