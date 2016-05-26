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
   * Draws the route to the screen.
   */
  public static void drawRoute(final Graphics2D graphics, final RouteDescription routeDescription, final MapPanel view,
      final MapData mapData, final String movementLeftForCurrentUnits) {
    // set thickness and color of the future drawings
    graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    graphics.setPaint(Color.red);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    if (routeDescription == null) {
      return;
    }
    final Route route = routeDescription.getRoute();
    if (route == null) {
      return;
    }
    final int numTerritories = route.getAllTerritories().size();
    final Point[] points = getRoutePoints(routeDescription, mapData);
    final boolean tooFewTerritories = numTerritories <= 1;
    final boolean tooFewPoints = points.length <= 2;
    final int xOffset = view.getXOffset();
    final int yOffset = view.getYOffset();
    
    if (!tooFewTerritories && tooFewPoints) {
      drawMoveLength(graphics, routeDescription, points, xOffset, yOffset, view, mapData.scrollWrapX(),
          mapData.scrollWrapY(), numTerritories, movementLeftForCurrentUnits);
    }
    final double scale = view.getScale();
    final int jointsize = 10;
    if (tooFewTerritories || tooFewPoints) {
      drawLineWithTranslate(graphics, new Line2D.Float(routeDescription.getStart(), routeDescription.getEnd()), xOffset,
          yOffset, scale);
      graphics.fillOval((int) (((routeDescription.getEnd().x - xOffset) - jointsize / 2) * scale),
          (int) (((routeDescription.getEnd().y - yOffset) - jointsize / 2) * scale), jointsize, jointsize);
    } else {
      drawCurvedPath(graphics, points, view);
      drawMoveLength(graphics, routeDescription, points, xOffset, yOffset, view, mapData.scrollWrapX(),
          mapData.scrollWrapY(), numTerritories, movementLeftForCurrentUnits);
    }
    drawJoints(graphics, points, xOffset, yOffset, jointsize, scale);
    final Image cursorImage = routeDescription.getCursorImage();
    if (cursorImage != null) {
      graphics.drawImage(cursorImage,
          (int) (((routeDescription.getEnd().x - xOffset) - (cursorImage.getWidth(null) / 2)) * scale),
          (int) (((routeDescription.getEnd().y - yOffset) - (cursorImage.getHeight(null) / 2)) * scale), null);
    }
  }

  private static void drawJoints(Graphics2D graphics, Point[] points, int xOffset, int yOffset, int jointsize,
      double scale) {
    for (Point p : points) {
      graphics.fillOval((int) (((p.x - xOffset) - jointsize / 2) * scale),
          (int) (((p.y - yOffset) - jointsize / 2) * scale), jointsize, jointsize);
    }

  }

  private static double[] getIndex(Point[] points) {
    final double[] index = new double[points.length];
    index[0] = 0;
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
  }

  private static Point[] getRoutePoints(RouteDescription routeDescription, MapData mapData) {
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

  private static Tuple<double[], double[]> pointsToDoubleArrays(Point[] points) {
    double[] xResult = new double[points.length];
    double[] yResult = new double[points.length];
    for (int i = 0; i < points.length; i++) {
      xResult[i] = points[i].getX();
      yResult[i] = points[i].getY();
    }
    return Tuple.of(xResult, yResult);
  }

  private static double[] getCoords(PolynomialSplineFunction curve, double[] index) {
    final double[] coords = new double[(int) Math.round(index[index.length - 1])];
    for (int i = 0; i < coords.length; i++) {
      coords[i] = curve.value(i);
    }
    return coords;
  }

  private static void drawMoveLength(Graphics2D graphics, RouteDescription routeDescription, Point[] points,
      int xOffset, int yOffset, MapPanel view, boolean scrollWrapX, boolean scrollWrapY, int numTerritories,
      String movementLeftForCurrentUnits) {
    final double scale = view.getScale();
    final Point cursorPos = points[points.length - 1];
    final int xDir = cursorPos.x - points[numTerritories - 2].x;
    final int textXOffset = xDir > 0 ? 6 : (xDir == 0 ? 0 : -14);

    final int yDir = cursorPos.y - points[numTerritories - 2].y;
    final int textYOffset = yDir > 0 ? 18 : -24;

    final String unitMovementLeft =
        movementLeftForCurrentUnits == null || movementLeftForCurrentUnits.trim().length() == 0 ? ""
            : "    /" + movementLeftForCurrentUnits;
    final BufferedImage movementImage = new BufferedImage(72, 24, BufferedImage.TYPE_INT_ARGB);
    setupTextImage(movementImage.createGraphics(), String.valueOf(numTerritories - 1), unitMovementLeft);
    graphics.drawImage(movementImage, (int) ((cursorPos.x + textXOffset - xOffset) * scale),
        (int) ((cursorPos.y + textYOffset - yOffset) * scale), null);


    final int translateX = scrollWrapX ? view.getImageWidth() : 0;
    final int translateY = scrollWrapY ? view.getImageHeight() : 0;
    graphics.drawImage(movementImage,
        (int) ((cursorPos.x + textXOffset - xOffset - translateX) * scale),
        (int) ((cursorPos.y + textYOffset - yOffset + translateY) * scale), null);
    graphics.drawImage(movementImage,
        (int) ((cursorPos.x + textXOffset - xOffset + translateX) * scale),
        (int) ((cursorPos.y + textYOffset - yOffset - translateY) * scale), null);
  }

  private static void drawCurvedPath(Graphics2D graphics, Point[] points, MapPanel view) {
    final double[] index = getIndex(points);
    final Tuple<double[], double[]> pointArrays = pointsToDoubleArrays(points);
    final PolynomialSplineFunction xcurve = splineInterpolator.interpolate(index, pointArrays.getFirst());
    final PolynomialSplineFunction ycurve = splineInterpolator.interpolate(index, pointArrays.getSecond());
    final double[] xcoords = getCoords(xcurve, index);
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

  private static void setupTextImage(Graphics2D textG2D, String textRouteMovement, String unitMovementLeft) {
    textG2D.setColor(Color.YELLOW);
    textG2D.setFont(new Font("Dialog", Font.BOLD, 20));
    textG2D.drawString(textRouteMovement, 0, 20);
    textG2D.setColor(new Color(33, 0, 127));
    textG2D.setFont(new Font("Dialog", Font.BOLD, 16));
    textG2D.drawString(unitMovementLeft, 0, 20);
  }
}
