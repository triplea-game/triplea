package games.strategy.triplea.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.awt.Point;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.junit.Ignore;
import org.junit.Test;

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
  @Ignore
  public void testCurve(){//TODO multiply the value from coords array with a unknown factor in order to get the original value
    final double[] testYValues = new double[]{20, 40, 90};
    final PolynomialSplineFunction testFunction = new SplineInterpolator().interpolate(dummyIndex, testYValues);
    final double[] coords = MapRouteDrawer.getCoords(testFunction, dummyIndex);
    assertEquals(testYValues[0], coords[(int)Math.round(dummyIndex[0])], 0);
    assertEquals(testYValues[1], coords[(int)Math.round(dummyIndex[1])], 0);
    assertEquals(testYValues[2], coords[(int)Math.round(dummyIndex[2])], 0);
  }
  
  @Test
  @Ignore
  public void testPointSplitting(){
//    MapRouteDrawer.pointsToDoubleArrays(points);
  }
  
  @Test
  @Ignore
  public void testPointAcquisition(){
//    MapRouteDrawer.getRoutePoints(routeDescription, mapData);
  }
}
