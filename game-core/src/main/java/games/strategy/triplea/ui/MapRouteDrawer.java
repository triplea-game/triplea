package games.strategy.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.logic.RouteCalculator;
import games.strategy.triplea.ui.mapdata.MapData;

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
  public static final double DETAIL_LEVEL = 1.0;
  private static final int arrowLength = 4;

  private final RouteCalculator routeCalculator;
  private final MapData mapData;
  private final MapPanel mapPanel;

  MapRouteDrawer(final MapPanel mapPanel, final MapData mapData) {
    routeCalculator = new RouteCalculator(mapData.scrollWrapX(), mapData.scrollWrapY(), mapPanel.getImageWidth(),
        mapPanel.getImageHeight());
    this.mapData = checkNotNull(mapData);
    this.mapPanel = checkNotNull(mapPanel);
  }

  /**
   * Draws the route to the screen.
   */
  public void drawRoute(final Graphics2D graphics, final RouteDescription routeDescription, final String maxMovement) {
    final Route route = routeDescription.getRoute();
    if (route == null) {
      return;
    }
    // set thickness and color of the future drawings
    graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    graphics.setPaint(Color.red);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    final int numTerritories = route.getAllTerritories().size();
    final Point2D[] points = routeCalculator.getTranslatedRoute(getRoutePoints(routeDescription));
    final boolean tooFewTerritories = numTerritories <= 1;
    final boolean tooFewPoints = points.length <= 2;
    if (tooFewTerritories || tooFewPoints) {
      if (routeDescription.getEnd() != null) { // AI has no End Point
        drawDirectPath(graphics, routeDescription.getStart(), routeDescription.getEnd());
      } else {
        drawDirectPath(graphics, points[0], points[points.length - 1]);
      }
      if (tooFewPoints && !tooFewTerritories) {
        drawMoveLength(graphics, points, numTerritories, maxMovement);
      }
    } else {
      drawCurvedPath(graphics, points);
      drawMoveLength(graphics, points, numTerritories, maxMovement);
    }
    drawJoints(graphics, points);
    drawCustomCursor(graphics, routeDescription, points[points.length - 1]);
  }

  /**
   * Draws Points on the Map.
   *
   * @param graphics The {@linkplain Graphics2D} Object being drawn on
   * @param points The {@linkplain Point2D} array aka the "Joints" to be drawn
   */
  private void drawJoints(final Graphics2D graphics, final Point2D[] points) {
    final int jointsize = 10;
    // If the points array is bigger than 1 the last joint should not be drawn (draw an arrow instead)
    final Point2D[] newPoints = (points.length > 1) ? Arrays.copyOf(points, points.length - 1) : points;
    for (final Point2D[] joints : routeCalculator.getAllPoints(newPoints)) {
      for (final Point2D p : joints) {
        final Ellipse2D circle = new Ellipse2D.Double(jointsize / -2, jointsize / -2, jointsize, jointsize);
        final AffineTransform ellipseTransform = getDrawingTransform();
        ellipseTransform.translate(p.getX(), p.getY());
        final double scale = mapPanel.getScale();
        ellipseTransform.scale(1 / scale, 1 / scale);
        graphics.fill(ellipseTransform.createTransformedShape(circle));
      }
    }
  }

  /**
   * Draws a specified CursorImage if available.
   *
   * @param graphics The {@linkplain Graphics2D} Object being drawn on
   * @param routeDescription The RouteDescription object containing the CursorImage
   * @param lastRoutePoint The last {@linkplain Point2D} on the drawn Route as a center for the cursor icon.
   */
  private void drawCustomCursor(final Graphics2D graphics, final RouteDescription routeDescription,
      final Point2D lastRoutePoint) {
    final BufferedImage cursorImage = (BufferedImage) routeDescription.getCursorImage();
    if (cursorImage != null) {
      for (final Point2D[] endPoint : routeCalculator.getAllPoints(lastRoutePoint)) {
        final AffineTransform imageTransform = getDrawingTransform();
        imageTransform.translate(endPoint[0].getX(), endPoint[0].getY());
        imageTransform.translate(cursorImage.getWidth() / 2.0, cursorImage.getHeight() / 2.0);
        imageTransform.scale(1 / mapPanel.getScale(), 1 / mapPanel.getScale());
        graphics.drawImage(cursorImage, imageTransform, null);
      }
    }
  }

  /**
   * Draws a straight Line from the start to the stop of the specified {@linkplain RouteDescription}
   * Also draws a small little point at the end of the Line.
   *
   * @param graphics The {@linkplain Graphics2D} Object being drawn on
   * @param start The start {@linkplain Point2D} of the Path
   * @param end The end {@linkplain Point2D} of the Path
   */
  private void drawDirectPath(final Graphics2D graphics, final Point2D start, final Point2D end) {
    final Point2D[] points = routeCalculator.getTranslatedRoute(start, end);
    for (final Point2D[] newPoints : routeCalculator.getAllPoints(points)) {
      drawTransformedShape(graphics, new Line2D.Float(newPoints[0], newPoints[1]));
      if (newPoints[0].distance(newPoints[1]) > arrowLength) {
        drawArrow(graphics, newPoints[0], newPoints[1]);
      }
    }
  }

  /**
   * Centripetal parameterization
   *
   * <p>
   * Check <a href="http://stackoverflow.com/a/37370620/5769952">http://stackoverflow.com/a/37370620/5769952</a> for
   * more information
   * </p>
   *
   * @param points - The Points which should be parameterized
   * @return A Parameter-Array called the "Index"
   */
  protected double[] createParameterizedIndex(final Point2D[] points) {
    final double[] index = new double[points.length];
    if (index.length > 0) {
      index[0] = 0;
    }
    for (int i = 1; i < points.length; i++) {
      index[i] = index[i - 1] + Math.sqrt(points[i - 1].distance(points[i]));
    }
    return index;
  }

  /**
   * Draws a line to the Screen regarding the Map-Offset and scale.
   *
   * @param graphics The {@linkplain Graphics2D} Object to be drawn on
   * @param shape The Shape to be drawn
   */
  private void drawTransformedShape(final Graphics2D graphics, final Shape shape) {
    graphics.draw(getDrawingTransform().createTransformedShape(shape));
  }

  /**
   * Creates a {@linkplain Point2D} Array out of a {@linkplain RouteDescription} and a {@linkplain MapData} object.
   *
   * @param routeDescription {@linkplain RouteDescription} containing the Route information
   * @param mapData {@linkplain MapData} Object containing Information about the Map Coordinates
   * @return The {@linkplain Point2D} array specified by the {@linkplain RouteDescription} and {@linkplain MapData}
   *         objects
   */
  protected Point2D[] getRoutePoints(final RouteDescription routeDescription) {
    final List<Territory> territories = routeDescription.getRoute().getAllTerritories();
    final int numTerritories = territories.size();
    final Point2D[] points = new Point2D[numTerritories];
    for (int i = 0; i < numTerritories; i++) {
      points[i] = mapData.getCenter(territories.get(i));
    }
    if (routeDescription.getStart() != null) {
      points[0] = routeDescription.getStart();
    }
    if ((routeDescription.getEnd() != null) && (numTerritories > 1)) {
      points[numTerritories - 1] = routeDescription.getEnd();
    }
    return points;
  }

  /**
   * Creates double arrays of y or x coordinates of the given {@linkplain Point2D} Array.
   *
   * @param points The {@linkplain Point2D} Array containing the Coordinates
   * @param extractor A function specifying which value to return
   * @return A double array with values specified by the given function
   */
  protected double[] getValues(final Point2D[] points, final Function<Point2D, Double> extractor) {
    final double[] result = new double[points.length];
    for (int i = 0; i < points.length; i++) {
      result[i] = extractor.apply(points[i]);
    }
    return result;

  }

  /**
   * Creates a double array containing y coordinates of a {@linkplain PolynomialSplineFunction} with the above specified
   * {@code DETAIL_LEVEL}.
   *
   * @param fuction The {@linkplain PolynomialSplineFunction} with the values
   * @param index the parameterized array to indicate the maximum Values
   * @return an array of double-precision y values of the specified function
   */
  protected double[] getCoords(final PolynomialSplineFunction fuction, final double[] index) {
    final double defaultCoordSize = index[index.length - 1];
    final double[] coords = new double[(int) Math.round(DETAIL_LEVEL * defaultCoordSize) + 1];
    final double stepSize = fuction.getKnots()[fuction.getKnots().length - 1] / coords.length;
    double curValue = 0;
    for (int i = 0; i < coords.length; i++) {
      coords[i] = fuction.value(curValue);
      curValue += stepSize;
    }
    return coords;
  }

  /**
   * Draws how many moves are left.
   *
   * @param graphics The {@linkplain Graphics2D} Object to be drawn on
   * @param points The {@linkplain Point2D} array of the unit's tour
   * @param numTerritories how many Territories the unit traveled so far
   * @param maxMovement The String indicating how man
   */
  private void drawMoveLength(final Graphics2D graphics, final Point2D[] points, final int numTerritories,
      final String maxMovement) {
    final Point2D cursorPos = points[points.length - 1];
    final String unitMovementLeft =
        ((maxMovement == null) || (maxMovement.trim().length() == 0)) ? ""
            : ("    /" + maxMovement);
    final BufferedImage movementImage = new BufferedImage(50, 20, BufferedImage.TYPE_INT_ARGB);
    createMovementLeftImage(movementImage, String.valueOf(numTerritories - 1), unitMovementLeft);

    final int textXOffset = -movementImage.getWidth() / 2;
    final double deltaY = cursorPos.getY() - points[numTerritories - 2].getY();
    final int textYOffset = (deltaY > 0) ? movementImage.getHeight() : (movementImage.getHeight() * -2);
    for (final Point2D[] cursorPositions : routeCalculator.getAllPoints(cursorPos)) {
      final AffineTransform imageTransform = getDrawingTransform();
      imageTransform.translate(textXOffset, textYOffset);
      imageTransform.translate(cursorPositions[0].getX(), cursorPositions[0].getY());
      imageTransform.scale(1 / mapPanel.getScale(), 1 / mapPanel.getScale());
      graphics.drawImage(movementImage, imageTransform, null);
    }
  }

  /**
   * Draws a smooth curve through the given array of points
   *
   * <p>
   * This algorithm is called Spline-Interpolation
   * because the Apache-commons-math library we are using here does not accept
   * values but {@code f(x)=y} with x having to increase all the time
   * the idea behind this is to use a parameter array - the so called index
   * as x array and splitting the points into a x and y coordinates array.
   * </p>
   *
   * <p>
   * Finally those 2 interpolated arrays get unified into a single {@linkplain Point2D} array and drawn to the Map
   * </p>
   *
   * @param graphics The {@linkplain Graphics2D} Object to be drawn on
   * @param points The Knot Points for the Spline-Interpolator aka the joints
   */
  private void drawCurvedPath(final Graphics2D graphics, final Point2D[] points) {
    final double[] index = createParameterizedIndex(points);
    final PolynomialSplineFunction xcurve =
        splineInterpolator.interpolate(index, getValues(points, point -> point.getX()));
    final double[] xcoords = getCoords(xcurve, index);
    final PolynomialSplineFunction ycurve =
        splineInterpolator.interpolate(index, getValues(points, point -> point.getY()));
    final double[] ycoords = getCoords(ycurve, index);
    final List<Path2D> paths = routeCalculator.getAllNormalizedLines(xcoords, ycoords);
    for (final Path2D path : paths) {
      drawTransformedShape(graphics, path);
    }
    // draws the Line to the Cursor on every possible screen, so that the line ends at the cursor no matter what...
    final List<Point2D[]> finishingPoints = routeCalculator.getAllPoints(
        new Point2D.Double(xcoords[xcoords.length - 1], ycoords[ycoords.length - 1]),
        points[points.length - 1]);
    final boolean hasArrowEnoughSpace = points[points.length - 2].distance(points[points.length - 1]) > arrowLength;
    for (final Point2D[] finishingPointArray : finishingPoints) {
      drawTransformedShape(graphics, new Line2D.Double(finishingPointArray[0], finishingPointArray[1]));
      if (hasArrowEnoughSpace) {
        drawArrow(graphics, finishingPointArray[0], finishingPointArray[1]);
      }
    }
  }

  /**
   * This draws how many moves are left on the given {@linkplain BufferedImage}
   *
   * @param image The Image to be drawn on
   * @param curMovement How many territories the unit traveled so far
   * @param maxMovement How many territories is allowed to travel. Is empty when the unit traveled too far
   */
  private static void createMovementLeftImage(final BufferedImage image, final String curMovement,
      final String maxMovement) {
    final Graphics2D textG2D = image.createGraphics();
    textG2D.setColor(Color.YELLOW);
    textG2D.setFont(new Font("Dialog", Font.BOLD, 20));
    final int textThicknessOffset = textG2D.getFontMetrics().stringWidth(curMovement) / 2;
    final boolean distanceTooBig = maxMovement.isEmpty();
    textG2D.drawString(
        curMovement,
        distanceTooBig ? ((image.getWidth() / 2) - textThicknessOffset) : 10,
        image.getHeight());
    if (!distanceTooBig) {
      textG2D.setColor(new Color(33, 0, 127));
      textG2D.setFont(new Font("Dialog", Font.BOLD, 16));
      textG2D.drawString(maxMovement, 10, image.getHeight());
    }
  }

  /**
   * Creates an Arrow-Shape.
   *
   * @param angle The radiant angle at which the arrow should be rotated
   * @return A transformed Arrow-Shape
   */
  private static Shape createArrowTipShape(final double angle) {
    final int arrowOffset = 1;
    final Polygon arrowPolygon = new Polygon();
    arrowPolygon.addPoint(arrowOffset - arrowLength, arrowLength / 2);
    arrowPolygon.addPoint(arrowOffset, 0);
    arrowPolygon.addPoint(arrowOffset - arrowLength, arrowLength / -2);


    final AffineTransform transform = new AffineTransform();
    transform.scale(arrowLength, arrowLength);
    transform.rotate(angle);

    return transform.createTransformedShape(arrowPolygon);
  }

  /**
   * Draws an Arrow on the {@linkplain Graphics2D} Object.
   *
   * @param graphics The {@linkplain Graphics2D} object to draw on
   * @param from The destination {@linkplain Point2D} form the Arrow
   * @param to The placement {@linkplain Point2D} for the Arrow
   */
  private void drawArrow(final Graphics2D graphics, final Point2D from, final Point2D to) {
    final Shape arrow = createArrowTipShape(Math.atan2(to.getY() - from.getY(), to.getX() - from.getX()));
    final double scale = mapPanel.getScale();
    final Shape antiScaledArrow = AffineTransform.getScaleInstance(1 / scale, 1 / scale).createTransformedShape(arrow);
    final AffineTransform transform = getDrawingTransform();
    transform.translate(to.getX(), to.getY());
    graphics.fill(transform.createTransformedShape(antiScaledArrow));
  }

  private AffineTransform getDrawingTransform() {
    final double scale = mapPanel.getScale();
    final AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
    transform.translate(-mapPanel.getXOffset(), -mapPanel.getYOffset());
    return transform;
  }
}
