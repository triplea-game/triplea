package tools.image;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import games.strategy.triplea.ui.screen.TileManager;
import tools.util.ToolLogger;

/**
 * Utility for breaking an image into seperate smaller images.
 * User must make a new directory called "newImages" and then run the utility
 * first.
 * To create sea zones only, he must choose "Y" at the prompt. To create
 * territories, he must choose "N" at the prompt.
 * sea zone images directory must be renamed to "seazone
 */
public class TileImageBreaker {
  private static String location = null;
  private static final JFrame observer = new JFrame();
  private static File mapFolderLocation = null;
  private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
  private static final JTextAreaOptionPane textOptionPane = new JTextAreaOptionPane(null,
      "TileImageBreaker Log\r\n\r\n", "", "TileImageBreaker Log", null, 500, 300, true, 1, null);

  /**
   * main(java.lang.String[] args)
   * Main program begins here. Creates a new instance of ReliefImageBreaker
   * and calls createMaps() method to start the computations.
   *
   * @param args The command line parameters.
   * @exception java.lang.Exception
   *            throws
   */
  public static void main(final String[] args) throws Exception {
    handleCommandLineArgs(args);
    JOptionPane.showMessageDialog(null,
        new JLabel("<html>" + "This is the TileImageBreaker, it will create the map image tiles file for you. "
            + "<br>It will take any image, and break it up into 256x256 pixel squares, and put them all in a folder. "
            + "<br>You can use this to create the base tiles (background) as well as the relief tiles (art relief)."
            + "<br>For the base image (the one used to make centers.txt, etc), please save it to a folder called "
            + "baseTiles"
            + "<br>For the relief image, please save it to a folder called reliefTiles" + "</html>"));
    final FileSave locationSelection = new FileSave("Where to save Tile Images?", null, mapFolderLocation);
    location = locationSelection.getPathString();
    if ((mapFolderLocation == null) && (locationSelection.getFile() != null)) {
      mapFolderLocation = locationSelection.getFile().getParentFile();
    }
    if (location == null) {
      ToolLogger.info("You need to select a folder to save the tiles in for this to work");
      ToolLogger.info("Shutting down");
      System.exit(0);
      return;
    }
    new TileImageBreaker().createMaps();
  }

  /**
   * createMaps()
   * One of the main methods that is used to create the actual maps. Calls on
   * various methods to get user input and create the maps.
   *
   * @exception java.io.IOException
   *            throws
   */
  public void createMaps() throws IOException {
    // ask user to input image location
    final Image map = loadImage();
    if (map == null) {
      ToolLogger.info("You need to select a map image for this to work");
      ToolLogger.info("Shutting down");
      System.exit(0);
      return;
    }


    textOptionPane.show();
    for (int x = 0; ((x) * TileManager.TILE_SIZE) < map.getWidth(null); x++) {
      for (int y = 0; ((y) * TileManager.TILE_SIZE) < map.getHeight(null); y++) {
        final Rectangle bounds = new Rectangle(x * TileManager.TILE_SIZE, y * TileManager.TILE_SIZE,
            TileManager.TILE_SIZE, TileManager.TILE_SIZE);
        final GraphicsConfiguration localGraphicSystem =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        final BufferedImage relief = localGraphicSystem.createCompatibleImage(TileManager.TILE_SIZE,
            TileManager.TILE_SIZE, Transparency.TRANSLUCENT);
        relief.getGraphics().drawImage(map, 0, 0, TileManager.TILE_SIZE, TileManager.TILE_SIZE, bounds.x, bounds.y,
            bounds.x + TileManager.TILE_SIZE, bounds.y + TileManager.TILE_SIZE, observer);

        final String outFileName = location + File.separator + x + "_" + y + ".png";

        ImageIO.write(relief, "png", new File(outFileName));
        textOptionPane.appendNewLine("wrote " + outFileName);
      }
    }
    textOptionPane.appendNewLine("\r\nAll Finished!");
    textOptionPane.countDown();
    textOptionPane.dispose();
    JOptionPane.showMessageDialog(null, new JLabel("All Finished"));
    System.exit(0);
  }


  /**
   * java.awt.Image loadImage()
   * Asks the user to select an image and then it loads it up into an Image
   * object and returns it to the calling class.
   *
   * @return java.awt.Image img the loaded image
   */
  private static Image loadImage() {
    ToolLogger.info("Select the map");
    final String mapName = new FileOpen("Select The Map", mapFolderLocation, ".gif", ".png").getPathString();
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
