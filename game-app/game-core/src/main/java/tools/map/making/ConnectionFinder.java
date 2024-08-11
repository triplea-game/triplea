package tools.map.making;

import static com.google.common.base.Preconditions.checkState;

import java.awt.Dimension;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.AlphanumComparator;
import org.triplea.util.PointFileReaderWriter;
import tools.image.FileOpen;
import tools.image.FileSave;
import tools.image.MapFolderLocationSystemProperty;
import tools.util.ToolsUtil;

/**
 * Utility to find connections between polygons Not pretty, meant only for one time use. Inputs - a
 * polygons.txt file Outputs - a list of connections between the Polygons
 */
@Slf4j
public final class ConnectionFinder {
  @NonNls private static final String LINE_THICKNESS = "triplea.map.lineThickness";
  @NonNls private static final String SCALE_PIXELS = "triplea.map.scalePixels";
  @NonNls private static final String MIN_OVERLAP = "triplea.map.minOverlap";

  private Path mapFolderLocation = null;
  private boolean dimensionsSet = false;
  private StringBuilder territoryDefinitions = null;
  // how many pixels should each area become bigger in both x and y axis to see which area it
  // overlaps?
  // default 8, or if LINE_THICKNESS if given 4x linethickness
  private int scalePixels = 8;
  // how many pixels should the boundingbox of the overlapping area have for it to be considered a
  // valid connection?
  // default 32, or if LINE_THICKNESS is given 16 x linethickness
  private double minOverlap = 32.0;

  /**
   * Runs the connection finder tool.
   *
   * @throws IllegalStateException If not invoked on the EDT.
   */
  public static void run() {
    checkState(SwingUtilities.isEventDispatchThread());

    new ConnectionFinder().runInternal();
  }

  private void runInternal() {
    handleSystemProperties();
    JOptionPane.showMessageDialog(
        null,
        new JLabel(
            "<html>"
                + "This is the ConnectionFinder. "
                + "<br>It will create a file containing the connections between territories, "
                + "and optionally the territory definitions as well. "
                + "<br>Copy and paste everything from this file into your game xml file "
                + "(the 'map' section). "
                + "<br>The connections file can and Should Be Deleted when finished, because "
                + "it is Not Needed and not read by the engine. "
                + "</html>"));
    log.info("Select polygons.txt");
    Path polyFile = null;
    if (mapFolderLocation != null && Files.exists(mapFolderLocation)) {
      polyFile = mapFolderLocation.resolve("polygons.txt");
    }
    if (polyFile == null
        || !Files.exists(polyFile)
        || JOptionPane.showConfirmDialog(
                null,
                "A polygons.txt file was found in the map's folder, do you want to use it?",
                "File Suggestion",
                JOptionPane.YES_NO_CANCEL_OPTION)
            != 0) {
      polyFile = new FileOpen("Select The polygons.txt file", mapFolderLocation, ".txt").getFile();
    }
    if (polyFile == null || !Files.exists(polyFile)) {
      log.info("No polygons.txt Selected. Shutting down.");
      return;
    }
    if (mapFolderLocation == null) {
      mapFolderLocation = polyFile.getParent();
    }
    final Map<String, List<Area>> territoryAreas = new HashMap<>();
    final Map<String, List<Polygon>> mapOfPolygons;
    try {
      mapOfPolygons = PointFileReaderWriter.readOneToManyPolygons(polyFile);
      for (final Entry<String, List<Polygon>> entry : mapOfPolygons.entrySet()) {
        territoryAreas.put(
            entry.getKey(), entry.getValue().stream().map(Area::new).collect(Collectors.toList()));
      }
    } catch (final IOException e) {
      log.error("Failed to load polygons: " + polyFile.toAbsolutePath(), e);
      return;
    }
    if (!dimensionsSet) {
      final String lineWidth =
          JOptionPane.showInputDialog(
              null,
              "Enter the width of territory border lines on your map? \r\n(eg: 1, or 2, etc.)");
      try {
        final int lineThickness = Integer.parseInt(lineWidth);
        scalePixels = lineThickness * 4;
        minOverlap = scalePixels * 4;
        dimensionsSet = true;
      } catch (final NumberFormatException ex) {
        // ignore malformed input
      }
    }
    if (JOptionPane.showConfirmDialog(
            null,
            "Scale set to "
                + scalePixels
                + " pixels larger, and minimum overlap set to "
                + minOverlap
                + " pixels. \r\n"
                + "Do you wish to continue with this? \r\n"
                + "Select Yes to continue, Select No to override and change the size.",
            "Scale and Overlap Size",
            JOptionPane.YES_NO_OPTION)
        == 1) {
      final String scale =
          JOptionPane.showInputDialog(
              null,
              "Enter the number of pixels larger each territory should become? \r\n"
                  + "(Normally 4x bigger than the border line width. eg: 4, or 8, etc)");
      try {
        scalePixels = Integer.parseInt(scale);
      } catch (final NumberFormatException ex) {
        // ignore malformed input
      }
      final String overlap =
          JOptionPane.showInputDialog(
              null,
              "Enter the minimum number of overlapping pixels for a connection? \r\n"
                  + "(Normally 16x bigger than the border line width. eg: 16, or 32, etc.)");
      try {
        minOverlap = Integer.parseInt(overlap);
      } catch (final NumberFormatException ex) {
        // ignore malformed input
      }
    }
    log.info("Now Scanning for Connections");
    // sort so that they are in alphabetic order (makes xml's prettier and easier to update in
    // future)
    final List<String> allTerritories = new ArrayList<>(mapOfPolygons.keySet());
    allTerritories.sort(new AlphanumComparator());
    final List<String> allAreas = new ArrayList<>(territoryAreas.keySet());
    allAreas.sort(new AlphanumComparator());
    final Map<String, Collection<String>> connections = new HashMap<>();
    for (final String territory : allTerritories) {
      final Set<String> thisTerritoryConnections = new LinkedHashSet<>();
      final List<Polygon> currentPolygons = mapOfPolygons.get(territory);
      for (final Polygon currentPolygon : currentPolygons) {
        final Shape scaledShape = scale(currentPolygon, scalePixels);
        for (final String otherTerritory : allAreas) {
          if (otherTerritory.equals(territory)) {
            continue;
          }
          if (thisTerritoryConnections.contains(otherTerritory)) {
            continue;
          }
          if (connections.get(otherTerritory) != null
              && connections.get(otherTerritory).contains(territory)) {
            continue;
          }
          for (final Area otherArea : territoryAreas.get(otherTerritory)) {
            final Area testArea = new Area(scaledShape);
            testArea.intersect(otherArea);
            if (!testArea.isEmpty() && sizeOfArea(testArea) > minOverlap) {
              thisTerritoryConnections.add(otherTerritory);
            }
          }
        }
        connections.put(territory, thisTerritoryConnections);
      }
    }
    if (JOptionPane.showConfirmDialog(
            null,
            "Do you also want to create the Territory Definitions?",
            "Territory Definitions",
            JOptionPane.YES_NO_CANCEL_OPTION)
        == 0) {
      final String waterString =
          JOptionPane.showInputDialog(
              null,
              "Enter a string or regex that determines if the territory is Water? \r\n(e.g.: "
                  + ToolsUtil.TERRITORY_SEA_ZONE_INFIX
                  + ")",
              ToolsUtil.TERRITORY_SEA_ZONE_INFIX);
      territoryDefinitions = doTerritoryDefinitions(allTerritories, waterString);
    }
    try {
      final Path fileName =
          new FileSave(
                  "Where To Save connections.txt ? (cancel to print to console)",
                  "connections.txt",
                  mapFolderLocation)
              .getFile();
      final StringBuilder connectionsString = convertToXml(connections);
      if (fileName == null) {
        if (territoryDefinitions != null) {
          log.info(territoryDefinitions.toString());
        }
        log.info(connectionsString.toString());
      } else {
        try (OutputStream out = Files.newOutputStream(fileName)) {
          if (territoryDefinitions != null) {
            out.write(String.valueOf(territoryDefinitions).getBytes(StandardCharsets.UTF_8));
          }
          out.write(String.valueOf(connectionsString).getBytes(StandardCharsets.UTF_8));
        }
        log.info("Data written to :" + fileName.normalize().toAbsolutePath());
      }
    } catch (final Exception e) {
      log.error("Failed to write connections", e);
    }
  }

  /**
   * Creates the xml territory definitions.
   *
   * @param waterString a substring contained in a TerritoryName to define a Sea Zone or a regex
   *     expression that indicates that a territory is water
   * @return StringBuilder containing XML representing these connections
   */
  private static StringBuilder doTerritoryDefinitions(
      final List<String> allTerritoryNames, final String waterString) {
    // sort for pretty xml's
    allTerritoryNames.sort(new AlphanumComparator());
    final StringBuilder output = new StringBuilder();
    output.append("<!-- Territory Definitions -->\r\n");
    final Pattern waterPattern = Pattern.compile(waterString);
    for (final String t : allTerritoryNames) {
      final Matcher matcher = waterPattern.matcher(t);
      if (matcher.find()) {
        // <territory name="sea zone 1" water="true"/>
        output.append("<territory name=\"").append(t).append("\" water=\"true\"/>\r\n");
      } else {
        // <territory name="neutral territory 2"/>
        output.append("<territory name=\"").append(t).append("\"/>\r\n");
      }
    }
    output.append("\r\n");
    return output;
  }

  /**
   * Converts a map of connections to XML formatted text with the connections.
   *
   * @param connections a map of connections between Territories
   * @return a StringBuilder containing XML representing these connections
   */
  private static StringBuilder convertToXml(final Map<String, Collection<String>> connections) {
    final StringBuilder output = new StringBuilder();
    output.append("<!-- Territory Connections -->\r\n");
    // sort for pretty xml's
    final List<String> allTerritories = new ArrayList<>(connections.keySet());
    allTerritories.sort(new AlphanumComparator());
    for (final String t1 : allTerritories) {
      for (final String t2 : connections.get(t1)) {
        output
            .append("<connection t1=\"")
            .append(t1)
            .append("\" t2=\"")
            .append(t2)
            .append("\"/>\r\n");
      }
    }
    return output;
  }

  /**
   * Returns the size of the area of the bounding box of the polygon.
   *
   * @param area the area of which the boundingbox size is measured
   * @return the size of the area of the boundingbox of this area
   */
  private static double sizeOfArea(final Area area) {
    final Dimension d = area.getBounds().getSize();
    return d.getHeight() * d.getWidth();
  }

  /**
   * from: eu.hansolo.steelseries.tools.Scaler.java Returns a double that represents the area of the
   * given point array of a polygon
   *
   * @return a double that represents the area of the given point array of a polygon
   */
  private static double calcSignedPolygonArea(final Point2D[] pointArray) {
    final int length = pointArray.length;
    double area = 0;
    for (int i = 0; i < length; i++) {
      final int j = (i + 1) % length;
      area += pointArray[i].getX() * pointArray[j].getY();
      area -= pointArray[i].getY() * pointArray[j].getX();
    }
    area /= 2.0;
    return (area);
  }

  /**
   * from: eu.hansolo.steelseries.tools.Scaler.java Returns a Point2D object that represents the
   * center of mass of the given point array which represents a polygon.
   *
   * @return a Point2D object that represents the center of mass of the given point array
   */
  private static Point2D calcCenterOfMass(final Point2D[] pointArray) {
    final int length = pointArray.length;
    double cx = 0;
    double cy = 0;
    double area = calcSignedPolygonArea(pointArray);
    final Point2D centroid = new Point2D.Double();
    for (int i = 0; i < length; i++) {
      final int j = (i + 1) % length;
      final double factor =
          (pointArray[i].getX() * pointArray[j].getY()
              - pointArray[j].getX() * pointArray[i].getY());
      cx += (pointArray[i].getX() + pointArray[j].getX()) * factor;
      cy += (pointArray[i].getY() + pointArray[j].getY()) * factor;
    }
    area *= 6.0f;
    final double factor = 1 / area;
    cx *= factor;
    cy *= factor;
    centroid.setLocation(cx, cy);
    return centroid;
  }

  /**
   * from: eu.hansolo.steelseries.tools.Scaler.java Returns a Point2D object that represents the
   * center of mass of the given shape.
   *
   * @return a Point2D object that represents the center of mass of the given shape
   */
  private static Point2D getCentroid(final Shape currentShape) {
    final List<Point2D> pointList = new ArrayList<>(32);
    final PathIterator pathIterator = currentShape.getPathIterator(null);
    int lastMoveToIndex = -1;
    while (!pathIterator.isDone()) {
      final double[] coordinates = new double[6];
      final int segmentType = pathIterator.currentSegment(coordinates);
      switch (segmentType) {
        case PathIterator.SEG_MOVETO:
          pointList.add(new Point2D.Double(coordinates[0], coordinates[1]));
          lastMoveToIndex++;
          break;
        case PathIterator.SEG_LINETO:
          pointList.add(new Point2D.Double(coordinates[0], coordinates[1]));
          break;
        case PathIterator.SEG_QUADTO:
          pointList.add(new Point2D.Double(coordinates[0], coordinates[1]));
          pointList.add(new Point2D.Double(coordinates[2], coordinates[3]));
          break;
        case PathIterator.SEG_CUBICTO:
          pointList.add(new Point2D.Double(coordinates[0], coordinates[1]));
          pointList.add(new Point2D.Double(coordinates[2], coordinates[3]));
          pointList.add(new Point2D.Double(coordinates[4], coordinates[5]));
          break;
        case PathIterator.SEG_CLOSE:
          if (lastMoveToIndex >= 0) {
            pointList.add(pointList.get(lastMoveToIndex));
          }
          break;
        default:
          throw new AssertionError("unknown path iterator segment type: " + segmentType);
      }
      pathIterator.next();
    }
    final Point2D[] pointArray = new Point2D[pointList.size()];
    pointList.toArray(pointArray);
    return (calcCenterOfMass(pointArray));
  }

  private static Shape scale(final Shape currentShape, final int pixels) {
    final Dimension d = currentShape.getBounds().getSize();
    final double scaleFactorX = 1.0 + (1 / ((double) d.width)) * pixels;
    final double scaleFactorY = 1.0 + (1 / ((double) d.height)) * pixels;
    return scale(currentShape, scaleFactorX, scaleFactorY);
  }

  /**
   * from: eu.hansolo.steelseries.tools.Scaler.java Returns a scaled version of the given shape,
   * calculated by the given scale factor. The scaling will be calculated around the centroid of the
   * shape.
   *
   * @param sx how much to scale on the x-axis
   * @param sy how much to scale on the y-axis
   * @return a scaled version of the given shape, calculated around the centroid by the given scale
   *     factors.
   */
  private static Shape scale(final Shape currentPolygon, final double sx, final double sy) {
    final Point2D centroid = getCentroid(currentPolygon);
    final AffineTransform transform =
        AffineTransform.getTranslateInstance(
            (1.0 - sx) * centroid.getX(), (1.0 - sy) * centroid.getY());
    transform.scale(sx, sy);
    return transform.createTransformedShape(currentPolygon);
  }

  private void handleSystemProperties() {
    mapFolderLocation = MapFolderLocationSystemProperty.read();
    String value = System.getProperty(LINE_THICKNESS);
    if (value != null && value.length() > 0) {
      final int lineThickness = Integer.parseInt(value);
      scalePixels = lineThickness * 4;
      minOverlap = scalePixels * 4.0;
      dimensionsSet = true;
    }
    value = System.getProperty(MIN_OVERLAP);
    if (value != null && value.length() > 0) {
      minOverlap = Integer.parseInt(value);
    }
    value = System.getProperty(SCALE_PIXELS);
    if (value != null && value.length() > 0) {
      scalePixels = Integer.parseInt(value);
    }
  }
}
