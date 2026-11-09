package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import static org.triplea.swing.SwingComponents.DialogWithLinksParams;
import static org.triplea.swing.SwingComponents.DialogWithLinksTypes;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.ui.FileBackedGamePropertiesCache;
import games.strategy.engine.framework.startup.ui.IGamePropertiesCache;
import games.strategy.engine.framework.ui.GameChooser;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.engine.framework.ui.background.TaskRunner;
import games.strategy.triplea.UrlConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SectionHeader;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/**
 * Top of the game staging screens: a toolbar to pick the game, and a header naming the selected
 * game. Also owns the {@link GameOptionsPanel} shown beside the setup panel.
 */
public final class GameSelectorPanel extends JPanel implements Observer {
  private static final long serialVersionUID = -4598107601238030020L;
  private static final int TITLE_GAP = 4;

  private final GameSelectorModel model;
  private final IGamePropertiesCache gamePropertiesCache = new FileBackedGamePropertiesCache();
  private final GameOptionsPanel gameOptionsPanel;
  private final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
  private final JLabel nameText = new JLabel();
  private final JLabel saveGameText = new JLabel();
  private final JLabel roundText = new JLabel();
  private final MapThumbnail mapThumbnail;

  private final JButton loadNewGame =
      new JButtonBuilder()
          .title("Select Game")
          .toolTip("Start a new game (autosaves on bot servers will appear here)")
          .build();

  private final JButton loadSavedGame =
      new JButtonBuilder()
          .title("Saved Game")
          .toolTip("Open a game saved on your computer.")
          .build();

  /** Options of a game hosted on a bot live on the bot, so they open in a dialog instead. */
  private final JButton hostBotGameOptions =
      new JButtonBuilder()
          .title("Game Options")
          .toolTip("Set options for the game on the bot, such as Low Luck or Technology")
          .build();

  public GameSelectorPanel(final GameSelectorModel model) {
    this.model = model;
    gameOptionsPanel = new GameOptionsPanel(model, gamePropertiesCache);
    final GameData data = model.getGameData();
    gameOptionsPanel.update();
    if (data != null) {
      gamePropertiesCache.loadCachedGamePropertiesInto(data);
      gameOptionsPanel.refresh();
    }

    final JButton downloadMapButton =
        new JButtonBuilder()
            .title("Download Maps")
            .toolTip("Click this button to install additional maps")
            .actionListener(DownloadMapsWindow::showDownloadMapsWindow)
            .build();
    toolbar.add(loadNewGame);
    toolbar.add(loadSavedGame);
    toolbar.add(downloadMapButton);
    toolbar.add(hostBotGameOptions);
    toolbar.setBorder(SectionHeader.ruleBelow());

    nameText.setFont(nameText.getFont().deriveFont(Font.BOLD, nameText.getFont().getSize2D() + 10));
    final JPanel titleBlock = new JPanel();
    titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
    titleBlock.add(nameText);
    titleBlock.add(Box.createVerticalStrut(TITLE_GAP));
    titleBlock.add(roundText);
    titleBlock.add(saveGameText);

    mapThumbnail =
        new MapThumbnail(
            nameText.getFontMetrics(nameText.getFont()).getHeight()
                + TITLE_GAP
                + roundText.getFontMetrics(roundText.getFont()).getHeight());
    final JPanel header = new JPanel(new BorderLayout());
    header.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
    header.add(mapThumbnail.getComponent(), BorderLayout.WEST);
    header.add(titleBlock, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    setBorder(SectionHeader.ruleBelow());
    add(toolbar, BorderLayout.NORTH);
    add(header, BorderLayout.CENTER);

    loadNewGame.addActionListener(
        e -> {
          if (canSelectLocalGameData()) {
            selectGameFile();
          } else if (canChangeHostBotGameData()) {
            final ClientModel clientModelForHostBots = model.getClientModelForHostBots();
            if (clientModelForHostBots != null) {
              clientModelForHostBots.setMap(this);
            }
          }
        });

    loadSavedGame.addActionListener(
        e -> {
          if (canSelectLocalGameData()) {
            selectSavedGameFile();
          } else if (canChangeHostBotGameData()) {
            final ClientModel clientModelForHostBots = model.getClientModelForHostBots();
            if (clientModelForHostBots != null) {
              clientModelForHostBots.executeChangeGameToSaveGameClientAction(
                  JOptionPane.getFrameForComponent(this));
            }
          }
        });

    hostBotGameOptions.addActionListener(
        e -> {
          final ClientModel clientModelForHostBots = model.getClientModelForHostBots();
          if (clientModelForHostBots != null) {
            clientModelForHostBots.changeGameOptions(this);
          }
        });

    updateGameData();
  }

  public GameOptionsPanel getGameOptionsPanel() {
    return gameOptionsPanel;
  }

  private void updateGameData() {
    SwingAction.invokeNowOrLater(
        () -> {
          final String gameName = model.getGameName();
          nameText.setText(isUnset(gameName) ? "No game selected" : gameName);
          roundText.setText("Round " + model.getGameRound());
          roundText.setVisible(!isUnset(model.getGameRound()));
          saveGameText.setText("Saved game: " + model.getFileName());
          saveGameText.setVisible(!isUnset(model.getFileName()));
          final GameData gameData = model.getGameData();
          if (gameData == null) {
            mapThumbnail.clear();
          } else {
            mapThumbnail.showMap(gameData.getMapName());
          }

          final boolean canChangeHostBotGameData = canChangeHostBotGameData();
          // network clients can't change the game, so they don't get the controls for it
          toolbar.setVisible(canSelectLocalGameData() || canChangeHostBotGameData);
          hostBotGameOptions.setVisible(canChangeHostBotGameData);
        });
    gameOptionsPanel.update();
  }

  /** {@link GameSelectorModel} reports a missing name, round or file as "-". */
  private static boolean isUnset(final String value) {
    return "-".equals(value);
  }

  private boolean canSelectLocalGameData() {
    return model != null && model.isCanSelect();
  }

  private boolean canChangeHostBotGameData() {
    return model != null && model.isHostIsHeadlessBot();
  }

  @Override
  public void update(final Observable o, final Object arg) {
    updateGameData();
  }

  private void selectSavedGameFile() {
    GameFileSelector.builder()
        .fileDoesNotExistAction(
            file ->
                DialogBuilder.builder()
                    .parent(this)
                    .title("Save Game File Not Found")
                    .errorMessage("File does not exist: " + file.toAbsolutePath())
                    .showDialog())
        .build()
        .selectGameFile(JOptionPane.getFrameForComponent(this))
        .ifPresent(this::loadSaveFile);
  }

  public void loadSaveFile(final Path file) {
    TaskRunner.builder()
        .waitDialogTitle("Loading Save Game")
        .exceptionHandler(
            e ->
                SwingComponents.showDialogWithLinks(
                    DialogWithLinksParams.builder()
                        .title("Failed To Load Save Game")
                        .dialogType(DialogWithLinksTypes.ERROR)
                        .dialogText(
                            String.format(
                                "<html>Error: %s<br/><br/>"
                                    + "If this is not expected, please "
                                    + "file a <a href=%s>bug report</a><br/>"
                                    + "and attach the error message above and the "
                                    + "save game you are trying to load.",
                                e.getMessage(), UrlConstants.GITHUB_ISSUES))
                        .build()))
        .build()
        .run(() -> model.loadSave(file));
  }

  private void selectGameFile() {
    try {
      final InstalledMapsListing installedMapsListing =
          BackgroundTaskRunner.runInBackgroundAndReturn(
              "Loading all available games...", InstalledMapsListing::parseMapFiles);

      GameChooser.chooseGame(
          JOptionPane.getFrameForComponent(this),
          installedMapsListing,
          model.getGameName(),
          this::gameSelected);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void gameSelected(final Path gameFile) {
    BackgroundTaskRunner.runInBackground(
        "Loading map...",
        () -> {
          if (model.loadMap(gameFile)) {
            // only for new games, not saved games, we set the default options, and set them only
            // once (the first time it is loaded)
            gamePropertiesCache.loadCachedGamePropertiesInto(model.getGameData());
            // the editors may have been built before the cached values were set
            gameOptionsPanel.refresh();
          }
        });
  }
}
