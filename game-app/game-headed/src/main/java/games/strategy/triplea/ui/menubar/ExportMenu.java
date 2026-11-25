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
import games.strategy.triplea.ui.export.ScreenshotExporter;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.triplea.ui.menubar.help.UnitStatsTable;
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
import javax.annotation.Nonnull;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.map.data.elements.Game;
import org.triplea.map.xml.writer.GameXmlWriter;
import org.triplea.swing.FileChooser;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.util.FileNameUtils;

@UtilityClass
@Slf4j
final class ExportMenu {

  private static final String DATE_TIME_FORMAT_FILE_NAME = "yyyy_MM_dd";

  public static JMenu get(final TripleAFrame frame) {
    return new JMenuBuilder("Export", TripleAMenuBar.Mnemonic.EXPORT.getMnemonicCode())
        .addMenuItem(
            // TODO: create a second menu option for parsing current attachments
            new JMenuItemBuilder(
                    "Export game.xml File (Beta)", Mnemonic.EXPORT_XML.getMnemonicCode())
                .actionListener(() -> exportXmlFile(frame)))
        .addMenuItem(
            new JMenuItemBuilder(
                    "Export Short Game Stats", Mnemonic.EXPORT_STATS_SHORT.getMnemonicCode())
                .actionListener(() -> createAndSaveStats(frame, false)))
        .addMenuItem(
            new JMenuItemBuilder(
                    "Export Full Game Stats", Mnemonic.EXPORT_STATS_FULL.getMnemonicCode())
                .actionListener(() -> createAndSaveStats(frame, true)))
        .addMenuItem(
            new JMenuItemBuilder(
                    "Export Setup Charts", Mnemonic.EXPORT_CHARTS_SETUP.getMnemonicCode())
                .actionListener(() -> exportSetupCharts(frame)))
        .addMenuItem(
            new JMenuItemBuilder(
                    "Export Unit Charts", Mnemonic.EXPORT_CHARTS_UNIT.getMnemonicCode())
                .actionListener(() -> exportUnitCharts(frame)))
        .addMenuItem(
            new JMenuItemBuilder(
                    "Export Gameboard Picture", Mnemonic.EXPORT_PICTURE.getMnemonicCode())
                .actionListener(() -> saveScreenshot(frame)))
        .build();
  }

  private static void exportXmlFile(final TripleAFrame frame) {
    final GameData gameData = frame.getGame().getData();
    // The existing XML file is needed for attachment ordering data.
    final Path gameXmlPath =
        gameData.getGameXmlPath(frame.getUiContext().getMapLocation()).orElse(null);
    if (gameXmlPath == null) {
      JOptionPane.showMessageDialog(frame, "Error: Existing XML file not found.");
      return;
    }
    final String defaultFileName =
        FileNameUtils.removeIllegalCharacters(
                String.format(
                    "xml_%s_%s_round_%d",
                    getCurrentDateTimeForFileName(),
                    gameData.getGameName(),
                    gameData.getCurrentRound()))
            + ".xml";

    FileChooser.chooseExportFileWithDefaultName(frame, defaultFileName)
        .ifPresent(
            path -> {
              try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
                final Game xmlGameModel = GameDataExporter.convertToXmlModel(gameData, gameXmlPath);
                GameXmlWriter.exportXml(xmlGameModel, path);
              }
            });
  }

  @Nonnull
  private static String getCurrentDateTimeForFileName() {
    return DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_FILE_NAME)
        .format(LocalDateTime.now(ZoneId.systemDefault()));
  }

  private static void saveScreenshot(final TripleAFrame frame) {
    // get current history node. if we are in history view, get the selected node.
    final GameData gameData = frame.getGame().getData();
    final HistoryPanel historyPanel = frame.getHistoryPanel();
    final HistoryNode curNode;
    if (historyPanel == null) {
      curNode = gameData.getHistory().getLastNode();
    } else {
      curNode = historyPanel.getCurrentNode();
    }
    ScreenshotExporter.exportScreenshot(frame, gameData, curNode);
  }

  private void createAndSaveStats(final TripleAFrame frame, final boolean showPhaseStats) {
    GameData gameData = frame.getGame().getData();
    final String defaultFileName =
        FileNameUtils.removeIllegalCharacters(
                String.format(
                    "stats_%s_%s_round_%s_%s",
                    getCurrentDateTimeForFileName(),
                    gameData.getGameName(),
                    gameData.getCurrentRound(),
                    showPhaseStats ? "full" : "short"))
            + ".csv";
    final CompletableFuture<GameData> cloneCompletableFuture =
        getGameDataCloneWithHistory(gameData);
    Optional<Path> chosenFilePath =
        FileChooser.chooseExportFileWithDefaultName(frame, defaultFileName);
    chosenFilePath.ifPresent(
        path ->
            StatsInfo.export(
                path, frame.getUiContext(), showPhaseStats, cloneCompletableFuture.join()));
  }

  /**
   * Provides a game data copy with its history by triggering an asynchronous process for the
   * copying task.
   *
   * @return {@link GameData} clone as {@link CompletableFuture} incl. {@link History} which has
   *     already {@link History#enableSeeking(HistoryPanel)} called. Fails with {@link
   *     IllegalStateException} in case the cloning fails.
   */
  private CompletableFuture<GameData> getGameDataCloneWithHistory(final GameData gameData) {
    return CompletableFuture.supplyAsync(
        () -> GameDataUtils.cloneGameDataWithHistory(gameData, true));
  }

  private static void exportUnitCharts(final TripleAFrame frame) {
    final GameData gameData = frame.getGame().getData();
    final String defaultFileName =
        FileNameUtils.removeIllegalCharacters(gameData.getGameName()) + "_unit_stats.html";
    FileChooser.chooseExportFileWithDefaultName(frame, defaultFileName)
        .ifPresent(
            path -> {
              try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write(
                    UnitStatsTable.getUnitStatsTable(gameData, frame.getUiContext())
                        .replaceAll("</?p>|</tr>", "$0\r\n")
                        .replaceAll("(?i)<img[^>]+/>", ""));
              } catch (final IOException e1) {
                log.error("Failed to write unit stats: {}", path, e1);
              }
            });
  }

  private static void exportSetupCharts(final TripleAFrame frame) {
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
                            frame, getGameDataCloneWithHistory(frame.getGame().getData())))
                    .build())
        .visible(true)
        .build();
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public enum Mnemonic {
    EXPORT_PICTURE(KeyCode.E),
    EXPORT_STATS_FULL(KeyCode.F),
    EXPORT_STATS_SHORT(KeyCode.S),
    EXPORT_XML(KeyCode.X),
    EXPORT_CHARTS_UNIT(KeyCode.U),
    EXPORT_CHARTS_SETUP(KeyCode.C);

    private final KeyCode mnemonicCode;

    public int getValue() {
      return mnemonicCode.getInputEventCode();
    }
  }
}
