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
import java.util.List;
import java.util.function.Function;

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
   * This value influences the "resolution" of the Path.
   * Too low values make the Path look edgy, too high values will cause lag and rendering errors
   * because the distance between the drawing segments is shorter than 2 pixels 
   */
  public static final double DETAIL_LEVEL = 1.0D;

  /**
   * Draws the route to the screen.
   */
  public static void drawRoute(final Graphics2D graphics, final RouteDescription routeDescription, final MapPanel view,
      final MapData mapData, final String movementLeftForCurrentUnits) {
    if (routeDescription == null) {
      return;
    }
    final Route route = routeDescription.getRoute();
    if (route == null) {
      return;
    }
    // set thickness and color of the future drawings
    graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    graphics.setPaint(Color.red);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    final int numTerritories = route.getAllTerritories().size();
    final Point[] points = getRoutePoints(routeDescription, mapData);
    final boolean tooFewTerritories = numTerritories <= 1;
    final boolean tooFewPoints = points.length <= 2;
    final int xOffset = view.getXOffset();
    final int yOffset = view.getYOffset();
    final double scale = view.getScale();
    final int jointsize = 10;
    if (tooFewTerritories || tooFewPoints) {
      drawDirectPath(graphics, routeDescription, xOffset, yOffset, scale, jointsize);
      if(tooFewPoints && !tooFewTerritories){
        drawMoveLength(graphics, routeDescription, points, xOffset, yOffset, view, numTerritories, movementLeftForCurrentUnits);
      }
    } else {
      drawCurvedPath(graphics, points, view);
      drawMoveLength(graphics, routeDescription, points, xOffset, yOffset, view, numTerritories, movementLeftForCurrentUnits);
    }
    drawJoints(graphics, points, xOffset, yOffset, jointsize, scale);
    drawCustomCursor(graphics, routeDescription, xOffset, yOffset, scale);
  }
  
  /**
   * Draws Points on the Map
   * 
   * @param graphics The Graphics2D being drawn on
   * @param points The Point array aka the "Joints" to be drawn
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param jointsize The diameter of the Points being drawn
   * @param scale The scale-factor of the Map
   */
  private static void drawJoints(Graphics2D graphics, Point[] points, int xOffset, int yOffset, int jointsize,
      double scale) {
    for (Point p : points) {
      graphics.fillOval((int) (((p.x - xOffset) - jointsize / 2) * scale),
          (int) (((p.y - yOffset) - jointsize / 2) * scale), jointsize, jointsize);
    }

  }
  
  
  private static void drawCustomCursor(Graphics2D graphics, RouteDescription routeDescription, int xOffset, int yOffset, double scale){
    final Image cursorImage = routeDescription.getCursorImage();
    if (cursorImage != null) {
      graphics.drawImage(cursorImage,
          (int) (((routeDescription.getEnd().x - xOffset) - (cursorImage.getWidth(null) / 2)) * scale),
          (int) (((routeDescription.getEnd().y - yOffset) - (cursorImage.getHeight(null) / 2)) * scale), null);
    }
    
  }
  
  private static void drawDirectPath(Graphics2D graphics, RouteDescription routeDescription, int xOffset, int yOffset, double scale, int jointsize){
    drawLineWithTranslate(graphics, new Line2D.Float(routeDescription.getStart(), routeDescription.getEnd()), xOffset,
        yOffset, scale);
    graphics.fillOval(
        (int) (((routeDescription.getEnd().x - xOffset) - jointsize / 2) * scale),
        (int) (((routeDescription.getEnd().y - yOffset) - jointsize / 2) * scale),
        jointsize, jointsize);
  }
  /**
   * Centripetal parameterization
   * 
   * Check http://stackoverflow.com/a/37370620/5769952 for more information
   * 
   * @param points - The Points which should be parameterized
   * @return A Parameter-Array called the "Index"
   */
  protected static double[] createParameterizedIndex(Point[] points) {
    final double[] index = new double[points.length];
    if(index.length > 0){
      index[0] = 0;
    }
    for (int i = 1; i < points.length; i++) {
      index[i] = index[i - 1] + Math.sqrt(points[i - 1].distance(points[i]));
    }
    return index;
  }

  private static void drawLineWithTranslate(Graphics2D graphics, Line2D line, double translateX, double translateY,
      double scale) {
    final Point2D point1 =
        new Point2D.Double((line.getP1().getX() - translateX) * scale, (line.getP1().getY() - translateY) * scale);
    final Point2D point2 =
        new Point2D.Double((line.getP2().getX() - translateX) * scale, (line.getP2().getY() - translateY) * scale);
    graphics.draw(new Line2D.Double(point1, point2));
    //TODO skip drawing when none of those points is on screen in order to increase performance
  }

  protected static Point[] getRoutePoints(RouteDescription routeDescription, MapData mapData) {
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
  
  protected static double[] getValues(Point[] points, Function<Point, Double> extractor){
    double[] result = new double[points.length];
    for (int i = 0; i < points.length; i++) {
      result[i] = extractor.apply(points[i]);
    }
    return result;
    
  }

  protected static double[] getCoords(PolynomialSplineFunction curve, double[] index) {
    final double defaultCoordSize = index[index.length - 1];
    final double[] coords = new double[(int)Math.round(DETAIL_LEVEL * defaultCoordSize) + 1];
    final double stepSize = curve.getKnots()[curve.getKnots().length - 1] / coords.length;
    double curValue = 0;
    for (int i = 0; i < coords.length; i ++) {
      coords[i] = curve.value(curValue);
      curValue += stepSize;
    }
    return coords;
  }

  private static void drawMoveLength(Graphics2D graphics, RouteDescription routeDescription, Point[] points,
      int xOffset, int yOffset, MapPanel view, int numTerritories,
      String movementLeftForCurrentUnits) {
    final double scale = view.getScale();
    final Point cursorPos = points[points.length - 1];
    final String unitMovementLeft =
        movementLeftForCurrentUnits == null || movementLeftForCurrentUnits.trim().length() == 0 ? ""
            : "    /" + movementLeftForCurrentUnits;
    final BufferedImage movementImage = new BufferedImage(50, 20, BufferedImage.TYPE_INT_ARGB);
    setupTextImage(movementImage, String.valueOf(numTerritories - 1), unitMovementLeft);
    
    final int textXOffset = -movementImage.getWidth() / 2;
    final int yDir = cursorPos.y - points[numTerritories - 2].y;
    final int textYOffset = yDir > 0 ? movementImage.getHeight() : movementImage.getHeight() * -2;
    graphics.drawImage(movementImage,
        (int) ((cursorPos.x + textXOffset - xOffset) * scale),
        (int) ((cursorPos.y + textYOffset - yOffset) * scale), null);
  }

  private static void drawCurvedPath(Graphics2D graphics, Point[] points, MapPanel view) {
    final double[] index = createParameterizedIndex(points);
    final PolynomialSplineFunction xcurve = splineInterpolator.interpolate(index, getValues(points, point -> point.getX()));
    final double[] xcoords = getCoords(xcurve, index);
    final PolynomialSplineFunction ycurve = splineInterpolator.interpolate(index, getValues(points, point -> point.getY()));
    final double[] ycoords = getCoords(ycurve, index);

    for (int i = 1; i < xcoords.length; i++) {
      drawLineWithTranslate(graphics, new Line2D.Double(xcoords[i - 1], ycoords[i - 1], xcoords[i], ycoords[i]),
          view.getXOffset(), view.getYOffset(), view.getScale());
    }
    // draws the Line to the Cursor, so that the line ends at the cursor no matter what...
    drawLineWithTranslate(graphics,
        new Line2D.Double(new Point2D.Double(xcoords[xcoords.length - 1], ycoords[ycoords.length - 1]),
            points[points.length - 1]),
        view.getXOffset(), view.getYOffset(), view.getScale());
  }

  private static void setupTextImage(BufferedImage image, String textRouteMovement, String unitMovementLeft) {
    final Graphics2D textG2D = image.createGraphics();
    textG2D.setColor(Color.YELLOW);
    textG2D.setFont(new Font("Dialog", Font.BOLD, 20));
    final int textThicknessOffset = textG2D.getFontMetrics().stringWidth(textRouteMovement) / 2;
    final boolean distanceTooBig = unitMovementLeft.equals("");
    textG2D.drawString(textRouteMovement, distanceTooBig ? image.getWidth() / 2 - textThicknessOffset : 0, image.getHeight());
    if(!distanceTooBig){
      textG2D.setColor(new Color(33, 0, 127));
      textG2D.setFont(new Font("Dialog", Font.BOLD, 16));
      textG2D.drawString(unitMovementLeft, 0, image.getHeight());
    }
  }
}
