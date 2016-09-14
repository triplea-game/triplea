package games.strategy.triplea.ui.menubar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.export.GameDataExporter;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.printgenerator.SetupFrame;
import games.strategy.triplea.ui.ExtendedStats;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.MapPanel;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.util.PlayerOrderComparator;
import games.strategy.ui.SwingAction;
import games.strategy.util.IllegalCharacterRemover;
import games.strategy.util.LocalizeHTML;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class ExportMenu {

  private final TripleAFrame frame;
  private final GameData gameData;
  private final IUIContext iuiContext;

  public ExportMenu(final TripleAMenuBar menuBar, final TripleAFrame frame) {
    this.frame = frame;
    gameData = frame.getGame().getData();
    iuiContext = frame.getUIContext();

    final Menu menuGame = new Menu("_Export");
    menuGame.setMnemonicParsing(true);
    menuBar.getMenus().add(menuGame);
    addExportXML(menuGame);
    addExportStats(menuGame);
    addExportStatsFull(menuGame);
    addExportSetupCharts(menuGame);
    addExportUnitStats(menuGame);
    addSaveScreenshot(menuGame);
  }

  // TODO: create a second menu option for parsing current attachments
  private void addExportXML(final Menu parentMenu) {
    final MenuItem exportXML = new MenuItem("E_xport game.xml File (Beta)");
    exportXML.setMnemonicParsing(true);
    exportXML.setOnAction(e -> exportXMLFile());
    parentMenu.getItems().add(exportXML);
  }

  private void exportXMLFile() {
    final FileChooser chooser = new FileChooser();
    final File rootDir = new File(System.getProperties().getProperty("user.dir"));
    final DateFormat formatDate = new SimpleDateFormat("yyyy_MM_dd");
    int round = 0;
    try {
      gameData.acquireReadLock();
      round = gameData.getSequence().getRound();
    } finally {
      gameData.releaseReadLock();
    }
    String defaultFileName =
        "xml_" + formatDate.format(new Date()) + "_" + gameData.getGameName() + "_round_" + round;
    defaultFileName = IllegalCharacterRemover.removeIllegalCharacter(defaultFileName);
    defaultFileName = defaultFileName + ".xml";
    chooser.setInitialDirectory(rootDir);
    chooser.setInitialFileName(defaultFileName);
    File file = chooser.showSaveDialog(frame);
    if (file == null) {
      return;
    }
    final String xmlFile;
    try {
      gameData.acquireReadLock();
      final GameDataExporter exporter = new games.strategy.engine.data.export.GameDataExporter(gameData, false);
      xmlFile = exporter.getXML();
    } finally {
      gameData.releaseReadLock();
    }
    try {
      try (final FileWriter writer = new FileWriter(file);) {
        writer.write(xmlFile);
      }
    } catch (final IOException e1) {
      ClientLogger.logQuietly(e1);
    }
  }


  private void addSaveScreenshot(final Menu parentMenu) {
    MenuItem saveScreenshot = new MenuItem("_Export Screenshot");
    saveScreenshot.setMnemonicParsing(true);
    saveScreenshot.setOnAction(e -> {

      final HistoryPanel historyPanel = frame.getHistoryPanel();
      final HistoryNode curNode;
      if (historyPanel == null) {
        curNode = gameData.getHistory().getLastNode();
      } else {
        curNode = historyPanel.getCurrentNode();
      }
      saveScreenshot(curNode, frame, gameData);
    });
    parentMenu.getItems().add(saveScreenshot);
  }

  public static void saveScreenshot(final HistoryNode node, final TripleAFrame frame, final GameData gameData) {
    final FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().add(new ExtensionFilter("Saved Screenshots", "*.png"));
    File f = fileChooser.showSaveDialog(null);
    if (f == null) {
      return;
    }
    if (!f.getName().toLowerCase().endsWith(".png")) {
      f = new File(f.getParent(), f.getName() + ".png");
    }
    // A small warning so users will not over-write a file,
    if (f.exists()) {
      final int choice =
          JOptionPane.showConfirmDialog(null, "A file by that name already exists. Do you wish to over write it?",
              "Over-write?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      if (choice != JOptionPane.OK_OPTION) {
        return;
      }
    }
    final File file = f;
    final Runnable t = () -> {
      if (saveScreenshot(node, file, frame, gameData)) {
        Platform.runLater(() -> new Alert(AlertType.INFORMATION, "Screenshot Saved"));
      }
    };
    SwingAction.invokeAndWait(t);

  }

  private static boolean saveScreenshot(final HistoryNode node, final File file, final TripleAFrame frame,
      final GameData gameData) {
    // get current history node. if we are in history view, get the selected node.
    boolean retval = true;
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
    Canvas canvas = new Canvas(scale * mapPanel.getImageWidth(), scale * mapPanel.getImageHeight());
    try {
      // workaround to get the whole map
      // (otherwise the map is cut if current window is not on top of map)
      final int xOffset = mapPanel.getXOffset();
      final int yOffset = mapPanel.getYOffset();
      mapPanel.setTopLeft(0, 0);
      mapPanel.print(canvas.getGraphicsContext2D());
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
      if (iuiContext.getMapData().getBooleanProperty(MapData.PROPERTY_SCREENSHOT_TITLE_ENABLED)) {
        // everything else should be scaled down onto map image
        GraphicsContext mapGraphics = canvas.getGraphicsContext2D();
        mapGraphics.scale(scale, scale);
        mapGraphics.setFont(Font.font("Ariel", FontWeight.BOLD, title_size));
        mapGraphics.setFill(title_color);
        mapGraphics.fillText(gameData.getGameName() + " Round " + round, title_x, title_y);
      }

      // save Image as .png
      try {
        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, writableImage);
        ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
      } catch (final IOException e) {
        ClientLogger.logError("Error while saving Screenshot", e);
        retval = false;
      }
    } catch (Exception e) {
      ClientLogger.logError(e);
    }
    return retval;
  }

  private void addExportStatsFull(final Menu parentMenu) {
    MenuItem showDiceStats = new MenuItem("Export _Full Game Stats");
    showDiceStats.setMnemonicParsing(true);
    showDiceStats.setOnAction(e -> createAndSaveStats(true));
    parentMenu.getItems().add(showDiceStats);
  }

  private void addExportStats(final Menu parentMenu) {
    MenuItem showDiceStats = new MenuItem("Export _Short Game Stats");
    showDiceStats.setMnemonicParsing(true);
    showDiceStats.setOnAction(e -> createAndSaveStats(false));
    parentMenu.getItems().add(showDiceStats);
  }

  private void createAndSaveStats(final boolean showPhaseStats) {
    final ExtendedStats statPanel = new ExtendedStats(gameData, iuiContext);
    final FileChooser chooser = new FileChooser();
    final File rootDir = new File(System.getProperties().getProperty("user.dir"));
    final DateFormat formatDate = new SimpleDateFormat("yyyy_MM_dd");
    int currentRound = 0;
    try {
      gameData.acquireReadLock();
      currentRound = gameData.getSequence().getRound();
    } finally {
      gameData.releaseReadLock();
    }
    String defaultFileName = "stats_" + formatDate.format(new Date()) + "_" + gameData.getGameName() + "_round_"
        + currentRound + (showPhaseStats ? "_full" : "_short");
    defaultFileName = IllegalCharacterRemover.removeIllegalCharacter(defaultFileName);
    defaultFileName = defaultFileName + ".csv";
    chooser.setInitialDirectory(rootDir);
    chooser.setInitialFileName(defaultFileName);
    File file = chooser.showSaveDialog(frame);
    if (file == null) {
      return;
    }
    final StringBuilder text = new StringBuilder(1000);
    GameData clone;
    try {
      gameData.acquireReadLock();
      clone = GameDataUtils.cloneGameData(gameData);
      final IStat[] stats = statPanel.getStats();
      // extended stats covers stuff that doesn't show up in the game stats menu bar, like custom resources or tech
      // tokens or # techs, etc.
      final IStat[] statsExtended = statPanel.getStatsExtended(gameData);
      final String[] alliances = statPanel.getAlliances().toArray(new String[statPanel.getAlliances().size()]);
      final PlayerID[] players = statPanel.getPlayers().toArray(new PlayerID[statPanel.getPlayers().size()]);
      // its important here to translate the player objects into our game data
      // the players for the stat panel are only relevant with respect to
      // the game data they belong to
      for (int i = 0; i < players.length; i++) {
        players[i] = clone.getPlayerList().getPlayerID(players[i].getName());
      }
      text.append(defaultFileName + ",");
      text.append("\n");
      text.append("TripleA Engine Version: ,");
      text.append(games.strategy.engine.ClientContext.engineVersion() + ",");
      text.append("\n");
      text.append("Game Name: ,");
      text.append(gameData.getGameName() + ",");
      text.append("\n");
      text.append("Game Version: ,");
      text.append(gameData.getGameVersion() + ",");
      text.append("\n");
      text.append("\n");
      text.append("Current Round: ,");
      text.append(currentRound + ",");
      text.append("\n");
      text.append("Number of Players: ,");
      text.append(statPanel.getPlayers().size() + ",");
      text.append("\n");
      text.append("Number of Alliances: ,");
      text.append(statPanel.getAlliances().size() + ",");
      text.append("\n");
      text.append("\n");
      text.append("Turn Order: ,");
      text.append("\n");
      final List<PlayerID> playerOrderList = new ArrayList<>();
      playerOrderList.addAll(gameData.getPlayerList().getPlayers());
      Collections.sort(playerOrderList, new PlayerOrderComparator(gameData));
      final Set<PlayerID> playerOrderSetNoDuplicates = new LinkedHashSet<>(playerOrderList);
      final Iterator<PlayerID> playerOrderIterator = playerOrderSetNoDuplicates.iterator();
      while (playerOrderIterator.hasNext()) {
        final PlayerID currentPlayerID = playerOrderIterator.next();
        text.append(currentPlayerID.getName()).append(",");
        final Iterator<String> allianceName =
            gameData.getAllianceTracker().getAlliancesPlayerIsIn(currentPlayerID).iterator();
        while (allianceName.hasNext()) {
          text.append(allianceName.next()).append(",");
        }
        text.append("\n");
      }
      text.append("\n");
      text.append("Winners: ,");
      final EndRoundDelegate delegateEndRound = (EndRoundDelegate) gameData.getDelegateList().getDelegate("endRound");
      if (delegateEndRound != null && delegateEndRound.getWinners() != null) {
        for (final PlayerID p : delegateEndRound.getWinners()) {
          text.append(p.getName()).append(",");
        }
      } else {
        text.append("none yet; game not over,");
      }
      text.append("\n");
      text.append("\n");
      text.append("Resource Chart: ,");
      text.append("\n");
      final Iterator<Resource> resourceIterator = gameData.getResourceList().getResources().iterator();
      while (resourceIterator.hasNext()) {
        text.append(resourceIterator.next().getName() + ",");
        text.append("\n");
      }
      // if short, we won't both showing production and unit info
      if (showPhaseStats) {
        text.append("\n");
        text.append("Production Rules: ,");
        text.append("\n");
        text.append("Name,Result,Quantity,Cost,Resource,\n");
        final Iterator<ProductionRule> purchaseOptionsIterator =
            gameData.getProductionRuleList().getProductionRules().iterator();
        while (purchaseOptionsIterator.hasNext()) {
          final ProductionRule pr = purchaseOptionsIterator.next();
          String costString = pr.toStringCosts().replaceAll("; ", ",");
          costString = costString.replaceAll(" ", ",");
          text.append(pr.getName()).append(",").append(pr.getResults().keySet().iterator().next().getName()).append(",")
              .append(pr.getResults().getInt(pr.getResults().keySet().iterator().next())).append(",").append(costString)
              .append(",");
          text.append("\n");
        }
        text.append("\n");
        text.append("Unit Types: ,");
        text.append("\n");
        text.append("Name,Listed Abilities\n");
        final Iterator<UnitType> allUnitsIterator = gameData.getUnitTypeList().iterator();
        while (allUnitsIterator.hasNext()) {
          final UnitAttachment ua = UnitAttachment.get(allUnitsIterator.next());
          if (ua == null) {
            continue;
          }
          String toModify = ua.allUnitStatsForExporter();
          toModify = toModify.replaceFirst("UnitType called ", "").replaceFirst(" with:", "")
              .replaceAll("games.strategy.engine.data.", "").replaceAll("\n", ";").replaceAll(",", ";");
          toModify = toModify.replaceAll("  ", ",");
          toModify = toModify.replaceAll(", ", ",").replaceAll(" ,", ",");
          text.append(toModify);
          text.append("\n");
        }
      }
      text.append("\n");
      text.append((showPhaseStats ? "Full Stats (includes each phase that had activity),"
          : "Short Stats (only shows first phase with activity per player per round),"));
      text.append("\n");
      text.append("Turn Stats: ,");
      text.append("\n");
      text.append("Round,Player Turn,Phase Name,");
      for (final IStat stat : stats) {
        for (final PlayerID player : players) {
          text.append(stat.getName()).append(" ");
          text.append(player.getName());
          text.append(",");
        }
        for (final String alliance : alliances) {
          text.append(stat.getName()).append(" ");
          text.append(alliance);
          text.append(",");
        }
      }
      for (final IStat element : statsExtended) {
        for (final PlayerID player : players) {
          text.append(element.getName()).append(" ");
          text.append(player.getName());
          text.append(",");
        }
        for (final String alliance : alliances) {
          text.append(element.getName()).append(" ");
          text.append(alliance);
          text.append(",");
        }
      }
      text.append("\n");
      clone.getHistory().gotoNode(clone.getHistory().getLastNode());
      @SuppressWarnings("unchecked")
      final Enumeration<HistoryNode> nodes =
          ((DefaultMutableTreeNode) clone.getHistory().getRoot()).preorderEnumeration();
      PlayerID currentPlayer = null;
      int round = 0;
      while (nodes.hasMoreElements()) {
        // we want to export on change of turn
        final HistoryNode element = nodes.nextElement();
        if (element instanceof Round) {
          round++;
        }
        if (!(element instanceof Step)) {
          continue;
        }
        final Step step = (Step) element;
        if (step.getPlayerID() == null || step.getPlayerID().isNull()) {
          continue;
        }
        // this is to stop from having multiple entries for each players turn.
        if (!showPhaseStats) {
          if (step.getPlayerID() == currentPlayer) {
            continue;
          }
        }
        currentPlayer = step.getPlayerID();
        clone.getHistory().gotoNode(element);
        final String playerName = step.getPlayerID() == null ? "" : step.getPlayerID().getName() + ": ";
        String stepName = step.getStepName();
        // copied directly from TripleAPlayer, will probably have to be updated in the future if more delegates are made
        if (stepName.endsWith("Bid")) {
          stepName = "Bid";
        } else if (stepName.endsWith("Tech")) {
          stepName = "Tech";
        } else if (stepName.endsWith("TechActivation")) {
          stepName = "TechActivation";
        } else if (stepName.endsWith("Purchase")) {
          stepName = "Purchase";
        } else if (stepName.endsWith("NonCombatMove")) {
          stepName = "NonCombatMove";
        } else if (stepName.endsWith("Move")) {
          stepName = "Move";
        } else if (stepName.endsWith("Battle")) {
          stepName = "Battle";
        } else if (stepName.endsWith("BidPlace")) {
          stepName = "BidPlace";
        } else if (stepName.endsWith("Place")) {
          stepName = "Place";
        } else if (stepName.endsWith("Politics")) {
          stepName = "Politics";
        } else if (stepName.endsWith("EndTurn")) {
          stepName = "EndTurn";
        } else {
          stepName = "";
        }
        text.append(round).append(",").append(playerName).append(",").append(stepName).append(",");
        for (final IStat stat : stats) {
          for (final PlayerID player : players) {
            text.append(stat.getFormatter().format(stat.getValue(player, clone)));
            text.append(",");
          }
          for (final String alliance : alliances) {
            text.append(stat.getFormatter().format(stat.getValue(alliance, clone)));
            text.append(",");
          }
        }
        for (final IStat element2 : statsExtended) {
          for (final PlayerID player : players) {
            text.append(element2.getFormatter().format(element2.getValue(player, clone)));
            text.append(",");
          }
          for (final String alliance : alliances) {
            text.append(element2.getFormatter().format(element2.getValue(alliance, clone)));
            text.append(",");
          }
        }
        text.append("\n");
      }
    } finally {
      gameData.releaseReadLock();
    }
    try (final FileWriter writer = new FileWriter(file)) {
      writer.write(text.toString());
    } catch (final IOException e1) {
      ClientLogger.logQuietly(e1);
    }
  }

  private void addExportUnitStats(final Menu parentMenu) {
    final MenuItem menuFileExport = new MenuItem("Export _Unit Charts");
    menuFileExport.setMnemonicParsing(true);
    menuFileExport.setOnAction(e -> {
      final FileChooser chooser = new FileChooser();
      final File rootDir = new File(System.getProperties().getProperty("user.dir"));
      String defaultFileName = gameData.getGameName() + "_unit_stats";
      defaultFileName = IllegalCharacterRemover.removeIllegalCharacter(defaultFileName);
      defaultFileName = defaultFileName + ".html";
      chooser.setInitialFileName(defaultFileName);
      chooser.setInitialDirectory(rootDir);
      File selectedFile = chooser.showSaveDialog(frame);
      if (selectedFile == null) {
        return;
      }
      try (final FileWriter writer = new FileWriter(selectedFile)) {
        writer.write(HelpMenu.getUnitStatsTable(gameData, iuiContext).replaceAll("<p>", "<p>\r\n")
            .replaceAll("</p>", "</p>\r\n")
            .replaceAll("</tr>", "</tr>\r\n").replaceAll(LocalizeHTML.PATTERN_HTML_IMG_TAG, ""));
      } catch (final IOException e1) {
        ClientLogger.logQuietly(e1);
      }

    });
    parentMenu.getItems().add(menuFileExport);
  }


  private void addExportSetupCharts(final Menu parentMenu) {
    final MenuItem menuFileExport = new MenuItem("Export Setup _Charts");
    menuFileExport.setMnemonicParsing(true);
    menuFileExport.setOnAction(e -> Platform.runLater(() -> {
      Stage stage = new Stage();
      stage.setTitle("Export Setup Charts");
      GameData clonedGameData;
      gameData.acquireReadLock();
      try {
        clonedGameData = GameDataUtils.cloneGameData(gameData);
      } finally {
        gameData.releaseReadLock();
      }
      stage.setScene(new Scene(new SetupFrame(clonedGameData)));
      iuiContext.addShutdownWindow(stage.getScene().getWindow());
      stage.show();
    }));
    parentMenu.getItems().add(menuFileExport);

  }
}
