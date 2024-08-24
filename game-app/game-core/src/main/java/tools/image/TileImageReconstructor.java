package tools.image;

import static com.google.common.base.Preconditions.checkState;
import static games.strategy.triplea.ui.screen.TileManager.TILE_SIZE;

import games.strategy.ui.Util;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.util.PointFileReaderWriter;

/** For taking a folder of basetiles and putting them back together into an image. */
@Slf4j
public final class TileImageReconstructor {
  private Path baseTileLocation = null;
  private Path imageSaveLocation = null;
  private final JTextAreaOptionPane textOptionPane =
      new JTextAreaOptionPane(
          null,
          "TileImageReconstructor Log\r\n\r\n",
          "",
          "TileImageReconstructor Log",
          null,
          500,
          300,
          1,
          null);
  private int sizeX = -1;
  private int sizeY = -1;
  private Map<String, List<Polygon>> polygons = new HashMap<>();

  private TileImageReconstructor() {}

  /**
   * Runs the tile image reconstructor tool.
   *
   * @throws IllegalStateException If not invoked on the EDT.
   */
  public static void run() {
    checkState(SwingUtilities.isEventDispatchThread());

    new TileImageReconstructor().runInternal();
  }

  private void runInternal() {
    @Nullable Path mapFolderLocation = MapFolderLocationSystemProperty.read();
    JOptionPane.showMessageDialog(
        null,
        new JLabel(
            "<html>"
                + "This is the TileImageReconstructor, it will reconstruct a single "
                + "map image from a folder full of basetiles. "
                + "<br>You must know the size of the map image before you begin, this is "
                + "normally found in the map.properties file. "
                + "</html>"));
    final FileSave baseTileLocationSelection =
        new FileSave("Where are the Tile Images?", null, mapFolderLocation);
    baseTileLocation = baseTileLocationSelection.getFile();
    if (mapFolderLocation == null && baseTileLocationSelection.getFile() != null) {
      mapFolderLocation = baseTileLocationSelection.getFile().getParent();
    }
    if (baseTileLocation == null) {
      log.info("You need to select a folder where the basetiles are for this to work");
      log.info("Shutting down");
      return;
    }
    final FileSave imageSaveLocationSelection =
        new FileSave(
            "Save Map Image As?",
            null,
            mapFolderLocation,
            JFileChooser.FILES_ONLY,
            mapFolderLocation.resolve("map.png"),
            new FileFilter() {
              @Override
              public boolean accept(final File f) {
                return !f.isDirectory() && f.getName().endsWith(".png");
              }

              @Override
              public String getDescription() {
                return "*.png";
              }
            });
    imageSaveLocation = imageSaveLocationSelection.getFile();
    if (imageSaveLocation == null) {
      log.info("You need to choose a name and location for your image file for this to work");
      log.info("Shutting down");
      return;
    }
    final String width =
        JOptionPane.showInputDialog(null, "Enter the map image's full width in pixels:");
    if (width != null) {
      try {
        sizeX = Integer.parseInt(width);
      } catch (final NumberFormatException ex) {
        // ignore malformed input
      }
    }
    final String height =
        JOptionPane.showInputDialog(null, "Enter the map image's full height in pixels:");
    if (height != null) {
      try {
        sizeY = Integer.parseInt(height);
      } catch (final NumberFormatException ex) {
        // ignore malformed input
      }
    }
    if (sizeX <= 0 || sizeY <= 0) {
      log.info("Map dimensions must be greater than zero for this to work");
      log.info("Shutting down");
      return;
    }
    if (JOptionPane.showConfirmDialog(
            null,
            "Do not draw polygons.txt file onto your image?\r\n(Default = 'yes' = do not draw)",
            "Do Not Also Draw Polygons?",
            JOptionPane.YES_NO_OPTION)
        == JOptionPane.NO_OPTION) {
      try {
        log.info("Load a polygon file");
        final Path polyName =
            new FileOpen("Load A Polygon File", mapFolderLocation, ".txt").getFile();
        if (polyName != null) {
          try {
            polygons = PointFileReaderWriter.readOneToManyPolygons(polyName);
          } catch (final IOException e) {
            log.error("Failed to load polygons: " + polyName, e);
            return;
          }
        }
      } catch (final Exception e) {
        log.error("Failed to load polygons", e);
      }
    }
    createMap();
  }

  private void createMap() {
    textOptionPane.show();
    final GraphicsConfiguration localGraphicSystem =
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration();
    final BufferedImage mapImage =
        localGraphicSystem.createCompatibleImage(sizeX, sizeY, Transparency.TRANSLUCENT);
    final Graphics graphics = mapImage.getGraphics();
    for (int x = 0; x * TILE_SIZE < sizeX; x++) {
      for (int y = 0; y * TILE_SIZE < sizeY; y++) {
        @NonNls final String tileName = x + "_" + y + ".png";
        final Path tileFile = baseTileLocation.resolve(tileName);
        if (!Files.exists(tileFile)) {
          continue;
        }
        final Image tile = Toolkit.getDefaultToolkit().createImage(tileFile.toString());
        Util.ensureImageLoaded(tile);
        final Rectangle tileBounds =
            new Rectangle(
                x * TILE_SIZE,
                y * TILE_SIZE,
                Math.min((x * TILE_SIZE) + TILE_SIZE, sizeX),
                Math.min((y * TILE_SIZE) + TILE_SIZE, sizeY));
        graphics.drawImage(
            tile,
            tileBounds.x,
            tileBounds.y,
            tileBounds.x + tileBounds.width,
            tileBounds.y + tileBounds.height,
            0,
            0,
            tileBounds.width,
            tileBounds.height,
            null);
        textOptionPane.appendNewLine("Drew " + tileName);
      }
    }
    if (polygons != null && !polygons.isEmpty()) {
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
      ImageIO.write(mapImage, "png", imageSaveLocation.toFile());
    } catch (final IOException e) {
      log.error("Failed to save image: " + imageSaveLocation, e);
    }
    textOptionPane.appendNewLine("Wrote " + imageSaveLocation);
    textOptionPane.appendNewLine("\r\nAll Finished!");
    textOptionPane.countDown();
    textOptionPane.dispose();
    JOptionPane.showMessageDialog(null, new JLabel("All Finished"));
  }
}
