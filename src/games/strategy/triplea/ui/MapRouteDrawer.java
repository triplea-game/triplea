package games.strategy.triplea.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.util.Tuple;

/**
 * Draws a route on a map.
 */
public class MapRouteDrawer {

  private static final SplineInterpolator splineInterpolator = new SplineInterpolator();

  /**
   * Draws the route to the screen, does nothing if null.
   */
  public static void drawRoute(final Graphics2D graphics, final RouteDescription routeDescription, final MapPanel view, final MapData mapData, final String movementLeftForCurrentUnits) {
    if (routeDescription == null) {
      return;
    }
    final Route route = routeDescription.getRoute();
    if (route == null) {
      return;
    }
    
    final Point[] points = getRoutePoints(routeDescription, mapData);
    final int xOffset = view.getXOffset();
    final int yOffset = view.getYOffset();
    final int jointsize = 10;
    final int numTerritories = route.getAllTerritories().size();
    //set thickness and color of the future drawings
    graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    graphics.setPaint(Color.red);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    if(Arrays.asList(points).contains(null)){//If the Array is null at some point
      return;
    }

    final GeneralPath path = getSmoothPath(points);
    path.transform(new AffineTransform(1, 0, 0, 1, -xOffset, -yOffset));
    graphics.draw(path);
    if(numTerritories <= 1 || points.length <= 2){
      graphics.fillOval((routeDescription.getEnd().x - xOffset) - jointsize / 2, (routeDescription.getEnd().y - yOffset) - jointsize / 2, jointsize, jointsize);
    }
    else{
      drawMoveLength(graphics, routeDescription, points, xOffset, yOffset, view, mapData.scrollWrapX(), mapData.scrollWrapY(), numTerritories, movementLeftForCurrentUnits);
    }
    drawJoints(graphics, points, xOffset, yOffset, jointsize);
    final Image cursorImage = routeDescription.getCursorImage();
    if(cursorImage != null){
      graphics.drawImage(cursorImage, (routeDescription.getEnd().x - xOffset) - (cursorImage.getWidth(null) / 2), (routeDescription.getEnd().y - yOffset) - (cursorImage.getHeight(null) / 2), null);
    }
  }

  private static void drawJoints(Graphics2D graphics, Point[] points, int xOffset, int yOffset, int jointsize) {
    for(Point p : points){
      graphics.fillOval((p.x - xOffset) - jointsize / 2, (p.y - yOffset) - jointsize / 2, jointsize, jointsize);
    }
    
  }

  private static double[] getIndex(Point[] points) {
    final double[] index = new double[points.length];
    index[0] = 0;
    for (int i = 1; i < points.length; i++) {
      index[i] = index[i - 1] + points[i].distance(points[i - 1]);
    }
    return index;
  }
  
  private static Point[] getRoutePoints(RouteDescription routeDescription, MapData mapData){
    final List<Territory> territories = routeDescription.getRoute().getAllTerritories();
    final int numTerritories = territories.size();
    final Point[] points = new Point[numTerritories];
    for (int i = 0; i < numTerritories; i++) {
      points[i] = mapData.getCenter(territories.get(i));
    }
    if (routeDescription.getStart() != null) {
      points[0] = routeDescription.getStart();
    }
    if (routeDescription.getEnd() != null && numTerritories > 1) {
      points[numTerritories - 1] = new Point(routeDescription.getEnd());
    }
    return points;
  }
  
  private static void drawMoveLength(Graphics2D graphics, RouteDescription routeDescription, Point[] points, int xOffset, int yOffset, MapPanel view, boolean scrollWrapX, boolean scrollWrapY, int numTerritories, String movementLeftForCurrentUnits){
      final Point cursorPos = points[points.length - 1];
      final int xDir = cursorPos.x - points[numTerritories - 2].x;
      final int textXOffset = xDir > 0 ? 6 : (xDir == 0 ? 0 : -14);
      
      final int yDir = cursorPos.y - points[numTerritories - 2].y;
      final int textYOffset = yDir > 0 ? 18 : -24;
      
      final String unitMovementLeft = movementLeftForCurrentUnits == null || movementLeftForCurrentUnits.trim().length() == 0 ? "" : "    /" + movementLeftForCurrentUnits;
      final BufferedImage movementImage = new BufferedImage(72, 24, BufferedImage.TYPE_INT_ARGB);
      setupTextImage(movementImage.createGraphics(), String.valueOf(numTerritories - 1), unitMovementLeft);
      graphics.drawImage(movementImage, cursorPos.x + textXOffset - xOffset, cursorPos.y + textYOffset - yOffset, null);
      
      final int translateX = view.getImageWidth();
      final int translateY = view.getImageHeight();
      //This sets the corner in which the text will be drawn...
      if (scrollWrapX && !scrollWrapY){
        graphics.drawImage(movementImage, cursorPos.x + textXOffset - xOffset - translateX, cursorPos.y + textYOffset - yOffset, null);
        graphics.drawImage(movementImage, cursorPos.x + textXOffset - xOffset + translateX, cursorPos.y + textYOffset - yOffset, null);
      }
      if (scrollWrapY && !scrollWrapX){
        graphics.drawImage(movementImage, cursorPos.x + textXOffset - xOffset, cursorPos.y + textYOffset - yOffset + translateY, null);
        graphics.drawImage(movementImage, cursorPos.x + textXOffset - xOffset, cursorPos.y + textYOffset - yOffset - translateY, null);
      }
      if (scrollWrapX && scrollWrapY){
        graphics.drawImage(movementImage, cursorPos.x + textXOffset - xOffset - translateX, cursorPos.y + textYOffset - yOffset + translateY, null);
        graphics.drawImage(movementImage, cursorPos.x + textXOffset - xOffset + translateX, cursorPos.y + textYOffset - yOffset - translateY, null);
      }
  }

  private static final double minStepSizeCurveXValue = 5f;
  private static final int maxPointsNeededForSmoothing = 3;

  /**
   * Generates a smooth path which includes the original points.
   *
   * @param points - points array
   * @return smooth path through provided points.
   */
  private static GeneralPath getSmoothPath(final Point[] points) {
    final GeneralPath result = new GeneralPath();
    if (points.length > 1) {
      result.moveTo(points[0].x, points[0].y);
    }
    if (points.length == 2) {
      result.lineTo(points[1].x, points[1].y);
    } else if (points.length > 2) {
      addSmoothSegmentsToPath(result, points);
    }
    return result;
  }

  /**
   * Interpolates x and y values of the points-path and adds smooth segments from point to point to the path using the
   * interpolation result.
   *
   * @param path - path to be enhanced by smooth segments
   * @param points - original points
   */
  private static void addSmoothSegmentsToPath(final GeneralPath path, final Point[] points) {
    // Interpolates x and y values in separate curves
    final double[] curvesXValues = getIndex(points);
    final Tuple<PolynomialSplineFunction, PolynomialSplineFunction> curves =
        buildSplineFunctions(points, curvesXValues);
    final PolynomialSplineFunction xCurve = curves.getFirst();
    final PolynomialSplineFunction yCurve = curves.getSecond();
    // For each given point (to which one specific x value in curveXValues belongs)
    // use spline functions to get additional points for building the smooth path.
    // But no more needed than maxPointsNeededForDrawing.
    for (int curvesXValueIndex = 1; curvesXValueIndex < curvesXValues.length; curvesXValueIndex++) {
      // Get even currentStepSize between current and previous curvesXValues
      final double diffCurvesXValues = curvesXValues[curvesXValueIndex] - curvesXValues[curvesXValueIndex - 1];
      final int stepsUntilNextCurveXValue =
          Math.min(maxPointsNeededForSmoothing, (int) Math.ceil((diffCurvesXValues) / minStepSizeCurveXValue));
      final double currentStepSize = diffCurvesXValues / stepsUntilNextCurveXValue;
      // Collect interpolated values from xCurve and yCurve after each step
      final double xCurveValues[] = new double[stepsUntilNextCurveXValue];
      final double yCurveValues[] = new double[stepsUntilNextCurveXValue];
      // intermediate points from curve
      int curvesValueIndex = 0;
      for (double curvesXValue = curvesXValues[curvesXValueIndex - 1]; curvesValueIndex < stepsUntilNextCurveXValue
          - 1; ++curvesValueIndex) {
        curvesXValue += currentStepSize;
        xCurveValues[curvesValueIndex] = xCurve.value(curvesXValue);
        yCurveValues[curvesValueIndex] = yCurve.value(curvesXValue);
      }
      // next given point
      xCurveValues[curvesValueIndex] = points[curvesXValueIndex].x;
      yCurveValues[curvesValueIndex] = points[curvesXValueIndex].y;
      addSegmentToPath(path, xCurveValues, yCurveValues);
    }
  }

  /**
   * Interpolates a 2d path represented by its points and their distances/weights to their previous point.
   * 
   * @param points - path points
   * @param curveXValues - distances/weights between points and their previous point
   * @return Tuple of PolynomialSplineFunction, first for xCurve, second for yCurve
   */
  private static Tuple<PolynomialSplineFunction, PolynomialSplineFunction> buildSplineFunctions(final Point[] points,
      final double[] curveXValues) {
    final double[] xCurveYValues = new double[points.length];
    final double[] yCurveYValues = new double[points.length];
    for (int i = 0; i < points.length; i++) {
      xCurveYValues[i] = points[i].getX();
      yCurveYValues[i] = points[i].getY();
    }
    return Tuple.of(splineInterpolator.interpolate(curveXValues, xCurveYValues),
        splineInterpolator.interpolate(curveXValues, yCurveYValues));
  }

  /**
   * Adds a segment to the path from the current point to the point
   * ( xCurveValues[xCurveValues.length - 1], yCurveValues[yCurveValues.length - 1])
   * and uses array points in between for smoothing if possible.
   * 
   * @param path - path to add segment to
   * @param xCurveValues - array of xCurveValues
   * @param yCurveValues - array of yCurveValues
   */
  private static void addSegmentToPath(final GeneralPath path, final double[] xCurveValues,
      final double[] yCurveValues) {
    switch (xCurveValues.length) {
      case 3:
        path.curveTo(xCurveValues[0], yCurveValues[0], xCurveValues[1], yCurveValues[1], xCurveValues[2],
            yCurveValues[2]);
        break;
      case 2:
        path.quadTo(xCurveValues[0], yCurveValues[0], xCurveValues[1], yCurveValues[1]);
        break;
      case 1:
        path.lineTo(xCurveValues[0], yCurveValues[0]);
        break;
    }
  }
  
  private static void setupTextImage(Graphics2D textG2D, String textRouteMovement, String unitMovementLeft){
    textG2D.setColor(Color.YELLOW);
    textG2D.setFont(new Font("Dialog", Font.BOLD, 20));
    textG2D.drawString(textRouteMovement, 0, 20);
    textG2D.setColor(new Color(33, 0, 127));
    textG2D.setFont(new Font("Dialog", Font.BOLD, 16));
    textG2D.drawString(unitMovementLeft, 0, 20);
  }
  
}
