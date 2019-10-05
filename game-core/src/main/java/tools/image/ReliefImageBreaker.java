package tools.image;

import static com.google.common.base.Preconditions.checkState;

import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.ui.Util;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.java.Log;
import tools.map.making.ImageIoCompletionWatcher;
import tools.util.ToolArguments;

/**
 * Utility for breaking an image into separate smaller images. User must make a new directory called
 * "newImages" and then run the utility first. To create sea zones only, he must choose "Y" at the
 * prompt. To create territories, he must choose "N" at the prompt. sea zone images directory must
 * be renamed to "seazone
 */
@Log
public final class ReliefImageBreaker {
  private String location = null;
  private final JFrame observer = new JFrame();
  private boolean seaZoneOnly;
  private MapData mapData;
  private File mapFolderLocation = null;

  private ReliefImageBreaker() {}

  /**
   * Runs the relief image breaker tool.
   *
   * @throws IllegalStateException If not invoked on the EDT.
   */
  public static void run(final String[] args) {
    checkState(SwingUtilities.isEventDispatchThread());

    try {
      new ReliefImageBreaker().runInternal(args);
    } catch (final IOException e) {
      log.log(Level.SEVERE, "failed to run relief image breaker", e);
    }
  }

  private void runInternal(final String[] args) throws IOException {
    handleCommandLineArgs(args);
    JOptionPane.showMessageDialog(
        null,
        new JLabel(
            "<html>"
                + "This is the ReliefImageBreaker, it is no longer used. "
                + "<br>It will take any image and finalized map folder, and will create cut "
                + "out images of the relief art "
                + "<br>for each territory and sea zone."
                + "<br><br>TripleA no longer uses these, and instead uses reliefTiles (use the "
                + "TileImageBreaker for that)."
                + "</html>"));
    final FileSave locationSelection =
        new FileSave("Where to save Relief Images?", null, mapFolderLocation);
    location = locationSelection.getPathString();
    if (mapFolderLocation == null && locationSelection.getFile() != null) {
      mapFolderLocation = locationSelection.getFile().getParentFile();
    }
    if (location == null) {
      log.info("You need to select a folder to save the tiles in for this to work");
      log.info("Shutting down");
      return;
    }
    createMaps();
  }

  /**
   * One of the main methods that is used to create the actual maps. Calls on various methods to get
   * user input and create the maps.
   */
  private void createMaps() throws IOException {
    // ask user to input image location
    final Image map = loadImage();
    if (map == null) {
      log.info("You need to select a map image for this to work");
      log.info("Shutting down");
      return;
    }
    // ask user whether it is sea zone only or not
    seaZoneOnly = doSeaZone();

    // ask user where the map is
    final String mapDir = getMapDirectory();
    if (mapDir == null || mapDir.isEmpty()) {
      log.info("You need to specify a map name for this to work");
      log.info("Shutting down");
      return;
    }
    try {
      mapData = new MapData(mapDir);
      // files for the map.
    } catch (final NullPointerException e) {
      log.log(Level.SEVERE, "Bad data given or missing text files, shutting down", e);
      return;
    }
    for (final String territoryName : mapData.getTerritories()) {
      final boolean seaZone = Util.isTerritoryNameIndicatingWater(territoryName);
      if (!seaZone && seaZoneOnly) {
        continue;
      }
      if (seaZone && !seaZoneOnly) {
        continue;
      }
      processImage(territoryName, map);
    }
    log.info("All Finished!");
  }

  /**
   * Asks user whether to do sea zones only or not.
   *
   * @return True to do seazones only.
   */
  private static boolean doSeaZone() {
    while (true) {
      final String answer = JOptionPane.showInputDialog(null, "Only Do Sea Zones? Enter [Y/N]");
      if (answer != null) {
        if (answer.equalsIgnoreCase("Y")) {
          return true;
        } else if (answer.equalsIgnoreCase("N")) {
          return false;
        }
      }
    }
  }

  /**
   * Asks the user to input a valid map name that will be used to form the map directory in the core
   * of TripleA in the class TerritoryData. we need the exact map name as indicated in the XML game
   * file ie."revised" "classic" "pact_of_steel" of course, without the quotes.
   *
   * @return map name entered by the user (if any, null returned if canceled)
   */
  private static String getMapDirectory() {
    return JOptionPane.showInputDialog(null, "Enter the name of the map (ie. revised)");
  }

  /**
   * Asks the user to select an image and then it loads it up into an Image object and returns it to
   * the calling class.
   *
   * @return The loaded image.
   */
  private Image loadImage() {
    log.info("Select the map");
    final String mapName =
        new FileOpen("Select The Map", mapFolderLocation, ".gif", ".png").getPathString();
    if (mapName != null) {
      final Image img = Toolkit.getDefaultToolkit().createImage(mapName);
      final MediaTracker tracker = new MediaTracker(new Panel());
      tracker.addImage(img, 1);
      try {
        tracker.waitForAll();
        return img;
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return null;
  }

  private void processImage(final String territory, final Image map) throws IOException {
    final Rectangle bounds = mapData.getBoundingRect(territory);
    final int width = bounds.width;
    final int height = bounds.height;
    final BufferedImage alphaChannelImage = Util.newImage(bounds.width, bounds.height, true);
    for (final Polygon polygon : mapData.getPolygons(territory)) {
      alphaChannelImage
          .getGraphics()
          .fillPolygon(Util.translatePolygon(polygon, -bounds.x, -bounds.y));
    }
    final GraphicsConfiguration localGraphicSystem =
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration();
    final BufferedImage relief =
        localGraphicSystem.createCompatibleImage(
            width, height, seaZoneOnly ? Transparency.BITMASK : Transparency.TRANSLUCENT);
    relief
        .getGraphics()
        .drawImage(
            map,
            0,
            0,
            width,
            height,
            bounds.x,
            bounds.y,
            bounds.x + width,
            bounds.y + height,
            observer);
    blankOutline(alphaChannelImage, relief);
    String outFileName = location + File.separator + territory;
    if (!seaZoneOnly) {
      outFileName += "_relief.png";
    } else {
      outFileName += ".png";
    }
    ImageIO.write(relief, "png", new File(outFileName));
    log.info("wrote " + outFileName);
  }

  /** Sets the alpha channel to the same as that of the base image. */
  private static void blankOutline(final Image alphaChannelImage, final BufferedImage relief) {
    final Graphics2D gc = (Graphics2D) relief.getGraphics();

    final Composite prevComposite = gc.getComposite();
    gc.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));
    /*
     * draw the image, and check for the possibility it doesn't complete now
     */
    final ImageIoCompletionWatcher watcher = new ImageIoCompletionWatcher();
    final boolean drawComplete = gc.drawImage(alphaChannelImage, 0, 0, watcher);
    // use the watcher to for the draw to finish
    if (!drawComplete) {
      watcher.waitForCompletion();
    }
    // cleanup
    gc.setComposite(prevComposite);
  }

  private static String getValue(final String arg) {
    final int index = arg.indexOf('=');
    if (index == -1) {
      return "";
    }
    return arg.substring(index + 1);
  }

  private void handleCommandLineArgs(final String[] args) {
    // arg can only be the map folder location.
    if (args.length == 1) {
      final String value;
      if (args[0].startsWith(ToolArguments.MAP_FOLDER)) {
        value = getValue(args[0]);
      } else {
        value = args[0];
      }
      final File mapFolder = new File(value);
      if (mapFolder.exists()) {
        mapFolderLocation = mapFolder;
      } else {
        log.info("Could not find directory: " + value);
      }
    } else if (args.length > 1) {
      log.info("Only argument allowed is the map directory.");
    }
    // might be set by -D
    if (mapFolderLocation == null || mapFolderLocation.length() < 1) {
      final String value = System.getProperty(ToolArguments.MAP_FOLDER);
      if (value != null && value.length() > 0) {
        final File mapFolder = new File(value);
        if (mapFolder.exists()) {
          mapFolderLocation = mapFolder;
        } else {
          log.info("Could not find directory: " + value);
        }
      }
    }
  }
}
