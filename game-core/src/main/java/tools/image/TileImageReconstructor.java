package tools.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import games.strategy.triplea.ui.screen.TileManager;
import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;
import tools.util.ToolLogger;

/**
 * For taking a folder of basetiles and putting them back together into an image.
 */
public class TileImageReconstructor {
  private static String baseTileLocation = null;
  private static String imageSaveLocation = null;
  private static File mapFolderLocation = null;
  private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
  private static final JTextAreaOptionPane textOptionPane = new JTextAreaOptionPane(null,
      "TileImageReconstructor Log\r\n\r\n", "", "TileImageReconstructor Log", null, 500, 300, true, 1, null);
  private static int sizeX = -1;
  private static int sizeY = -1;
  private static Map<String, List<Polygon>> polygons = new HashMap<>();

  public static void main(final String[] args) throws Exception {
    handleCommandLineArgs(args);
    JOptionPane.showMessageDialog(null,
        new JLabel("<html>"
            + "This is the TileImageReconstructor, it will reconstruct a single map image from a folder full of "
            + "basetiles. "
            + "<br>You must know the size of the map image before you begin, this is normally found in the "
            + "map.properties file. "
            + "</html>"));
    final FileSave baseTileLocationSelection = new FileSave("Where are the Tile Images?", null, mapFolderLocation);
    baseTileLocation = baseTileLocationSelection.getPathString();
    if ((mapFolderLocation == null) && (baseTileLocationSelection.getFile() != null)) {
      mapFolderLocation = baseTileLocationSelection.getFile().getParentFile();
    }
    if (baseTileLocation == null) {
      ToolLogger.info("You need to select a folder where the basetiles are for this to work");
      ToolLogger.info("Shutting down");
      System.exit(0);
      return;
    }
    final FileSave imageSaveLocationSelection = new FileSave("Save Map Image As?", null, mapFolderLocation,
        JFileChooser.FILES_ONLY, new File(mapFolderLocation, "map.png"), new FileFilter() {
          @Override
          public boolean accept(final File f) {
            if (f.isDirectory()) {
              return false;
            }
            return f.getName().endsWith(".png");
          }

          @Override
          public String getDescription() {
            return "*.png";
          }
        });
    imageSaveLocation = imageSaveLocationSelection.getPathString();
    if (imageSaveLocation == null) {
      ToolLogger.info("You need to choose a name and location for your image file for this to work");
      ToolLogger.info("Shutting down");
      System.exit(0);
      return;
    }
    final String width = JOptionPane.showInputDialog(null, "Enter the map image's full width in pixels:");
    if (width != null) {
      try {
        sizeX = Integer.parseInt(width);
      } catch (final NumberFormatException ex) {
        // ignore malformed input
      }
    }
    final String height = JOptionPane.showInputDialog(null, "Enter the map image's full height in pixels:");
    if (height != null) {
      try {
        sizeY = Integer.parseInt(height);
      } catch (final NumberFormatException ex) {
        // ignore malformed input
      }
    }
    if ((sizeX <= 0) || (sizeY <= 0)) {
      ToolLogger.info("Map dimensions must be greater than zero for this to work");
      ToolLogger.info("Shutting down");
      System.exit(0);
      return;
    }
    if (JOptionPane.showConfirmDialog(null,
        "Do not draw polgyons.txt file onto your image?\r\n(Default = 'yes' = do not draw)",
        "Do Not Also Draw Polygons?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
      try {
        ToolLogger.info("Load a polygon file");
        final String polyName = new FileOpen("Load A Polygon File", mapFolderLocation, ".txt").getPathString();
        if (polyName != null) {
          try (InputStream in = new FileInputStream(polyName)) {
            polygons = PointFileReaderWriter.readOneToManyPolygons(in);
          } catch (final IOException e) {
            ToolLogger.error("Failed to load polygons: " + polyName, e);
            System.exit(0);
          }
        }
      } catch (final Exception e) {
        ToolLogger.error("Failed to load polygons", e);
      }
    }
    createMap();
  }

  private static void createMap() {
    textOptionPane.show();
    final GraphicsConfiguration localGraphicSystem =
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    final BufferedImage mapImage = localGraphicSystem.createCompatibleImage(sizeX, sizeY, Transparency.TRANSLUCENT);
    final Graphics graphics = mapImage.getGraphics();
    for (int x = 0; ((x) * TileManager.TILE_SIZE) < sizeX; x++) {
      for (int y = 0; ((y) * TileManager.TILE_SIZE) < sizeY; y++) {
        final String tileName = x + "_" + y + ".png";
        final File tileFile = new File(baseTileLocation, tileName);
        if (!tileFile.exists()) {
          continue;
        }
        final Image tile = Toolkit.getDefaultToolkit().createImage(tileFile.getPath());
        Util.ensureImageLoaded(tile);
        final Rectangle tileBounds = new Rectangle(x * TileManager.TILE_SIZE, y * TileManager.TILE_SIZE,
            Math.min((x * TileManager.TILE_SIZE) + TileManager.TILE_SIZE, sizeX),
            Math.min((y * TileManager.TILE_SIZE) + TileManager.TILE_SIZE, sizeY));
        graphics.drawImage(tile, tileBounds.x, tileBounds.y, tileBounds.x + tileBounds.width,
            tileBounds.y + tileBounds.height, 0, 0, tileBounds.width, tileBounds.height, null);
        textOptionPane.appendNewLine("Drew " + tileName);
      }
    }
    if ((polygons != null) && !polygons.isEmpty()) {
      graphics.setColor(Color.black);
      textOptionPane.appendNewLine("Drawing Polygons");
      for (final Entry<String, List<Polygon>> entry : polygons.entrySet()) {
        for (final Polygon poly : entry.getValue()) {
          graphics.drawPolygon(poly.xpoints, poly.ypoints, poly.npoints);
        }
      }
    }
    textOptionPane.appendNewLine("Saving as " + imageSaveLocation + " ... ");
    try {
      ImageIO.write(mapImage, "png", new File(imageSaveLocation));
    } catch (final IOException e) {
      ToolLogger.error("Failed to save image: " + imageSaveLocation, e);
    }
    textOptionPane.appendNewLine("Wrote " + imageSaveLocation);
    textOptionPane.appendNewLine("\r\nAll Finished!");
    textOptionPane.countDown();
    textOptionPane.dispose();
    JOptionPane.showMessageDialog(null, new JLabel("All Finished"));
    System.exit(0);
  }

  private static String getValue(final String arg) {
    final int index = arg.indexOf('=');
    if (index == -1) {
      return "";
    }
    return arg.substring(index + 1);
  }

  private static void handleCommandLineArgs(final String[] args) {
    // arg can only be the map folder location.
    if (args.length == 1) {
      final String value;
      if (args[0].startsWith(TRIPLEA_MAP_FOLDER)) {
        value = getValue(args[0]);
      } else {
        value = args[0];
      }
      final File mapFolder = new File(value);
      if (mapFolder.exists()) {
        mapFolderLocation = mapFolder;
      } else {
        ToolLogger.info("Could not find directory: " + value);
      }
    } else if (args.length > 1) {
      ToolLogger.info("Only argument allowed is the map directory.");
    }
    // might be set by -D
    if ((mapFolderLocation == null) || (mapFolderLocation.length() < 1)) {
      final String value = System.getProperty(TRIPLEA_MAP_FOLDER);
      if ((value != null) && (value.length() > 0)) {
        final File mapFolder = new File(value);
        if (mapFolder.exists()) {
          mapFolderLocation = mapFolder;
        } else {
          ToolLogger.info("Could not find directory: " + value);
        }
      }
    }
  }
}
