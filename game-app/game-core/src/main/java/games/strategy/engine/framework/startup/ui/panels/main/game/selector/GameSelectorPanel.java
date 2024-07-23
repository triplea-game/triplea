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
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.framework.ui.GameChooser;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.engine.framework.ui.background.TaskRunner;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.UrlConstants;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;

/**
 * Left hand side panel of the launcher screen that has various info, like selected game and engine
 * version.
 */
public final class GameSelectorPanel extends JPanel implements Observer {
  private static final long serialVersionUID = -4598107601238030020L;

  private final GameSelectorModel model;
  private final IGamePropertiesCache gamePropertiesCache = new FileBackedGamePropertiesCache();
  private final Map<String, Object> originalPropertiesMap = new HashMap<>();
  private final JLabel nameText = new JLabel();
  private final JLabel saveGameText = new JLabel();
  private final JLabel roundText = new JLabel();

  private final JButton loadNewGame =
      new JButtonBuilder()
          .title("Select Game")
          .toolTip("Start a new game or load an autosave from the host server.")
          .build();

  private final JButton loadSavedGame =
      new JButtonBuilder()
          .title("Upload Saved Game")
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

    setLayout(new GridBagLayout());

    final JLabel logoLabel =
        new JLabel(
            new ImageIcon(
                ResourceLoader.loadImageAsset(Path.of("launch_screens", "triplea-logo.png"))));

    int row = 0;
    add(
        logoLabel,
        new GridBagConstraintsBuilder(0, row)
            .gridWidth(2)
            .insets(new Insets(10, 10, 3, 5))
            .build());
    row++;

    add(new JLabel("Java Version:"), buildGridCell(0, row, new Insets(10, 10, 3, 5)));
    add(
        new JLabel(SystemProperties.getJavaVersion()),
        buildGridCell(1, row, new Insets(10, 0, 3, 0)));
    row++;

    add(new JLabel("Engine Version:"), buildGridCell(0, row, new Insets(0, 10, 3, 5)));
    add(
        new JLabel(ProductVersionReader.getCurrentVersion().toString()),
        buildGridCell(1, row, new Insets(0, 0, 3, 0)));
    row++;

    add(new JLabel("Game Name:"), buildGridCell(0, row, new Insets(0, 10, 3, 5)));
    add(nameText, buildGridCell(1, row, new Insets(0, 0, 3, 0)));
    row++;

    add(new JLabel("Game Round:"), buildGridCell(0, row, new Insets(0, 10, 3, 5)));
    add(roundText, buildGridCell(1, row, new Insets(0, 0, 3, 0)));
    row++;

    add(new JLabel("Loaded Savegame:"), buildGridCell(0, row, new Insets(20, 10, 3, 5)));
    row++;

    add(saveGameText, buildGridRow(0, row, new Insets(0, 10, 3, 5)));
    row++;

    add(loadNewGame, buildGridRow(0, row, new Insets(25, 10, 10, 10)));
    row++;

    add(loadSavedGame, buildGridRow(0, row, new Insets(0, 10, 10, 10)));
    row++;

    final JButton downloadMapButton =
        new JButtonBuilder()
            .title("Download Maps")
            .toolTip("Click this button to install additional maps")
            .actionListener(DownloadMapsWindow::showDownloadMapsWindow)
            .build();
    add(downloadMapButton, buildGridRow(0, row, new Insets(0, 10, 10, 10)));
    row++;

    add(mapOptions, buildGridRow(0, row, new Insets(25, 10, 10, 10)));
    row++;

    // spacer
    add(
        new JPanel(),
        new GridBagConstraints(
            0,
            row,
            2,
            1,
            1,
            1,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0),
            0,
            0));

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

  private static GridBagConstraints buildGridCell(final int x, final int y, final Insets insets) {
    return buildGrid(x, y, insets, 1);
  }

  private static GridBagConstraints buildGridRow(final int x, final int y, final Insets insets) {
    return buildGrid(x, y, insets, 2);
  }

  private static GridBagConstraints buildGrid(
      final int x, final int y, final Insets insets, final int width) {
    final int gridHeight = 1;
    final double weightX = 0;
    final double weightY = 0;
    final int anchor = GridBagConstraints.WEST;
    final int fill = GridBagConstraints.NONE;
    final int ipadx = 0;
    final int ipady = 0;

    return new GridBagConstraints(
        x, y, width, gridHeight, weightX, weightY, anchor, fill, insets, ipadx, ipady);
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
          nameText.setText(model.getGameName());
          roundText.setText(model.getGameRound());
          saveGameText.setText(model.getFileName());

          final boolean canSelectGameData = canSelectLocalGameData();
          final boolean canChangeHostBotGameData = canChangeHostBotGameData();
          loadSavedGame.setEnabled(canSelectGameData || canChangeHostBotGameData);
          loadNewGame.setEnabled(canSelectGameData || canChangeHostBotGameData);
          // Disable game options if there are none.
          mapOptions.setEnabled(
              canChangeHostBotGameData
                  || (canSelectGameData
                      && model.getGameData() != null
                      && !model.getGameData().getProperties().getEditableProperties().isEmpty()));
        });
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

  private void gameSelected(final Path gameFile) {
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
