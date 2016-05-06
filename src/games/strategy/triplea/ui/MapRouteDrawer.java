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

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import games.strategy.engine.data.Route;

/**
 * Draws a route on a map.
 */
public class MapRouteDrawer {

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
    graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    graphics.setPaint(Color.red);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    final Point[] points = getPoints(routeDescription, mapData);
    final int xOffset = view.getXOffset();
    final int yOffset = view.getYOffset();
    final int jointsize = 10;
    
    if(route.getAllTerritories().size() <= 1){
      drawLineWithTranslate(graphics, new Line2D.Float(routeDescription.getStart(), routeDescription.getEnd()),
        view.getXOffset(), view.getYOffset());
      graphics.fillOval((routeDescription.getEnd().x - xOffset) - jointsize / 2, (routeDescription.getEnd().y - yOffset) - jointsize / 2, jointsize, jointsize);
    }
    else{
      final double[] index = getIndex(points);
      final float stepSize = 0.01f;//TODO calculating a step size that makes sense
      PolynomialSplineFunction xcurve = new SplineInterpolator().interpolate(index, pointsXToDoubleArray(points));
      PolynomialSplineFunction ycurve = new SplineInterpolator().interpolate(index, pointsYToDoubleArray(points));
      double[] xcoords = getCoords(xcurve, stepSize);
      double[] ycoords = getCoords(ycurve, stepSize);
      for(int i = 1; i < xcoords.length; i++){
        //TODO maybe a line is not the best way to draw this...
        drawLineWithTranslate(graphics, new Line2D.Double(xcoords[i-1], ycoords[i-1], xcoords[i], ycoords[i]), view.getXOffset(), view.getYOffset());
      }
      drawMoveLength(graphics, routeDescription, points, xOffset, yOffset, view, mapData.scrollWrapX(), mapData.scrollWrapY(), route.getAllTerritories().size(), movementLeftForCurrentUnits);
    }
    drawJoints(graphics, points, view.getXOffset(), view.getYOffset(), jointsize);
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

  private static void drawLineWithTranslate(Graphics2D graphics, Line2D line2D, double translateX, double translateY) {
      final Line2D line = (Line2D) line2D;
      final Point2D p1 = new Point2D.Double(line.getP1().getX() - translateX, line.getP1().getY() - translateY);
      final Point2D p2 = new Point2D.Double(line.getP2().getX() - translateX, line.getP2().getY() - translateY);
      graphics.draw(new Line2D.Double(p1, p2));
  }
  
  private static Point[] getPoints(RouteDescription routeDescription, MapData mapData){
    final int numTerritories = routeDescription.getRoute().getAllTerritories().size();
    final Point[] points = new Point[numTerritories];
    for (int i = 0; i < numTerritories; i++) {
      points[i] = mapData.getCenter(routeDescription.getRoute().getAllTerritories().get(i));
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
      
      
      final String textRouteMovement = String.valueOf(numTerritories - 1);
      final String unitMovementLeft = movementLeftForCurrentUnits == null || movementLeftForCurrentUnits.trim().length() <= 0 ? "" : "    /" + movementLeftForCurrentUnits;
      final BufferedImage movementImage = new BufferedImage(72, 24, BufferedImage.TYPE_INT_ARGB);
      
      final Graphics2D textG2D = movementImage.createGraphics();
      textG2D.setColor(Color.YELLOW);
      textG2D.setFont(new Font("Dialog", Font.BOLD, 20));
      textG2D.drawString(textRouteMovement, 0, 20);
      textG2D.setColor(new Color(33, 0, 127));
      textG2D.setFont(new Font("Dialog", Font.BOLD, 16));
      textG2D.drawString(unitMovementLeft, 0, 20);
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
}
