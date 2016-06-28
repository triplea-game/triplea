package games.strategy.triplea.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;

public class TestRoute {
  private MapRouteDrawer spyRouteDrawer = spy(new MapRouteDrawer());
  private Point[] dummyPoints = new Point[]{new Point(0,0), new Point(100,0), new Point(0,100)};
  private double[] dummyIndex = spyRouteDrawer.createParameterizedIndex(dummyPoints);
  private Route dummyRoute = spy(new Route());
  private MapData dummyMapData = mock(MapData.class);
  private RouteDescription dummyRouteDescription = spy(new RouteDescription(dummyRoute, dummyPoints[0], dummyPoints[2], null));
  private MapPanel dummyMapPanel = mock(MapPanel.class);
  
  @Before
  public void setUp(){
    dummyRoute.add(mock(Territory.class));//This will be overridden with the startPoint, since it's the origin territory
    dummyRoute.add(mock(Territory.class));
    when(dummyMapData.getCenter(any(Territory.class))).thenReturn(dummyPoints[1]);
    when(dummyMapPanel.getXOffset()).thenReturn(0);
    when(dummyMapPanel.getYOffset()).thenReturn(0);
  }
  
  @Test
  public void testIndex(){
    assertArrayEquals(spyRouteDrawer.createParameterizedIndex(new Point[]{}), new double[]{}, 0);
    assertEquals(dummyIndex.length, dummyPoints.length);
    //Not sure whether it makes sense to include a Test for specific values
    //The way the index is being calculated may change to a better System
    //Check the link for more information
    //http://stackoverflow.com/a/37370620/5769952
  }
  
  @Test
  public void testCurve(){
    final double[] testYValues = new double[]{20, 40, 90};
    final PolynomialSplineFunction testFunction = new SplineInterpolator().interpolate(dummyIndex, testYValues);
    final double[] coords = spyRouteDrawer.getCoords(testFunction, dummyIndex);
    final double stepSize = testFunction.getKnots()[testFunction.getKnots().length - 1] / coords.length;
    assertEquals(testYValues[0] * stepSize, coords[(int)Math.round(dummyIndex[0])], 1);
    assertEquals(testYValues[1] * stepSize, coords[(int)Math.round(dummyIndex[1])], 1);
    assertEquals(testYValues[2] * stepSize, coords[(int)Math.round(dummyIndex[2])], 1);
    //TODO change the calculation so that delta = 0;
  }
  
  @Test
  public void testPointSplitting(){
    double[] xCoords = new double[]{0, 100, 0};
    double[] yCoords = new double[]{0, 0, 100};
    assertArrayEquals(xCoords, spyRouteDrawer.getValues(dummyPoints, point -> point.getX()), 0);
    assertArrayEquals(yCoords, spyRouteDrawer.getValues(dummyPoints, point -> point.getY()), 0);
  }
  
  @Test
  public void testPointAcquisition(){
    assertArrayEquals(dummyPoints, spyRouteDrawer.getRoutePoints(dummyRouteDescription, dummyMapData, 0, 0, 0, 0));
  }
  
  @Test
  public void testCorrectParameterHandling(){
    //Should not throw any exception - should do nothing
    spyRouteDrawer.drawRoute(null, null, null, null, null);
    MapPanel mockedMapPanel = mock(MapPanel.class);
    when(mockedMapPanel.getXOffset()).thenReturn(0);
    when(mockedMapPanel.getYOffset()).thenReturn(0);
    when(mockedMapPanel.getScale()).thenReturn(0.0);
    Shape mockShape = mock(Shape.class);
    Graphics2D mockGraphics = mock(Graphics2D.class);
    when(mockShape.contains(any(Point2D.class))).thenReturn(true);
    when(mockGraphics.getClip()).thenReturn(mockShape);
    spyRouteDrawer.drawRoute(mockGraphics, dummyRouteDescription, mockedMapPanel, dummyMapData, "2");
    verify(mockGraphics, atLeastOnce()).draw(any(Line2D.class));
    verify(mockedMapPanel).getXOffset();//Those methods are needed
    verify(mockedMapPanel).getYOffset();
    verify(mockedMapPanel).getScale();
    
    verify(dummyRouteDescription, times(2)).getRoute();
    verify(dummyRouteDescription.getRoute(), atLeastOnce()).getAllTerritories();
  }
}
