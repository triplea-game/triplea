package games.strategy.triplea.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.logic.Point;
import games.strategy.triplea.ui.mapdata.MapData;

public class RouteTest {
  private final Point[] dummyPoints = new Point[] {new Point(0, 0), new Point(100, 0), new Point(0, 100)};
  private final MapData dummyMapData = mock(MapData.class);
  private final MapRouteDrawer spyRouteDrawer = spy(new MapRouteDrawer(mock(MapPanel.class), dummyMapData));
  private final double[] dummyIndex = spyRouteDrawer.createParameterizedIndex(dummyPoints);
  private final Route dummyRoute = spy(new Route());
  private final RouteDescription dummyRouteDescription =
      spy(new RouteDescription(dummyRoute, dummyPoints[0].toPoint(), dummyPoints[2].toPoint(), null));

  @Before
  public void setUp() {
    dummyRoute.add(mock(Territory.class));// This will be overridden with the startPoint, since it's the origin
                                          // territory
    dummyRoute.add(mock(Territory.class));
    when(dummyMapData.getCenter(any(Territory.class))).thenReturn(dummyPoints[1].toPoint());
    when(dummyMapData.getMapDimensions()).thenReturn(new Dimension(1000, 1000));
  }

  @Test
  public void testIndex() {
    assertArrayEquals(spyRouteDrawer.createParameterizedIndex(new Point[] {}), new double[] {}, 0);
    assertEquals(dummyIndex.length, dummyPoints.length);
    // Not sure whether it makes sense to include a Test for specific values
    // The way the index is being calculated may change to a better System
    // Check the link for more information
    // http://stackoverflow.com/a/37370620/5769952
  }

  @Test
  public void testCurve() {
    final double[] testYValues = new double[] {20, 40, 90};
    final PolynomialSplineFunction testFunction = new SplineInterpolator().interpolate(dummyIndex, testYValues);
    final double[] coords = spyRouteDrawer.getCoords(testFunction, dummyIndex);
    final double stepSize = testFunction.getKnots()[testFunction.getKnots().length - 1] / coords.length;
    assertEquals(testYValues[0] * stepSize, coords[(int) Math.round(dummyIndex[0])], 1);
    assertEquals(testYValues[1] * stepSize, coords[(int) Math.round(dummyIndex[1])], 1);
    assertEquals(testYValues[2] * stepSize, coords[(int) Math.round(dummyIndex[2])], 1);
    // TODO change the calculation so that delta = 0;
  }

  @Test
  public void testPointSplitting() {
    final double[] xCoords = new double[] {0, 100, 0};
    final double[] yCoords = new double[] {0, 0, 100};
    assertArrayEquals(xCoords, spyRouteDrawer.getValues(dummyPoints, point -> point.getX()), 0);
    assertArrayEquals(yCoords, spyRouteDrawer.getValues(dummyPoints, point -> point.getY()), 0);
  }


  @Test
  public void testCorrectParameterHandling() {
    final MapPanel mockedMapPanel = mock(MapPanel.class);
    MapRouteDrawer routeDrawer = spy(new MapRouteDrawer(mockedMapPanel, dummyMapData));
    when(mockedMapPanel.getXOffset()).thenReturn(0);
    when(mockedMapPanel.getYOffset()).thenReturn(0);
    when(mockedMapPanel.getScale()).thenReturn(0.0);
    when(mockedMapPanel.getImageWidth()).thenReturn(1);
    when(mockedMapPanel.getImageHeight()).thenReturn(1);
    final Shape mockShape = mock(Shape.class);
    final Graphics2D mockGraphics = mock(Graphics2D.class);
    when(mockShape.contains(any(Point2D.class))).thenReturn(true);
    when(mockGraphics.getClip()).thenReturn(mockShape);
    routeDrawer.drawRoute(mockGraphics, dummyRouteDescription, "2");
    verify(mockGraphics, atLeastOnce()).draw(any(Line2D.class));
    verify(mockedMapPanel).getXOffset();// Those methods are needed
    verify(mockedMapPanel).getYOffset();
    verify(mockedMapPanel).getScale();

    verify(dummyRouteDescription, times(2)).getRoute();
    verify(dummyRouteDescription.getRoute(), atLeastOnce()).getAllTerritories();
  }
}
