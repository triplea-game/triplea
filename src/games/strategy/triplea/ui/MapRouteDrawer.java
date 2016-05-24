package games.strategy.triplea.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;

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
    
    if(numTerritories <= 1 || points.length <= 2){
      drawLineWithTranslate(graphics, new Line2D.Float(routeDescription.getStart(), routeDescription.getEnd()), xOffset, yOffset);
      graphics.fillOval((routeDescription.getEnd().x - xOffset) - jointsize / 2, (routeDescription.getEnd().y - yOffset) - jointsize / 2, jointsize, jointsize);
    }
    else{
      drawCurvedPath(graphics, points, view);
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
    for(int i = 0; i < points.length; i++){
      index[i] = i;
    }
    return index;
  }

  @SuppressWarnings("unused")//Could be useful
  private static double sqrtDistance(Point point1, Point point2) {
    return Math.sqrt(getDistance(point1, point2));
  }
  
  private static double getDistance(Point point1, Point point2){
    //double c = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2)); Pythagoras' theorem
    return Math.sqrt(Math.pow(Math.abs(point2.getX() - point1.getX()), 2) + Math.pow(Math.abs(point2.getY() - point1.getY()), 2));
  }

  private static void drawLineWithTranslate(Graphics2D graphics, Line2D line2D, double translateX, double translateY) {
      final Line2D line = (Line2D) line2D;
      final Point2D point1 = new Point2D.Double(line.getP1().getX() - translateX, line.getP1().getY() - translateY);
      final Point2D point2 = new Point2D.Double(line.getP2().getX() - translateX, line.getP2().getY() - translateY);
      graphics.draw(new Line2D.Double(point1, point2));
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
  
  private static double[] pointsXToDoubleArray(Point[] points){
    double[] result = new double[points.length];
    for(int i = 0; i < points.length; i++){
      result[i] = points[i].getX();
    }
    return result;
  }
  private static double[] pointsYToDoubleArray(Point[] points){
    double[] result = new double[points.length];
    for(int i = 0; i < points.length; i++){
      result[i] = points[i].getY();
    }
    return result;
  }
  
  private static double[] getCoords(PolynomialSplineFunction curve, float stepSize){
    final double[] coords = new double[(int) (curve.getN() / stepSize)];
    for(int i = 0; i < curve.getN() / stepSize; i++){
      coords[i] = curve.value(i * stepSize);
    }
    return coords;
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
  
  private static void drawCurvedPath(Graphics2D graphics, Point[] points, MapPanel view){
    final double[] index = getIndex(points);
    final float stepSize = 0.01f;//TODO calculating a step size that makes sense
    final PolynomialSplineFunction xcurve = splineInterpolator.interpolate(index, pointsXToDoubleArray(points));
    final PolynomialSplineFunction ycurve = splineInterpolator.interpolate(index, pointsYToDoubleArray(points));
    final double[] xcoords = getCoords(xcurve, stepSize);
    final double[] ycoords = getCoords(ycurve, stepSize);
    
    for(int i = 1; i < xcoords.length; i++){
      //TODO maybe a line is not the best way to draw this...
      drawLineWithTranslate(graphics, new Line2D.Double(xcoords[i-1], ycoords[i-1], xcoords[i], ycoords[i]), view.getXOffset(), view.getYOffset());
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
