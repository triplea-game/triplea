package games.strategy.triplea.ui.panels.map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.image.ResourceImageFactory;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.Arrays;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

final class MapRouteDrawerTest {
  private final Point[] dummyPoints =
      new Point[] {new Point(0, 0), new Point(100, 0), new Point(0, 100)};
  private final MapData dummyMapData = mock(MapData.class);
  private final MapRouteDrawer spyRouteDrawer =
      spy(new MapRouteDrawer(mock(MapPanel.class), dummyMapData));
  private final double[] dummyIndex = spyRouteDrawer.newParameterizedIndex(dummyPoints);
  private final Route dummyRoute = spy(new Route(mock(Territory.class), mock(Territory.class)));
  private final RouteDescription dummyRouteDescription =
      spy(new RouteDescription(dummyRoute, dummyPoints[0], dummyPoints[2], null));

  @BeforeEach
  void setUp() {
    when(dummyMapData.getCenter(any(Territory.class))).thenReturn(dummyPoints[1]);
    when(dummyMapData.getMapDimensions()).thenReturn(new Dimension(1000, 1000));
  }

  @Test
  void testIndex() {
    assertArrayEquals(spyRouteDrawer.newParameterizedIndex(new Point2D[] {}), new double[] {});
    assertEquals(dummyIndex.length, dummyPoints.length);
    // Not sure whether it makes sense to include a Test for specific values
    // The way the index is being calculated may change to a better System
    // Check the link for more information
    // http://stackoverflow.com/a/37370620/5769952
  }

  @Test
  void testCurve() {
    final double[] testYValues = new double[] {20, 40, 90};
    final PolynomialSplineFunction testFunction =
        new SplineInterpolator().interpolate(dummyIndex, testYValues);
    final double[] coords = spyRouteDrawer.getCoords(testFunction, dummyIndex);
    final double stepSize =
        testFunction.getKnots()[testFunction.getKnots().length - 1] / coords.length;
    assertEquals(testYValues[0] * stepSize, coords[(int) Math.round(dummyIndex[0])], 1);
    assertEquals(testYValues[1] * stepSize, coords[(int) Math.round(dummyIndex[1])], 1);
    assertEquals(testYValues[2] * stepSize, coords[(int) Math.round(dummyIndex[2])], 1);
    // TODO change the calculation so that delta = 0;
  }

  @Test
  void testPointSplitting() {
    final double[] expectedXCoords = new double[] {0, 100, 0};
    final double[] expectedYCoords = new double[] {0, 0, 100};
    assertArrayEquals(expectedXCoords, spyRouteDrawer.getValues(dummyPoints, Point2D::getX));
    assertArrayEquals(expectedYCoords, spyRouteDrawer.getValues(dummyPoints, Point2D::getY));
  }

  @Test
  void testCorrectParameterHandling() {
    final MapPanel mockedMapPanel = mock(MapPanel.class);
    final MapRouteDrawer routeDrawer = spy(new MapRouteDrawer(mockedMapPanel, dummyMapData));
    when(mockedMapPanel.getXOffset()).thenReturn(0);
    when(mockedMapPanel.getYOffset()).thenReturn(0);
    when(mockedMapPanel.getScale()).thenReturn(0.0);
    when(mockedMapPanel.getImageWidth()).thenReturn(1);
    when(mockedMapPanel.getImageHeight()).thenReturn(1);
    final Shape mockShape = mock(Shape.class);
    final Graphics2D mockGraphics = mock(Graphics2D.class);
    when(mockShape.contains(any(Point2D.class))).thenReturn(true);
    final ResourceCollection mockResourceCollection = mock(ResourceCollection.class);
    when(mockResourceCollection.getResourcesCopy()).thenReturn(new IntegerMap<>());
    final ResourceImageFactory mockResourceImageFactory = mock(ResourceImageFactory.class);
    routeDrawer.drawRoute(
        mockGraphics, dummyRouteDescription, "2", mockResourceCollection, mockResourceImageFactory);
    verify(mockGraphics, atLeastOnce()).fill(any(Shape.class));
    verify(mockGraphics, atLeastOnce()).draw(any(Shape.class));
    verify(mockedMapPanel, atLeastOnce()).getXOffset(); // Those methods are needed
    verify(mockedMapPanel, atLeastOnce()).getYOffset();
    verify(mockedMapPanel, atLeastOnce()).getScale();

    verify(dummyRouteDescription, times(2)).getRoute();
    verify(dummyRouteDescription.getRoute(), atLeastOnce()).getAllTerritories();
  }

  /** Regression test for https://github.com/triplea-game/triplea/issues/7112 */
  @Test
  void verifySequenceIsTrulyMonotonic() {
    final MapRouteDrawer routeDrawer = new MapRouteDrawer(mock(MapPanel.class), dummyMapData);
    final double[] index =
        routeDrawer.newParameterizedIndex(
            new Point2D[] {new Double(0, 0), new Double(0, 0), new Double(0, 0)});

    assertThat(Arrays.stream(index).boxed().toArray(), is(arrayWithSize(3)));
    assertThat(index[0], is(lessThan(index[1])));
    assertThat(index[1], is(lessThan(index[2])));
  }
}
