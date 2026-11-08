package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import static org.triplea.swing.SwingComponents.DialogWithLinksParams;
import static org.triplea.swing.SwingComponents.DialogWithLinksTypes;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUi;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/**
 * Top of the game staging screens: a toolbar to pick the game, and a header naming the selected
 * game.
 */
public final class GameSelectorPanel extends JPanel implements Observer {
  private static final long serialVersionUID = -4598107601238030020L;

  private final GameSelectorModel model;
  private final IGamePropertiesCache gamePropertiesCache = new FileBackedGamePropertiesCache();
  private final Map<String, Object> originalPropertiesMap = new HashMap<>();
  private final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
  private final JLabel nameText = new JLabel();
  private final JLabel saveGameText = new JLabel();
  private final JLabel roundText = new JLabel();

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

  private final JButton mapOptions =
      new JButtonBuilder()
          .title("Game Options")
          .toolTip(
              "<html>Set options for the currently selected game, <br>such as enabling/disabling "
                  + "Low Luck, or Technology</html>")
          .build();

  public GameSelectorPanel(final GameSelectorModel model) {
    this.model = model;
    final GameData data = model.getGameData();
    if (data != null) {
      setOriginalPropertiesMap(data);
      gamePropertiesCache.loadCachedGamePropertiesInto(data);
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
    toolbar.add(mapOptions);

    nameText.setFont(nameText.getFont().deriveFont(Font.BOLD, nameText.getFont().getSize2D() + 10));
    final JPanel header = new JPanel();
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
    header.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
    header.add(nameText);
    header.add(Box.createVerticalStrut(4));
    header.add(roundText);
    header.add(saveGameText);

    setLayout(new BorderLayout());
    setBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")));
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

    mapOptions.addActionListener(
        e -> {
          if (canSelectLocalGameData()) {
            selectGameOptions();
          } else if (canChangeHostBotGameData()) {
            final ClientModel clientModelForHostBots = model.getClientModelForHostBots();
            if (clientModelForHostBots != null) {
              clientModelForHostBots.changeGameOptions(this);
            }
          }
        });

    updateGameData();
  }

  private void setOriginalPropertiesMap(final GameData data) {
    originalPropertiesMap.clear();
    if (data != null) {
      for (final IEditableProperty<?> property : data.getProperties().getEditableProperties()) {
        originalPropertiesMap.put(property.getName(), property.getValue());
      }
    }
  }

  private void selectGameOptions() {
    // backup current game properties before showing dialog
    final Map<String, Object> currentPropertiesMap = new HashMap<>();
    for (final IEditableProperty<?> property :
        model.getGameData().getProperties().getEditableProperties()) {
      currentPropertiesMap.put(property.getName(), property.getValue());
    }
    final PropertiesUi panel = new PropertiesUi(model.getGameData().getProperties(), true);
    final JScrollPane scroll = new JScrollPane(panel);
    scroll.setBorder(null);
    scroll.getViewport().setBorder(null);
    final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
    final String ok = "OK";
    final String cancel = "Cancel";
    final String makeDefault = "Make Default";
    final String reset = "Reset";
    pane.setOptions(new Object[] {ok, makeDefault, reset, cancel});
    final JDialog window =
        pane.createDialog(JOptionPane.getFrameForComponent(this), "Game Options");
    window.setVisible(true);
    final Object buttonPressed = pane.getValue();
    if (buttonPressed == null || buttonPressed.equals(cancel)) {
      // restore properties, if cancel was pressed, or window was closed
      for (final IEditableProperty<?> property :
          model.getGameData().getProperties().getEditableProperties()) {
        property.validateAndSet(currentPropertiesMap.get(property.getName()));
      }
    } else if (buttonPressed.equals(reset)) {
      if (!originalPropertiesMap.isEmpty()) {
        // restore properties, if cancel was pressed, or window was closed
        for (final IEditableProperty<?> property :
            model.getGameData().getProperties().getEditableProperties()) {
          property.validateAndSet(originalPropertiesMap.get(property.getName()));
        }
        selectGameOptions();
      }
    } else if (buttonPressed.equals(makeDefault)) {
      gamePropertiesCache.cacheGameProperties(model.getGameData());
    }
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

          final boolean canSelectGameData = canSelectLocalGameData();
          final boolean canChangeHostBotGameData = canChangeHostBotGameData();
          // network clients can't change the game, so they don't get the controls for it
          toolbar.setVisible(canSelectGameData || canChangeHostBotGameData);
          // Disable game options if there are none.
          mapOptions.setEnabled(
              canChangeHostBotGameData
                  || (canSelectGameData
                      && model.getGameData() != null
                      && !model.getGameData().getProperties().getEditableProperties().isEmpty()));
        });
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
        .run(
            () -> {
              if (model.loadSave(file)) {
                setOriginalPropertiesMap(model.getGameData());
              }
            });
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
            setOriginalPropertiesMap(model.getGameData());
            // only for new games, not saved games, we set the default options, and set them only
            // once (the first time it is loaded)
            gamePropertiesCache.loadCachedGamePropertiesInto(model.getGameData());
          }
        });
  }
}
