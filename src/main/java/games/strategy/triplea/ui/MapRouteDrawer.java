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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.logic.Line;
import games.strategy.triplea.ui.logic.Point;
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

  public MapRouteDrawer(final MapPanel mapPanel, final MapData mapData) {
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
    final int xOffset = mapPanel.getXOffset();
    final int yOffset = mapPanel.getYOffset();
    final Point[] points = routeCalculator.getTranslatedRoute(getRoutePoints(routeDescription));
    final boolean tooFewTerritories = numTerritories <= 1;
    final boolean tooFewPoints = points.length <= 2;
    final double scale = mapPanel.getScale();
    if (tooFewTerritories || tooFewPoints) {
      if (routeDescription.getEnd() != null) { // AI has no End Point
        drawDirectPath(graphics, new Point(routeDescription.getStart()), new Point(routeDescription.getEnd()), xOffset,
            yOffset, scale);
      } else {
        drawDirectPath(graphics, points[0], points[points.length - 1], xOffset, yOffset, scale);
      }
      if (tooFewPoints && !tooFewTerritories) {
        drawMoveLength(graphics, points, xOffset, yOffset, scale, numTerritories, maxMovement);
      }
    } else {
      drawCurvedPath(graphics, points, xOffset, yOffset, scale);
      drawMoveLength(graphics, points, xOffset, yOffset, scale, numTerritories, maxMovement);
    }
    drawJoints(graphics, points, xOffset, yOffset, scale);
    drawCustomCursor(graphics, routeDescription, xOffset, yOffset, scale);
  }

  /**
   * Draws Points on the Map.
   *
   * @param graphics The {@linkplain Graphics2D} Object being drawn on
   * @param points The {@linkplain Point} array aka the "Joints" to be drawn
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param jointsize The diameter of the Points being drawn
   * @param scale The scale-factor of the Map
   */
  private void drawJoints(final Graphics2D graphics, final Point[] points, final int xOffset, final int yOffset,
      final double scale) {
    final int jointsize = 10;
    // If the points array is bigger than 1 the last joint should not be drawn (draw an arrow instead)
    final Point[] newPoints = points.length > 1 ? Arrays.copyOf(points, points.length - 1) : points;
    for (Point[] joints : routeCalculator.getAllPoints(newPoints)) {
      for (final Point p : joints) {
        graphics.fillOval((int) (((p.getX() - xOffset) - (jointsize / 2) / scale) * scale),
            (int) (((p.getY() - yOffset) - (jointsize / 2) / scale) * scale), jointsize, jointsize);
      }
    }
  }

  /**
   * Draws a specified CursorImage if available.
   *
   * @param graphics The {@linkplain Graphics2D} Object being drawn on
   * @param routeDescription The RouteDescription object containing the CursorImage
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param scale The scale-factor of the Map
   */
  private void drawCustomCursor(final Graphics2D graphics, final RouteDescription routeDescription, final int xOffset,
      final int yOffset, final double scale) {
    final BufferedImage cursorImage = (BufferedImage) routeDescription.getCursorImage();
    if (cursorImage != null) {
      for (Point[] endPoint : routeCalculator.getAllPoints(routeCalculator.getLastEndPoint())) {
        graphics.drawImage(cursorImage,
            (int) (((endPoint[0].getX() - xOffset) - (cursorImage.getWidth() / 2)) * scale),
            (int) (((endPoint[0].getY() - yOffset) - (cursorImage.getHeight() / 2)) * scale), null);
      }
    }

  }

  /**
   * Draws a straight Line from the start to the stop of the specified {@linkplain RouteDescription}
   * Also draws a small little point at the end of the Line.
   *
   * @param graphics The {@linkplain Graphics2D} Object being drawn on
   * @param start The start {@linkplain Point} of the Path
   * @param end The end {@linkplain Point} of the Path
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param jointsize The diameter of the Points being drawn
   * @param scale The scale-factor of the Map
   */
  private void drawDirectPath(final Graphics2D graphics, final Point start, final Point end, final int xOffset,
      final int yOffset, final double scale) {
    final Point[] points = routeCalculator.getTranslatedRoute(start, end);
    for (Point[] newPoints : routeCalculator.getAllPoints(points)) {
      drawLineWithTranslate(graphics, new Line2D.Float(newPoints[0].toPoint(), newPoints[1].toPoint()), xOffset,
          yOffset, scale);
      if (newPoints[0].distance(newPoints[1]) > arrowLength) {
        drawArrow(graphics, newPoints[0].toPoint(), newPoints[1].toPoint(), xOffset, yOffset, scale);
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
  protected double[] createParameterizedIndex(final Point[] points) {
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
   * @param line The Line to be drawn
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param scale The scale-factor of the Map
   */
  private void drawLineWithTranslate(final Graphics2D graphics, final Line2D line, final double xOffset,
      final double yOffset, final double scale) {
    graphics.draw(
        new Line2D.Double(
            new Point2D.Double((line.getP1().getX() - xOffset) * scale, (line.getP1().getY() - yOffset) * scale),
            new Point2D.Double((line.getP2().getX() - xOffset) * scale, (line.getP2().getY() - yOffset) * scale)));
  }

  /**
   * Creates a {@linkplain Point} Array out of a {@linkplain RouteDescription} and a {@linkplain MapData} object.
   *
   * @param routeDescription {@linkplain RouteDescription} containing the Route information
   * @param mapData {@linkplain MapData} Object containing Information about the Map Coordinates
   * @return The {@linkplain Point} array specified by the {@linkplain RouteDescription} and {@linkplain MapData}
   *         objects
   */
  protected Point[] getRoutePoints(final RouteDescription routeDescription) {
    final List<Territory> territories = routeDescription.getRoute().getAllTerritories();
    final int numTerritories = territories.size();
    final Point[] points = new Point[numTerritories];
    for (int i = 0; i < numTerritories; i++) {
      points[i] = new Point(mapData.getCenter(territories.get(i)));
    }
    if (routeDescription.getStart() != null) {
      points[0] = new Point(routeDescription.getStart());
    }
    if (routeDescription.getEnd() != null && numTerritories > 1) {
      points[numTerritories - 1] = new Point(routeDescription.getEnd());
    }
    return points;
  }

  /**
   * Creates double arrays of y or x coordinates of the given {@linkplain Point} Array.
   *
   * @param points The {@linkplain Point} Array containing the Coordinates
   * @param extractor A function specifying which value to return
   * @return A double array with values specified by the given function
   */
  protected double[] getValues(final Point[] points, final Function<Point, Double> extractor) {
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
   * @param points The {@linkplain Point} array of the unit's tour
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param scale The scale-factor of the Map
   * @param numTerritories how many Territories the unit traveled so far
   * @param maxMovement The String indicating how man
   */
  private void drawMoveLength(final Graphics2D graphics, final Point[] points,
      final int xOffset, final int yOffset, final double scale, final int numTerritories,
      final String maxMovement) {
    final Point cursorPos = points[points.length - 1];
    final String unitMovementLeft =
        maxMovement == null || maxMovement.trim().length() == 0 ? ""
            : "    /" + maxMovement;
    final BufferedImage movementImage = new BufferedImage(50, 20, BufferedImage.TYPE_INT_ARGB);
    createMovementLeftImage(movementImage, String.valueOf(numTerritories - 1), unitMovementLeft);

    final int textXOffset = -movementImage.getWidth() / 2;
    final double yDir = cursorPos.getY() - points[numTerritories - 2].getY();
    final int textYOffset = yDir > 0 ? movementImage.getHeight() : movementImage.getHeight() * -2;
    for (Point[] cursorPositions : routeCalculator.getAllPoints(cursorPos)) {
      graphics.drawImage(movementImage,
          (int) ((cursorPositions[0].getX() + textXOffset - xOffset) * scale),
          (int) ((cursorPositions[0].getY() + textYOffset - yOffset) * scale), null);
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
   * Finally those 2 interpolated arrays get unified into a single {@linkplain Point} array and drawn to the Map
   * </p>
   *
   * @param graphics The {@linkplain Graphics2D} Object to be drawn on
   * @param points The Knot Points for the Spline-Interpolator aka the joints
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param scale The scale-factor of the Map
   */
  private void drawCurvedPath(final Graphics2D graphics, final Point[] points, final int xOffset, final int yOffset,
      final double scale) {
    final double[] index = createParameterizedIndex(points);
    final PolynomialSplineFunction xcurve =
        splineInterpolator.interpolate(index, getValues(points, point -> point.getX()));
    final double[] xcoords = getCoords(xcurve, index);
    final PolynomialSplineFunction ycurve =
        splineInterpolator.interpolate(index, getValues(points, point -> point.getY()));
    final double[] ycoords = getCoords(ycurve, index);
    List<Line> lines = routeCalculator.getAllNormalizedLines(xcoords, ycoords);
    for (Line line : lines) {
      drawLineWithTranslate(graphics, line.toLine2D(), xOffset, yOffset, scale);
    }
    // draws the Line to the Cursor on every possible screen, so that the line ends at the cursor no matter what...
    List<Point[]> finishingPoints = routeCalculator.getAllPoints(
        new Point(xcoords[xcoords.length - 1], ycoords[ycoords.length - 1]), points[points.length - 1]);
    boolean hasArrowEnoughSpace = points[points.length - 2].distance(points[points.length - 1]) > arrowLength;
    for (Point[] finishingPointArray : finishingPoints) {
      drawLineWithTranslate(graphics,
          new Line(finishingPointArray[0], finishingPointArray[1]).toLine2D(),
          xOffset, yOffset, scale);
      if (hasArrowEnoughSpace) {
        drawArrow(graphics, finishingPointArray[0].toPoint(), finishingPointArray[1].toPoint(), xOffset, yOffset,
            scale);
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
  private void createMovementLeftImage(final BufferedImage image, final String curMovement, final String maxMovement) {
    final Graphics2D textG2D = image.createGraphics();
    textG2D.setColor(Color.YELLOW);
    textG2D.setFont(new Font("Dialog", Font.BOLD, 20));
    final int textThicknessOffset = textG2D.getFontMetrics().stringWidth(curMovement) / 2;
    final boolean distanceTooBig = maxMovement.equals("");
    textG2D.drawString(curMovement, distanceTooBig ? image.getWidth() / 2 - textThicknessOffset : 10,
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
   * @param from The {@linkplain Point2D} specifying the direction of the Arrow
   * @param to The {@linkplain Point2D} where the arrow is placed
   * @return A transformed Arrow-Shape
   */
  private static Shape createArrowTipShape(final Point2D from, final Point2D to) {
    final int arrowOffset = 1;
    final Polygon arrowPolygon = new Polygon();
    arrowPolygon.addPoint(arrowOffset - arrowLength, arrowLength / 2);
    arrowPolygon.addPoint(arrowOffset, 0);
    arrowPolygon.addPoint(arrowOffset - arrowLength, arrowLength / -2);


    final AffineTransform transform = new AffineTransform();
    transform.translate(to.getX(), to.getY());
    transform.scale(arrowLength, arrowLength);
    final double rotate = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
    transform.rotate(rotate);

    return transform.createTransformedShape(arrowPolygon);
  }

  /**
   * Draws an Arrow on the {@linkplain Graphics2D} Object.
   *
   * @param graphics The {@linkplain Graphics2D} object to draw on
   * @param from The destination {@linkplain Point2D} form the Arrow
   * @param to The placement {@linkplain Point2D} for the Arrow
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param scale The scale-factor of the Map
   */
  private static void drawArrow(final Graphics2D graphics, final Point2D from, final Point2D to, final int xOffset,
      final int yOffset, final double scale) {
    final Point2D scaledStart = new Point2D.Double((from.getX() - xOffset) * scale,
        (from.getY() - yOffset) * scale);
    final Point2D scaledEnd = new Point2D.Double((to.getX() - xOffset) * scale,
        (to.getY() - yOffset) * scale);
    graphics.fill(createArrowTipShape(scaledStart, scaledEnd));
  }
}
