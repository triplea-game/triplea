package tools.map.making.ui.runnable;

import static games.strategy.triplea.ui.screen.TileManager.TILE_SIZE;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import lombok.extern.slf4j.Slf4j;
import tools.util.FileOpen;
import tools.util.FileSave;
import tools.util.ToolArguments;

/**
 * Utility for breaking an image into separate smaller images. User must make a new directory called
 * "newImages" and then run the utility first. To create sea zones only, he must choose "Y" at the
 * prompt. To create territories, he must choose "N" at the prompt. sea zone images directory must
 * be renamed to "seazone
 */
@Slf4j
public final class TileImageBreakerTask extends ToolRunnableTask {
  private Path location = null;
  private final JFrame observer = new JFrame();
  private Path mapFolderLocation = null;
  private final JTextAreaOptionPane textOptionPane =
      new JTextAreaOptionPane(
          null,
          "TileImageBreakerTask Log\r\n\r\n",
          "",
          "TileImageBreakerTask Log",
          null,
          500,
          300,
          1,
          null);

  TileImageBreakerTask() {}

  public static void run() {
    runTask(TileImageBreakerTask.class);
  }

  @Override
  protected void runInternal() throws IOException {
    ToolArguments.ifMapFolder(mapFolderProperty -> mapFolderLocation = mapFolderProperty);
    JOptionPane.showMessageDialog(
        null,
        new JLabel(
            "<html>"
                + "This is the TileImageBreakerTask, it will create the map image tiles file for you. "
                + "<br>It will take any image, and break it up into 256x256 pixel squares, "
                + "and put them all in a folder. "
                + "<br>You can use this to create the base tiles (background) as well as the "
                + "relief tiles (art relief)."
                + "<br>For the base image (the one used to make centers.txt, etc), please "
                + "save it to a folder called baseTiles"
                + "<br>For the relief image, please save it to a folder called reliefTiles"
                + "</html>"));
    final FileSave locationSelection =
        new FileSave("Where to save Tile Images?", null, mapFolderLocation);
    location = locationSelection.getFile();
    if (mapFolderLocation == null && locationSelection.getFile() != null) {
      mapFolderLocation = locationSelection.getFile().getParent();
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

    textOptionPane.show();
    for (int x = 0; x * TILE_SIZE < map.getWidth(null); x++) {
      for (int y = 0; y * TILE_SIZE < map.getHeight(null); y++) {
        final Rectangle bounds = new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        final GraphicsConfiguration localGraphicSystem =
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
        final BufferedImage relief =
            localGraphicSystem.createCompatibleImage(
                TILE_SIZE, TILE_SIZE, Transparency.TRANSLUCENT);
        relief
            .getGraphics()
            .drawImage(
                map,
                0,
                0,
                TILE_SIZE,
                TILE_SIZE,
                bounds.x,
                bounds.y,
                bounds.x + TILE_SIZE,
                bounds.y + TILE_SIZE,
                observer);

        final Path outFile = location.resolve(x + "_" + y + ".png");

        ImageIO.write(relief, "png", outFile.toFile());
        textOptionPane.appendNewLine("wrote " + outFile);
      }
    }
    textOptionPane.appendNewLine("\r\nAll Finished!");
    textOptionPane.countDown();
    textOptionPane.dispose();
    JOptionPane.showMessageDialog(null, new JLabel("All Finished"));
  }

  /**
   * Asks the user to select an image and then it loads it up into an Image object and returns it to
   * the calling class.
   *
   * @return The loaded image.
   */
  private @Nullable Image loadImage() {
    log.info("Select the map");
    final Path mapName =
        new FileOpen("Select The Map", mapFolderLocation, ".gif", ".png").getFile();
    if (mapName != null) {
      final Image img = Toolkit.getDefaultToolkit().createImage(mapName.toString());
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
}
