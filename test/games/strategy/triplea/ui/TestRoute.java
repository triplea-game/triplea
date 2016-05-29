package games.strategy.triplea.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Point;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.junit.Ignore;
import org.junit.Test;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.util.Tuple;

public class TestRoute {
  private Point[] dummyPoints = new Point[]{new Point(0,0), new Point(100,0), new Point(0,100)};
  private double[] dummyIndex = MapRouteDrawer.getIndex(dummyPoints);
  
  @Test
  public void testIndex(){
    assertArrayEquals(MapRouteDrawer.getIndex(new Point[]{}), new double[]{}, 0);
    assertEquals(dummyIndex.length, dummyPoints.length);
    //Not sure whether it makes sense to include a Test for specific values
    //The way the index is being calculated may change to a better System
    //Check the link for more information
    //http://stackoverflow.com/a/37370620/5769952
  }
  
  @Test
  @Ignore//Not working...
  public void testCurve(){//TODO multiply the value from coords array with a unknown factor in order to get the original value
    final double[] testYValues = new double[]{20, 40, 90};
    final PolynomialSplineFunction testFunction = new SplineInterpolator().interpolate(dummyIndex, testYValues);
    final double[] coords = MapRouteDrawer.getCoords(testFunction, dummyIndex);
    final double stepSize = testFunction.getKnots()[testFunction.getKnots().length - 1] / coords.length;
    assertEquals(testYValues[0], coords[(int)Math.round(dummyIndex[0]) + 1] * stepSize, 1);
    assertEquals(testYValues[1], coords[(int)Math.round(dummyIndex[1]) + 1] * stepSize, 1);
    assertEquals(testYValues[2], coords[(int)Math.round(dummyIndex[2]) + 1] * stepSize, 1);
  }
  
  @Test
  public void testPointSplitting(){
    double[] xCoords = new double[]{0, 100, 0};
    double[] yCoords = new double[]{0, 0, 100};
    Tuple<double[], double[]> dummyCoords = MapRouteDrawer.pointsToDoubleArrays(dummyPoints);
    assertArrayEquals(xCoords, dummyCoords.getFirst(), 0);
    assertArrayEquals(yCoords, dummyCoords.getSecond(), 0);
  }
  
  @Test
  public void testPointAcquisition(){
    Route dummyRoute = new Route();
    MapData dummyMapData = mock(MapData.class);
    when(dummyMapData.getCenter(any(Territory.class))).thenReturn(dummyPoints[1]);
    dummyRoute.add(mock(Territory.class));//This will e overridden with the startPoint, since it's the origin territory
    dummyRoute.add(mock(Territory.class));
    RouteDescription dummyRouteDescription = new RouteDescription(dummyRoute, dummyPoints[0], dummyPoints[2], null);
    assertArrayEquals(dummyPoints, MapRouteDrawer.getRoutePoints(dummyRouteDescription, dummyMapData));
  }
}
