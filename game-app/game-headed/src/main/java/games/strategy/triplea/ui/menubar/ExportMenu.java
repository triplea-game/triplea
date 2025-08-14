package games.strategy.triplea.ui.menubar;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.export.GameDataExporter;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.history.History;
import games.strategy.engine.history.HistoryNode;
import games.strategy.triplea.EngineImageLoader;
import games.strategy.triplea.printgenerator.SetupFrame;
import games.strategy.triplea.printgenerator.StatsInfo;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.export.ScreenshotExporter;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.triplea.ui.menubar.help.UnitStatsTable;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import lombok.extern.slf4j.Slf4j;
import org.triplea.map.data.elements.Game;
import org.triplea.map.xml.writer.GameXmlWriter;
import org.triplea.swing.FileChooser;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.util.FileNameUtils;

@Slf4j
final class ExportMenu extends JMenu {
  private static final long serialVersionUID = 8416990293444575737L;

  private final TripleAFrame frame;
  private final GameData gameData;
  private final UiContext uiContext;
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");

  ExportMenu(final TripleAFrame frame) {
    super("Export");

    this.frame = frame;
    gameData = frame.getGame().getData();
    uiContext = frame.getUiContext();

    setMnemonic(KeyEvent.VK_E);

    add(createExportXmlMenu());
    add(createExportStatsMenu());
    add(createExportStatsFullMenu());
    add(createExportSetupChartsMenu());
    add(createExportUnitStatsMenu());
    add(createSaveScreenshotMenu());
  }

  // TODO: create a second menu option for parsing current attachments
  private JMenuItem createExportXmlMenu() {
    return new JMenuItemBuilder("Export game.xml File (Beta)", KeyCode.X)
        .actionListener(this::exportXmlFile)
        .build();
  }

  private void exportXmlFile() {
    // The existing XML file is needed for attachment ordering data.
    final Path gameXmlPath = gameData.getGameXmlPath(uiContext.getMapLocation()).orElse(null);
    if (gameXmlPath == null) {
      JOptionPane.showMessageDialog(frame, "Error: Existing XML file not found.");
      return;
    }
    final String defaultFileName =
        FileNameUtils.removeIllegalCharacters(
                String.format(
                    "xml_%s_%s_round_%d",
                    dateTimeFormatter.format(LocalDateTime.now(ZoneId.systemDefault())),
                    gameData.getGameName(),
                    gameData.getCurrentRound()))
            + ".xml";

    Optional<Path> chosenFilePath =
        FileChooser.chooseExportFileWithDefaultName(frame, defaultFileName);
    chosenFilePath.ifPresent(
        path -> {
          try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
            final Game xmlGameModel = GameDataExporter.convertToXmlModel(gameData, gameXmlPath);
            GameXmlWriter.exportXml(xmlGameModel, path);
          }
        });
  }

  private JMenuItem createSaveScreenshotMenu() {
    return new JMenuItemBuilder("Export Gameboard Picture", KeyCode.E)
        .actionListener(this::saveScreenshot)
        .build();
  }

  private void saveScreenshot() {
    // get current history node. if we are in history view, get the selected node.
    final HistoryPanel historyPanel = frame.getHistoryPanel();
    final HistoryNode curNode;
    if (historyPanel == null) {
      curNode = gameData.getHistory().getLastNode();
    } else {
      curNode = historyPanel.getCurrentNode();
    }
    ScreenshotExporter.exportScreenshot(frame, gameData, curNode);
  }

  private JMenuItem createExportStatsFullMenu() {
    return new JMenuItemBuilder("Export Full Game Stats", KeyCode.F)
        .actionListener(() -> createAndSaveStats(true))
        .build();
  }

  private JMenuItem createExportStatsMenu() {
    return new JMenuItemBuilder("Export Short Game Stats", KeyCode.S)
        .actionListener(() -> createAndSaveStats(false))
        .build();
  }

  private void createAndSaveStats(final boolean showPhaseStats) {
    final String defaultFileName =
        FileNameUtils.removeIllegalCharacters(
                String.format(
                    "stats_%s_%s_round_%s_%s",
                    dateTimeFormatter.format(LocalDateTime.now(ZoneId.systemDefault())),
                    gameData.getGameName(),
                    gameData.getCurrentRound(),
                    showPhaseStats ? "full" : "short"))
            + ".csv";
    final CompletableFuture<GameData> cloneCompletableFuture = getGameDataCloneWithHistory();
    Optional<Path> chosenFilePath =
        FileChooser.chooseExportFileWithDefaultName(frame, defaultFileName);
    chosenFilePath.ifPresent(
        path -> StatsInfo.export(path, uiContext, showPhaseStats, cloneCompletableFuture.join()));
  }

  private JMenuItem createExportUnitStatsMenu() {
    return new JMenuItemBuilder("Export Unit Charts", KeyCode.U)
        .actionListener(this::exportUnitCharts)
        .build();
  }

  private void exportUnitCharts() {
    final String defaultFileName =
        FileNameUtils.removeIllegalCharacters(gameData.getGameName()) + "_unit_stats.html";
    Optional<Path> chosenFilePath =
        FileChooser.chooseExportFileWithDefaultName(frame, defaultFileName);
    chosenFilePath.ifPresent(
        path -> {
          try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(
                UnitStatsTable.getUnitStatsTable(gameData, uiContext)
                    .replaceAll("</?p>|</tr>", "$0\r\n")
                    .replaceAll("(?i)<img[^>]+/>", ""));
          } catch (final IOException e1) {
            log.error("Failed to write unit stats: {}", path, e1);
          }
        });
  }

  private JMenuItem createExportSetupChartsMenu() {
    return new JMenuItemBuilder("Export Setup Charts", KeyCode.C)
        .actionListener(this::exportSetupCharts)
        .build();
  }

  private void exportSetupCharts() {
    SwingAction.invokeNowOrLater(
        () ->
            JFrameBuilder.builder()
                .title("Export Setup Charts")
                .locateRelativeTo(frame)
                .iconImage(EngineImageLoader.loadFrameIcon())
                .pack()
                .disposeOnClose()
                .alwaysOnTop()
                .add(
                    exportSetupChartsFrame ->
                        new JPanelBuilder()
                            .add(
                                new SetupFrame(
                                    exportSetupChartsFrame, getGameDataCloneWithHistory()))
                            .build())
                .visible(true)
                .build());
  }

  /**
   * Provides a game data copy with its history by triggering an asynchronous process for the
   * copying task.
   *
   * @return {@link GameData} clone as {@link CompletableFuture} incl. {@link History} which has
   *     already {@link History#enableSeeking(HistoryPanel)} called. Fails with {@link
   *     IllegalStateException} in case the cloning fails.
   */
  private CompletableFuture<GameData> getGameDataCloneWithHistory() {
    return CompletableFuture.supplyAsync(
        () ->
            GameDataUtils.cloneGameDataWithHistory(gameData, true)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "ExportMenu: Cloning of game data for exporting failed.")));
  }
}
