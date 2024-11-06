package tools.image;

import static com.google.common.base.Preconditions.checkState;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.util.PointFileReaderWriter;
import tools.util.ToolArguments;

/**
 * The auto-placement finder map making tool.
 *
 * <p>This tool will attempt to automatically calculate appropriate unit placement locations for
 * each territory on a given map. It will generate a {@code places.txt} file containing the unit
 * placement locations.
 */
@Slf4j
public final class AutoPlacementFinder {
  private int placeWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private int placeHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private boolean placeDimensionsSet = false;
  private double unitZoomPercent = 1;
  private int unitWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private int unitHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private Path mapFolderLocation = null;
  private final JTextAreaOptionPane textOptionPane =
      new JTextAreaOptionPane(
          null,
          "AutoPlacementFinder Log\r\n\r\n",
          "",
          "AutoPlacementFinder Log",
          null,
          500,
          300,
          1,
          null);

  private AutoPlacementFinder() {}

  /**
   * Runs the auto-placement finder tool.
   *
   * @throws IllegalStateException If not invoked on the EDT.
   */
  public static void run() {
    checkState(SwingUtilities.isEventDispatchThread());

    new AutoPlacementFinder().runInternal();
  }

  private void runInternal() {
    handleSystemProperties();
    JOptionPane.showMessageDialog(
        null,
        new JLabel(
            "<html>"
                + "This is the AutoPlacementFinder, it will create a place.txt file for you. "
                + "<br>In order to run this, you must already have created a centers.txt file and "
                + "a polygons.txt file, "
                + "<br>and you must have already created the map directory structure in its "
                + "final place."
                + "<br>Example: the map folder should have a name, with the 2 text files already "
                + "in that folder, and "
                + "<br>the folder should be located in your users\\username\\triplea\\maps\\ "
                + "directory."
                + "<br><br>The program will ask for the folder name (just the name, not the "
                + "full path)."
                + "<br>Then it will ask for unit scale (unit zoom) level [normally between "
                + "0.5 and 1.0]"
                + "<br>Then it will ask for the unit image size when not zoomed [normally 48x48]."
                + "<br><br>If you want to have less, or more, room around the edges of your units, "
                + "you can change the unit size."
                + "<br><br>When done, the program will attempt to make placements for all "
                + "territories on your map."
                + "<br>However, it doesn't do a good job with thin or small territories, or "
                + "islands, so it is a very good idea"
                + "<br>to use the PlacementPicker to check all placements and redo some of them "
                + "by hand."
                + "</html>"));
    calculate();
  }

  /** Will calculate the placements on the map automatically. */
  private void calculate() {
    // ask user where the map is
    final String mapDir =
        mapFolderLocation == null ? getMapDirectory() : mapFolderLocation.getFileName().toString();
    if (mapDir == null) {
      log.info("You need to specify a map name for this to work");
      log.info("Shutting down");
      return;
    }
    final Path file = getMapPropertiesFile(mapDir);
    if (Files.exists(file) && mapFolderLocation == null) {
      mapFolderLocation = file.getParent();
    }
    if (!placeDimensionsSet) {
      try {
        if (Files.exists(file)) {
          double scale = unitZoomPercent;
          int width = unitWidth;
          int height = unitHeight;
          boolean found = false;
          try (Scanner scanner = new Scanner(file, StandardCharsets.UTF_8.name())) {
            @NonNls final String heightProperty = MapData.PROPERTY_UNITS_HEIGHT + "=";
            @NonNls final String widthProperty = MapData.PROPERTY_UNITS_WIDTH + "=";
            @NonNls final String scaleProperty = MapData.PROPERTY_UNITS_SCALE + "=";
            while (scanner.hasNextLine()) {
              final String line = scanner.nextLine();
              if (line.contains(scaleProperty)) {
                try {
                  scale =
                      Double.parseDouble(
                          line.substring(line.indexOf(scaleProperty) + scaleProperty.length())
                              .trim());
                  found = true;
                } catch (final NumberFormatException ex) {
                  // ignore malformed input
                }
              }
              if (line.contains(widthProperty)) {
                try {
                  width =
                      Integer.parseInt(
                          line.substring(line.indexOf(widthProperty) + widthProperty.length())
                              .trim());
                  found = true;
                } catch (final NumberFormatException ex) {
                  // ignore malformed input
                }
              }
              if (line.contains(heightProperty)) {
                try {
                  height =
                      Integer.parseInt(
                          line.substring(line.indexOf(heightProperty) + heightProperty.length())
                              .trim());
                  found = true;
                } catch (final NumberFormatException ex) {
                  // ignore malformed input
                }
              }
            }
          }
          if (found) {
            final int result =
                JOptionPane.showConfirmDialog(
                    new JPanel(),
                    "A map.properties file was found in the map's folder, "
                        + "\r\n do you want to use the file to supply the info for "
                        + "the placement box size? "
                        + "\r\n Zoom = "
                        + scale
                        + ",  Width = "
                        + width
                        + ",  Height = "
                        + height
                        + ",    Result = ("
                        + ((int) (scale * width))
                        + "x"
                        + ((int) (scale * height))
                        + ")",
                    "File Suggestion",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (result == 0) {
              unitZoomPercent = scale;
              placeWidth = (int) (unitZoomPercent * width);
              placeHeight = (int) (unitZoomPercent * height);
              placeDimensionsSet = true;
            }
          }
        }
      } catch (final Exception e) {
        log.error("Failed to initialize from map properties: " + file.toAbsolutePath(), e);
      }
    }
    if (!placeDimensionsSet
        || JOptionPane.showConfirmDialog(
                new JPanel(),
                "Placement Box Size already set ("
                    + placeWidth
                    + "x"
                    + placeHeight
                    + "), "
                    + "do you wish to continue with this?\r\n"
                    + "Select Yes to continue, Select No to override and change the size.",
                "Placement Box Size",
                JOptionPane.YES_NO_OPTION)
            == 1) {
      try {
        final String result = getUnitsScale();
        try {
          unitZoomPercent = Double.parseDouble(result.toLowerCase(Locale.ROOT));
        } catch (final NumberFormatException ex) {
          // ignore malformed input
        }
        final String width =
            JOptionPane.showInputDialog(
                null,
                "Enter the unit's image width in pixels (unscaled / without zoom).\r\n(e.g. 48)");
        if (width != null) {
          try {
            placeWidth = (int) (unitZoomPercent * Integer.parseInt(width));
          } catch (final NumberFormatException ex) {
            // ignore malformed input
          }
        }
        final String height =
            JOptionPane.showInputDialog(
                null,
                "Enter the unit's image height in pixels (unscaled / without zoom).\r\n(e.g. 48)");
        if (height != null) {
          try {
            placeHeight = (int) (unitZoomPercent * Integer.parseInt(height));
          } catch (final NumberFormatException ex) {
            // ignore malformed input
          }
        }
        placeDimensionsSet = true;
      } catch (final Exception e) {
        log.error("Failed to initialize from user input", e);
      }
    }

    textOptionPane.show();
    textOptionPane.appendNewLine(
        "Place Dimensions in pixels, being used: " + placeWidth + "x" + placeHeight + "\r\n");
    textOptionPane.appendNewLine("Calculating, this may take a while...\r\n");
    final Map<String, List<Point>> placements = new HashMap<>();
    final MapData mapData = new MapData(new ResourceLoader(mapFolderLocation));
    for (final String name : mapData.getTerritories()) {
      final Set<Polygon> containedPolygons = mapData.getContainedTerritoryPolygons(name);
      final List<Point> points =
          containedPolygons.isEmpty()
              ? getPlacementsStartingAtMiddle(
                  mapData.getPolygons(name), mapData.getBoundingRect(name), mapData.getCenter(name))
              : getPlacementsStartingAtTopLeft(
                  mapData.getPolygons(name),
                  mapData.getBoundingRect(name),
                  mapData.getCenter(name),
                  containedPolygons);
      placements.put(name, points);
      textOptionPane.appendNewLine(name + ": " + points.size());
    }
    textOptionPane.appendNewLine("\r\nAll Finished!");
    textOptionPane.countDown();
    final Path fileName =
        new FileSave("Where To Save place.txt ?", "place.txt", mapFolderLocation).getFile();
    if (fileName == null) {
      textOptionPane.appendNewLine("You chose not to save, Shutting down");
      textOptionPane.dispose();
      return;
    }
    try {
      PointFileReaderWriter.writeOneToMany(fileName, placements);
      textOptionPane.appendNewLine("Data written to: " + fileName.normalize().toAbsolutePath());
    } catch (final IOException e) {
      log.error("Failed to write points file: " + fileName, e);
      textOptionPane.dispose();
      return;
    }
    textOptionPane.dispose();
  }

  /**
   * we need the exact map name as indicated in the XML game file i.e. "revised" "classic"
   * "pact_of_steel" of course, without the quotes.
   */
  private static String getMapDirectory() {
    return JOptionPane.showInputDialog(null, "Enter the map name (ie. folder name)");
  }

  private static Path getMapPropertiesFile(final String mapDir) {
    final Path file = getMapPropertiesFileForCurrentFolderStructure(mapDir);
    if (Files.exists(file)) {
      return file;
    }

    return getMapPropertiesFileForLegacyFolderStructure(mapDir);
  }

  private static Path getMapPropertiesFileForCurrentFolderStructure(final String mapDir) {
    return ClientFileSystemHelper.getUserMapsFolder()
        .resolve(mapDir)
        .resolve("map")
        .resolve("map.properties");
  }

  private static Path getMapPropertiesFileForLegacyFolderStructure(final String mapDir) {
    return ClientFileSystemHelper.getUserMapsFolder().resolve(mapDir).resolve("map.properties");
  }

  private static String getUnitsScale() {
    final String unitsScale =
        JOptionPane.showInputDialog(
            null,
            "Enter the unit's scale (zoom)."
                + "\r\n(e.g. 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5)");
    return (unitsScale != null) ? unitsScale : "1";
  }

  private List<Point> getPlacementsStartingAtMiddle(
      final Collection<Polygon> countryPolygons, final Rectangle bounding, final Point center) {
    final List<Rectangle2D> placementRectangles = new ArrayList<>();
    final List<Point> placementPoints = new ArrayList<>();
    final Rectangle2D place = new Rectangle2D.Double(center.x, center.y, placeHeight, placeWidth);
    int x = center.x - (placeHeight / 2);
    int y = center.y - (placeWidth / 2);
    int step = 1;
    for (int i = 0; i < 2 * Math.max(bounding.width, bounding.height); i++) {
      for (int j = 0; j < Math.abs(step); j++) {
        if (step > 0) {
          x++;
        } else {
          x--;
        }
        isPlacement(countryPolygons, Set.of(), placementRectangles, placementPoints, place, x, y);
      }
      for (int j = 0; j < Math.abs(step); j++) {
        if (step > 0) {
          y++;
        } else {
          y--;
        }
        isPlacement(countryPolygons, Set.of(), placementRectangles, placementPoints, place, x, y);
      }
      step = -step;
      if (step > 0) {
        step++;
      } else {
        step--;
      }
      // For Debugging
      // textOptionPane.appendNewLine("x: " + x + " y: " + y);
      if (placementPoints.size() > 50) {
        break;
      }
    }
    if (placementPoints.isEmpty()) {
      final int defaultX = center.x - (placeHeight / 2);
      final int defaultY = center.y - (placeWidth / 2);
      placementPoints.add(new Point(defaultX, defaultY));
    }
    return placementPoints;
  }

  private List<Point> getPlacementsStartingAtTopLeft(
      final Collection<Polygon> countryPolygons,
      final Rectangle bounding,
      final Point center,
      final Collection<Polygon> containedCountryPolygons) {
    final List<Rectangle2D> placementRectangles = new ArrayList<>();
    final List<Point> placementPoints = new ArrayList<>();
    final Rectangle2D place = new Rectangle2D.Double(center.x, center.y, placeHeight, placeWidth);
    for (int x = bounding.x + 1; x < bounding.width + bounding.x; x++) {
      for (int y = bounding.y + 1; y < bounding.height + bounding.y; y++) {
        isPlacement(
            countryPolygons,
            containedCountryPolygons,
            placementRectangles,
            placementPoints,
            place,
            x,
            y);
      }
      if (placementPoints.size() > 50) {
        break;
      }
    }
    if (placementPoints.isEmpty()) {
      final int defaultX = center.x - (placeHeight / 2);
      final int defaultY = center.y - (placeWidth / 2);
      placementPoints.add(new Point(defaultX, defaultY));
    }
    return placementPoints;
  }

  private void isPlacement(
      final Collection<Polygon> countryPolygons,
      final Collection<Polygon> containedCountryPolygons,
      final List<Rectangle2D> placementRectangles,
      final List<Point> placementPoints,
      final Rectangle2D place,
      final int x,
      final int y) {
    place.setFrame(x, y, placeWidth, placeHeight);
    // make sure it is not in or intersects the contained country
    if (containedIn(place, countryPolygons)
        && !intersectsOneOf(place, placementRectangles)
        && (!containedIn(place, containedCountryPolygons)
            && !intersectsOneOf(place, containedCountryPolygons))) {
      placementPoints.add(new Point((int) place.getX(), (int) place.getY()));
      final Rectangle2D newRect = new Rectangle2D.Double();
      newRect.setFrame(place);
      placementRectangles.add(newRect);
    } // if
  }

  /**
   * Function to test if the given 2D rectangle is contained in any of the given shapes in the
   * collection.
   */
  private static boolean containedIn(final Rectangle2D r, final Collection<Polygon> shapes) {
    for (final Shape item : shapes) {
      if (item.contains(r)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Function to test if the given 2D rectangle intersects any of the shapes given in the
   * collection.
   */
  private static boolean intersectsOneOf(
      final Rectangle2D r, final Collection<? extends Shape> shapes) {
    if (shapes.isEmpty()) {
      return false;
    }
    for (final Shape item : shapes) {
      if (item.intersects(r)) {
        return true;
      }
    }
    return false;
  }

  private void handleSystemProperties() {
    mapFolderLocation = MapFolderLocationSystemProperty.read();
    final String zoomString = System.getProperty(ToolArguments.UNIT_ZOOM);
    if (zoomString != null && zoomString.length() > 0) {
      try {
        unitZoomPercent = Double.parseDouble(zoomString);
        log.info("Unit Zoom Percent to use: " + unitZoomPercent);
        placeDimensionsSet = true;
      } catch (final Exception e) {
        log.error("Not a decimal percentage: " + zoomString);
      }
    }
    final String widthString = System.getProperty(ToolArguments.UNIT_WIDTH);
    if (widthString != null && widthString.length() > 0) {
      try {
        unitWidth = Integer.parseInt(widthString);
        log.info("Unit Width to use: " + unitWidth);
        placeDimensionsSet = true;
      } catch (final Exception e) {
        log.error("Not an integer: " + widthString);
      }
    }
    final String heightString = System.getProperty(ToolArguments.UNIT_HEIGHT);
    if (heightString != null && heightString.length() > 0) {
      try {
        unitHeight = Integer.parseInt(heightString);
        log.info("Unit Height to use: " + unitHeight);
        placeDimensionsSet = true;
      } catch (final Exception e) {
        log.error("Not an integer: " + heightString);
      }
    }
    if (placeDimensionsSet) {
      placeWidth = (int) (unitZoomPercent * unitWidth);
      placeHeight = (int) (unitZoomPercent * unitHeight);
      log.info("Place Dimensions to use: " + placeWidth + "x" + placeHeight);
    }
  }
}
