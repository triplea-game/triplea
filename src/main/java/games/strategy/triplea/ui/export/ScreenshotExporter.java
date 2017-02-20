package games.strategy.triplea.ui.export;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.MapPanel;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.ui.SwingComponents;
import games.strategy.ui.Util;

public final class ScreenshotExporter {
  private final TripleAFrame frame;

  private ScreenshotExporter(final TripleAFrame frame) {
    this.frame = frame;
  }

  /**
   * Prompts the user for the file to which the screenshot will be saved and saves the screenshot for the specified game
   * at the specified history step to that file.
   *
   * @param frame The frame associated with the game screenshot to export; must not be {@code null}.
   * @param gameData The game data; must not be {@code null}.
   * @param node The history step at which the game screenshot is to be captured; must not be {@code null}.
   */
  public static void exportScreenshot(final TripleAFrame frame, final GameData gameData, final HistoryNode node) {
    checkNotNull(frame);
    checkNotNull(gameData);
    checkNotNull(node);

    final ScreenshotExporter exporter = new ScreenshotExporter(frame);
    exporter.promptSaveFile().ifPresent(file -> exporter.runSave(gameData, node, file));
  }

  private Optional<File> promptSaveFile() {
    return SwingComponents.promptSaveFile(frame, "png", "Saved Map Snapshots");
  }

  private void runSave(final GameData gameData, final HistoryNode node, final File file) {
    SwingComponents.runWithProgressBar(frame, "Saving map snapshot...", () -> {
      save(gameData, node, file);
      return null;
    }).whenComplete((ignore, e) -> {
      SwingUtilities.invokeLater(() -> {
        if (e == null) {
          JOptionPane.showMessageDialog(frame, "Map Snapshot Saved", "Map Snapshot Saved",
              JOptionPane.INFORMATION_MESSAGE);
        } else {
          JOptionPane.showMessageDialog(frame, e.getMessage(), "Error Saving Map Snapshot", JOptionPane.ERROR_MESSAGE);
        }
      });
    });
  }

  private void save(final GameData gameData, final HistoryNode node, final File file) throws IOException {
    // get round/step/player from history tree
    int round = 0;
    final Object[] pathFromRoot = node.getPath();
    for (final Object pathNode : pathFromRoot) {
      final HistoryNode curNode = (HistoryNode) pathNode;
      if (curNode instanceof Round) {
        round = ((Round) curNode).getRoundNo();
      }
    }
    final IUIContext iuiContext = frame.getUIContext();
    final double scale = iuiContext.getScale();
    // print map panel to image
    final MapPanel mapPanel = frame.getMapPanel();
    final BufferedImage mapImage =
        Util.createImage((int) (scale * mapPanel.getImageWidth()), (int) (scale * mapPanel.getImageHeight()), false);
    final Graphics2D mapGraphics = mapImage.createGraphics();
    try {
      // workaround to get the whole map
      // (otherwise the map is cut if current window is not on top of map)
      final int xOffset = mapPanel.getXOffset();
      final int yOffset = mapPanel.getYOffset();
      mapPanel.setTopLeft(0, 0);
      mapPanel.drawMapImage(mapGraphics);
      mapPanel.setTopLeft(xOffset, yOffset);
      // overlay title
      Color title_color = iuiContext.getMapData().getColorProperty(MapData.PROPERTY_SCREENSHOT_TITLE_COLOR);
      if (title_color == null) {
        title_color = Color.BLACK;
      }
      final String s_title_x = iuiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_X);
      final String s_title_y = iuiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_Y);
      final String s_title_size = iuiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_FONT_SIZE);
      int title_x;
      int title_y;
      int title_size;
      try {
        title_x = (int) (Integer.parseInt(s_title_x) * scale);
        title_y = (int) (Integer.parseInt(s_title_y) * scale);
        title_size = Integer.parseInt(s_title_size);
      } catch (final NumberFormatException nfe) {
        // choose safe defaults
        title_x = (int) (15 * scale);
        title_y = (int) (15 * scale);
        title_size = 15;
      }
      // everything else should be scaled down onto map image
      final AffineTransform transform = new AffineTransform();
      transform.scale(scale, scale);
      mapGraphics.setTransform(transform);
      mapGraphics.setFont(new Font("Ariel", Font.BOLD, title_size));
      mapGraphics.setColor(title_color);
      if (iuiContext.getMapData().getBooleanProperty(MapData.PROPERTY_SCREENSHOT_TITLE_ENABLED)) {
        mapGraphics.drawString(gameData.getGameName() + " Round " + round, title_x, title_y);
      }

      // save Image as .png
      ImageIO.write(mapImage, "png", file);
    } finally {
      // Clean up objects. There might be some overkill here,
      // but there were memory leaks that are fixed by some/all of these.
      mapImage.flush();
      mapGraphics.dispose();
    }
  }
}
