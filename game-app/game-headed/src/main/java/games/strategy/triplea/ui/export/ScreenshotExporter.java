package games.strategy.triplea.ui.export;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.ui.Util;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.concurrency.CompletableFutureUtils;
import org.triplea.swing.FileChooser;
import org.triplea.swing.SwingComponents;

/** Provides methods to export a screenshot for a game at a particular point in time. */
@Slf4j
public final class ScreenshotExporter {
  private final TripleAFrame frame;

  private ScreenshotExporter(final TripleAFrame frame) {
    this.frame = frame;
  }

  /**
   * Prompts the user for the file to which the screenshot will be saved and saves the screenshot
   * for the specified game at the specified history step to that file.
   *
   * @param frame The frame associated with the game screenshot to export; must not be {@code null}.
   * @param gameData The game data; must not be {@code null}.
   * @param node The history step at which the game screenshot is to be captured; must not be {@code
   *     null}.
   */
  public static void exportScreenshot(
      final TripleAFrame frame, final GameData gameData, final HistoryNode node) {
    checkNotNull(frame);
    checkNotNull(gameData);
    checkNotNull(node);

    final ScreenshotExporter exporter = new ScreenshotExporter(frame);
    exporter.promptSaveFile().ifPresent(file -> exporter.runSave(gameData, node, file));
  }

  private Optional<Path> promptSaveFile() {
    return FileChooser.builder()
        .parent(frame)
        .title("Export Screenshot")
        .fileExtension("png")
        .filenameFilter((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"))
        .build()
        .chooseFile();
  }

  private void runSave(final GameData gameData, final HistoryNode node, final Path file) {
    final CompletableFuture<?> future =
        SwingComponents.runWithProgressBar(
                frame,
                "Saving picture of the game board...",
                () -> {
                  save(gameData, node, file);
                  return null;
                })
            .whenComplete(
                (ignore, e) ->
                    SwingUtilities.invokeLater(
                        () -> {
                          if (e == null) {
                            JOptionPane.showMessageDialog(
                                frame,
                                "Saved to: " + file.toAbsolutePath(),
                                "Game Board Picture Saved",
                                JOptionPane.INFORMATION_MESSAGE);
                          } else {
                            JOptionPane.showMessageDialog(
                                frame,
                                e.getMessage(),
                                "Error Saving Game Board Picture",
                                JOptionPane.ERROR_MESSAGE);
                          }
                        }));
    CompletableFutureUtils.logExceptionWhenComplete(
        future, throwable -> log.error("Failed to save map snapshot", throwable));
  }

  private void save(final GameData gameData, final HistoryNode node, final Path file)
      throws IOException {
    // get round/step/player from history tree
    int round = 0;
    final Object[] pathFromRoot = node.getPath();
    for (final Object pathNode : pathFromRoot) {
      final HistoryNode curNode = (HistoryNode) pathNode;
      if (curNode instanceof Round) {
        round = ((Round) curNode).getRoundNo();
      }
    }
    final UiContext uiContext = frame.getUiContext();
    // print map panel to image
    final MapPanel mapPanel = frame.getMapPanel();
    final BufferedImage mapImage =
        Util.newImage(mapPanel.getImageWidth(), mapPanel.getImageHeight(), false);
    final Graphics2D mapGraphics = mapImage.createGraphics();
    mapGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    mapGraphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    try {
      mapPanel.drawMapImage(mapGraphics);
      // overlay title
      Color titleColor =
          uiContext.getMapData().getColorProperty(MapData.PROPERTY_SCREENSHOT_TITLE_COLOR);
      if (titleColor == null) {
        titleColor = Color.BLACK;
      }
      final String encodedTitleX =
          uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_X);
      final String encodedTitleY =
          uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_Y);
      final String encodedTitleSize =
          uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_FONT_SIZE);
      int titleX;
      int titleY;
      int titleSize;
      try {
        titleX = Integer.parseInt(encodedTitleX);
        titleY = Integer.parseInt(encodedTitleY);
        titleSize = Integer.parseInt(encodedTitleSize);
      } catch (final NumberFormatException nfe) {
        // choose safe defaults
        titleX = 15;
        titleY = 15;
        titleSize = 15;
      }
      mapGraphics.setFont(new Font(MapImage.FONT_FAMILY_DEFAULT, Font.BOLD, titleSize));
      mapGraphics.setColor(titleColor);
      if (uiContext.getMapData().getBooleanProperty(MapData.PROPERTY_SCREENSHOT_TITLE_ENABLED)) {
        mapGraphics.drawString(gameData.getGameName() + " Round " + round, titleX, titleY);
      }

      // save Image as .png
      ImageIO.write(mapImage, "png", file.toFile());
    } finally {
      // Clean up objects. There might be some overkill here,
      // but there were memory leaks that are fixed by some/all of these.
      mapImage.flush();
      mapGraphics.dispose();
    }
  }
}
