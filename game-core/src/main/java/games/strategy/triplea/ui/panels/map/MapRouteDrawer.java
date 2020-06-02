package games.strategy.triplea.ui.panels.map;

import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.image.ResourceImageFactory;
import games.strategy.triplea.ui.logic.RouteCalculator;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
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

/** Draws a route on a map. */
public class MapRouteDrawer {

  private static final SplineInterpolator splineInterpolator = new SplineInterpolator();
  /**
   * This value influences the "resolution" of the Path. Too low values make the Path look edgy, too
   * high values will cause lag and rendering errors because the distance between the drawing
   * segments is shorter than 2 pixels
   */
  private static final double DETAIL_LEVEL = 1.0;

  private static final int ARROW_LENGTH = 4;
  private static final int MESSAGE_HEIGHT = 26;
  private static final int MESSAGE_PADDING = 8;
  private static final int MESSAGE_TEXT_Y = 18;
  private static final int MESSAGE_TEXT_SPACING = 6;
  private static final Font MESSAGE_FONT = new Font("Dialog", Font.BOLD, 16);

  private final RouteCalculator routeCalculator;
  private final MapData mapData;
  private final MapPanel mapPanel;

  MapRouteDrawer(final MapPanel mapPanel, final MapData mapData) {
    routeCalculator =
        RouteCalculator.builder()
            .isInfiniteX(mapData.scrollWrapX())
            .isInfiniteY(mapData.scrollWrapY())
            .mapWidth(mapPanel.getImageWidth())
            .mapHeight(mapPanel.getImageHeight())
            .build();
    this.mapData = mapData;
    this.mapPanel = mapPanel;
  }

  /** Draws the route to the screen. */
  public void drawRoute(
      final Graphics2D graphics,
      final RouteDescription routeDescription,
      final String maxMovement,
      final ResourceCollection movementFuelCost,
      final ResourceImageFactory resourceImageFactory) {
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
        drawMoveLength(
            graphics, points, numTerritories, maxMovement, movementFuelCost, resourceImageFactory);
      }
    } else {
      drawCurvedPath(graphics, points);
      drawMoveLength(
          graphics, points, numTerritories, maxMovement, movementFuelCost, resourceImageFactory);
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
    // If the points array is bigger than 1 the last joint should not be drawn (draw an arrow
    // instead)
    final Point2D[] newPoints =
        points.length > 1 ? Arrays.copyOf(points, points.length - 1) : points;
    for (final Point2D[] joints : routeCalculator.getAllPoints(newPoints)) {
      for (final Point2D p : joints) {
        final Ellipse2D circle =
            new Ellipse2D.Double(jointsize / -2.0, jointsize / -2.0, jointsize, jointsize);
        final AffineTransform ellipseTransform = getDrawingTransform();
        ellipseTransform.translate(p.getX(), p.getY());
        graphics.fill(ellipseTransform.createTransformedShape(circle));
      }
    }
  }

  /**
   * Draws a specified CursorImage if available.
   *
   * @param graphics The {@linkplain Graphics2D} Object being drawn on
   * @param routeDescription The RouteDescription object containing the CursorImage
   * @param lastRoutePoint The last {@linkplain Point2D} on the drawn Route as a center for the
   *     cursor icon.
   */
  private void drawCustomCursor(
      final Graphics2D graphics,
      final RouteDescription routeDescription,
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
      if (newPoints[0].distance(newPoints[1]) > ARROW_LENGTH) {
        drawArrow(graphics, newPoints[0], newPoints[1]);
      }
    }
  }

  /**
   * Centripetal parameterization.
   *
   * <p>Check <a
   * href="http://stackoverflow.com/a/37370620/5769952">http://stackoverflow.com/a/37370620/5769952</a>
   * for more information
   *
   * @param points - The Points which should be parameterized
   * @return A Parameter-Array called the "Index"
   */
  protected double[] newParameterizedIndex(final Point2D[] points) {
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
   * Draws a line to the Screen regarding the Map-Offset.
   *
   * @param graphics The {@linkplain Graphics2D} Object to be drawn on
   * @param shape The Shape to be drawn
   */
  private void drawTransformedShape(final Graphics2D graphics, final Shape shape) {
    graphics.draw(getDrawingTransform().createTransformedShape(shape));
  }

  /**
   * Creates a {@linkplain Point2D} Array out of a {@linkplain RouteDescription} and a {@linkplain
   * MapData} object.
   *
   * @param routeDescription {@linkplain RouteDescription} containing the Route information
   * @return The {@linkplain Point2D} array specified by the {@linkplain RouteDescription} and
   *     {@linkplain MapData} objects
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
    if (routeDescription.getEnd() != null && numTerritories > 1) {
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
   * Creates a double array containing y coordinates of a {@linkplain PolynomialSplineFunction} with
   * the above specified {@code DETAIL_LEVEL}.
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
  private void drawMoveLength(
      final Graphics2D graphics,
      final Point2D[] points,
      final int numTerritories,
      final String maxMovement,
      final ResourceCollection movementFuelCost,
      final ResourceImageFactory resourceImageFactory) {

    final BufferedImage movementImage =
        newMovementLeftImage(
            String.valueOf(numTerritories - 1),
            maxMovement,
            movementFuelCost,
            resourceImageFactory);
    final int textXOffset = -movementImage.getWidth() / 2;
    final Point2D cursorPos = points[points.length - 1];
    final double deltaY = cursorPos.getY() - points[numTerritories - 2].getY();
    final int textYOffset = deltaY > 0 ? movementImage.getHeight() : movementImage.getHeight() * -2;
    for (final Point2D[] cursorPositions : routeCalculator.getAllPoints(cursorPos)) {
      final AffineTransform imageTransform = getDrawingTransform();
      imageTransform.translate(textXOffset, textYOffset);
      imageTransform.translate(cursorPositions[0].getX(), cursorPositions[0].getY());
      imageTransform.scale(1 / mapPanel.getScale(), 1 / mapPanel.getScale());
      graphics.drawImage(movementImage, imageTransform, null);
    }
  }

  /**
   * Draws a smooth curve through the given array of points.
   *
   * <p>This algorithm is called Spline-Interpolation because the Apache-commons-math library we are
   * using here does not accept values but {@code f(x)=y} with x having to increase all the time the
   * idea behind this is to use a parameter array - the so called index as x array and splitting the
   * points into a x and y coordinates array.
   *
   * <p>Finally those 2 interpolated arrays get unified into a single {@linkplain Point2D} array and
   * drawn to the Map
   *
   * @param graphics The {@linkplain Graphics2D} Object to be drawn on
   * @param points The Knot Points for the Spline-Interpolator aka the joints
   */
  private void drawCurvedPath(final Graphics2D graphics, final Point2D[] points) {
    final double[] index = newParameterizedIndex(points);
    final PolynomialSplineFunction xcurve =
        splineInterpolator.interpolate(index, getValues(points, Point2D::getX));
    final double[] xcoords = getCoords(xcurve, index);
    final PolynomialSplineFunction ycurve =
        splineInterpolator.interpolate(index, getValues(points, Point2D::getY));
    final double[] ycoords = getCoords(ycurve, index);
    final List<Path2D> paths = routeCalculator.getAllNormalizedLines(xcoords, ycoords);
    for (final Path2D path : paths) {
      drawTransformedShape(graphics, path);
    }
    // draws the Line to the Cursor on every possible screen, so that the line ends at the cursor no
    // matter what...
    final List<Point2D[]> finishingPoints =
        routeCalculator.getAllPoints(
            new Point2D.Double(xcoords[xcoords.length - 1], ycoords[ycoords.length - 1]),
            points[points.length - 1]);
    final boolean hasArrowEnoughSpace =
        points[points.length - 2].distance(points[points.length - 1]) > ARROW_LENGTH;
    for (final Point2D[] finishingPointArray : finishingPoints) {
      drawTransformedShape(
          graphics, new Line2D.Double(finishingPointArray[0], finishingPointArray[1]));
      if (hasArrowEnoughSpace) {
        drawArrow(graphics, finishingPointArray[0], finishingPointArray[1]);
      }
    }
  }

  /** This draws current moves, max moves, and fuel cost. */
  private static BufferedImage newMovementLeftImage(
      final String curMovement,
      final String maxMovement,
      final ResourceCollection movementFuelCost,
      final ResourceImageFactory resourceImageFactory) {

    // Create and configure image
    final String unitMovementLeft =
        (maxMovement == null || maxMovement.isBlank()) ? "" : "/" + maxMovement;
    final int imageWidth =
        findMovementLeftImageWidth(curMovement, unitMovementLeft, movementFuelCost);
    final BufferedImage image =
        new BufferedImage(imageWidth, MESSAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = image.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setFont(MESSAGE_FONT);
    final FontMetrics fontMetrics = graphics.getFontMetrics();

    // Draw background
    graphics.setColor(new Color(220, 220, 220));
    final int rectHeight = MESSAGE_HEIGHT - 2;
    graphics.fillRoundRect(0, 0, imageWidth - 2, rectHeight, rectHeight, rectHeight);
    graphics.setColor(Color.BLACK);
    graphics.drawRoundRect(0, 0, imageWidth - 2, rectHeight, rectHeight, rectHeight);

    // Draw current movement
    graphics.setColor(new Color(0, 0, 200));
    final boolean hasEnoughMovement = !unitMovementLeft.isEmpty();
    final int textWidthOffset = fontMetrics.stringWidth(curMovement) / 2;
    graphics.drawString(
        curMovement,
        !hasEnoughMovement ? (image.getWidth() / 2 - textWidthOffset) : MESSAGE_PADDING,
        MESSAGE_TEXT_Y);

    // If has enough movement, draw remaining movement and fuel costs
    if (hasEnoughMovement) {
      int x = MESSAGE_PADDING + fontMetrics.stringWidth(curMovement);
      graphics.setColor(new Color(0, 0, 200));
      graphics.drawString(unitMovementLeft, x, MESSAGE_TEXT_Y);
      x += fontMetrics.stringWidth(unitMovementLeft) + MESSAGE_TEXT_SPACING;
      graphics.setColor(new Color(200, 0, 0));
      for (final Resource resource : movementFuelCost.getResourcesCopy().keySet()) {
        try {
          resourceImageFactory.getIcon(resource, false).paintIcon(null, graphics, x, 2);
        } catch (final IllegalStateException e) {
          graphics.drawString(resource.getName().substring(0, 1), x, MESSAGE_TEXT_Y);
        }
        x += ResourceImageFactory.IMAGE_SIZE;
        final String quantity = "-" + movementFuelCost.getQuantity(resource);
        graphics.drawString(quantity, x, MESSAGE_TEXT_Y);
        x += fontMetrics.stringWidth(quantity) + MESSAGE_TEXT_SPACING;
      }
    }

    graphics.dispose();
    return image;
  }

  private static int findMovementLeftImageWidth(
      final String curMovement,
      final String unitMovementLeft,
      final ResourceCollection movementFuelCost) {

    // Create temp graphics to calculate necessary image width based on font sizes
    final BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D tempGraphics = tempImage.createGraphics();
    tempGraphics.setFont(MESSAGE_FONT);
    final FontMetrics fontMetrics = tempGraphics.getFontMetrics();

    // Determine widths of each element to draw
    int imageWidth = 2 * MESSAGE_PADDING;
    imageWidth += fontMetrics.stringWidth(curMovement);
    if (!unitMovementLeft.isEmpty()) {
      imageWidth += fontMetrics.stringWidth(unitMovementLeft);
      for (final Resource resource : movementFuelCost.getResourcesCopy().keySet()) {
        imageWidth += MESSAGE_TEXT_SPACING;
        imageWidth += fontMetrics.stringWidth("-" + movementFuelCost.getQuantity(resource));
        imageWidth += ResourceImageFactory.IMAGE_SIZE;
      }
    }

    tempGraphics.dispose();
    return imageWidth;
  }

  /**
   * Creates an Arrow-Shape.
   *
   * @param angle The radiant angle at which the arrow should be rotated
   * @return A transformed Arrow-Shape
   */
  private static Shape newArrowTipShape(final double angle) {
    final int arrowOffset = 1;
    final Polygon arrowPolygon = new Polygon();
    arrowPolygon.addPoint(arrowOffset - ARROW_LENGTH, ARROW_LENGTH / 2);
    arrowPolygon.addPoint(arrowOffset, 0);
    arrowPolygon.addPoint(arrowOffset - ARROW_LENGTH, ARROW_LENGTH / -2);

    final AffineTransform transform = new AffineTransform();
    transform.scale(ARROW_LENGTH, ARROW_LENGTH);
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
    final Shape arrow =
        newArrowTipShape(Math.atan2(to.getY() - from.getY(), to.getX() - from.getX()));
    final AffineTransform transform = getDrawingTransform();
    transform.translate(to.getX(), to.getY());
    graphics.fill(transform.createTransformedShape(arrow));
  }

  private AffineTransform getDrawingTransform() {
    return AffineTransform.getTranslateInstance(-mapPanel.getXOffset(), -mapPanel.getYOffset());
  }
}
