package games.strategy.engine.framework.startup.ui;

import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.framework.ui.GameChooser;
import games.strategy.engine.framework.ui.GameChooserEntry;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingAction;
import games.strategy.util.Interruptibles;
import swinglib.JButtonBuilder;

/**
 * Left hand side panel of the launcher screen that has various info, like selected game and engine version.
 */
public class GameSelectorPanel extends JPanel implements Observer {
  private static final long serialVersionUID = -4598107601238030020L;

  private JLabel engineVersionLabel;
  private JLabel engineVersionText;
  private JLabel nameText;
  private JLabel versionText;
  private JLabel fileNameLabel;
  private JLabel fileNameText;
  private JLabel nameLabel;
  private JLabel versionLabel;
  private JLabel roundLabel;
  private JLabel roundText;
  private JButton loadSavedGame;
  private JButton loadNewGame;
  private JButton gameOptions;
  private final GameSelectorModel model;
  private final IGamePropertiesCache gamePropertiesCache = new FileBackedGamePropertiesCache();
  private final Map<String, Object> originalPropertiesMap = new HashMap<>();

  GameSelectorPanel(final GameSelectorModel model) {
    this.model = model;
    this.model.addObserver(this);
    final GameData data = model.getGameData();
    if (data != null) {
      setOriginalPropertiesMap(data);
      gamePropertiesCache.loadCachedGamePropertiesInto(data);
    }
    createComponents();
    layoutComponents();
    setupListeners();
    setWidgetActivation();
    updateGameData();
  }

  private void updateGameData() {
    SwingAction.invokeNowOrLater(() -> {
      nameText.setText(model.getGameName());
      versionText.setText(model.getGameVersion());
      roundText.setText(model.getGameRound());
      String fileName = model.getFileName();
      if ((fileName != null) && (fileName.length() > 1)) {
        try {
          fileName = URLDecoder.decode(fileName, "UTF-8");
        } catch (final IllegalArgumentException | UnsupportedEncodingException e) { // ignore
        }
      }
      fileNameText.setText(getFormattedFileNameText(fileName,
          Math.max(22, 3 + nameText.getText().length() + nameLabel.getText().length())));
      fileNameText.setToolTipText(fileName);
    });
  }

  /**
   * Formats the file name text to two lines.
   * The separation focuses on the second line being at least the filename while the first line
   * should show the the path including '...' in case it does not fit
   *
   * @param fileName
   *        full file name
   * @param maxLength
   *        maximum number of characters per line
   * @return filename formatted file name - in case it is too long (> maxLength) to two lines
   */
  private static String getFormattedFileNameText(final String fileName, final int maxLength) {
    if (fileName.length() <= maxLength) {
      return fileName;
    }
    int cutoff = fileName.length() - maxLength;
    String secondLine = fileName.substring(cutoff);
    if (secondLine.contains("/")) {
      cutoff += secondLine.indexOf("/") + 1;
    }
    secondLine = fileName.substring(cutoff);
    String firstLine = fileName.substring(0, cutoff);
    if (firstLine.length() > maxLength) {
      firstLine = firstLine.substring(0, maxLength - 4);
      if (firstLine.contains("/")) {
        cutoff = firstLine.lastIndexOf("/") + 1;
        firstLine = firstLine.substring(0, cutoff) + ".../";
      } else {
        firstLine = firstLine + "...";
      }
    }
    return "<html><p>" + firstLine + "<br/>" + secondLine + "</p></html>";
  }

  private void createComponents() {
    engineVersionLabel = new JLabel("Engine Version:");
    final String version = ClientContext.engineVersion().getExactVersion();
    engineVersionText = new JLabel(version);
    nameLabel = new JLabel("Map Name:");
    versionLabel = new JLabel("Map Version:");
    roundLabel = new JLabel("Game Round:");
    fileNameLabel = new JLabel("File Name:");
    nameText = new JLabel();
    versionText = new JLabel();
    roundText = new JLabel();
    fileNameText = new JLabel();
    loadNewGame = new JButton("Select Map");
    loadNewGame.setToolTipText("<html>Select a game from all the maps/games that come with TripleA, <br>and the ones "
        + "you have downloaded.</html>");
    loadSavedGame = new JButton("Open Saved Game");
    loadSavedGame.setToolTipText("Open a previously saved game, or an autosave.");
    gameOptions = new JButton("Map Options");
    gameOptions.setToolTipText("<html>Set options for the currently selected game, <br>such as enabling/disabling "
        + "Low Luck, or Technology, etc.</html>");
  }



  private void layoutComponents() {
    setLayout(new GridBagLayout());
    add(engineVersionLabel, buildGridCell(0, 0, new Insets(10, 10, 3, 5)));
    add(engineVersionText, buildGridCell(1, 0, new Insets(10, 0, 3, 0)));

    add(nameLabel, buildGridCell(0, 1, new Insets(0, 10, 3, 5)));
    add(nameText, buildGridCell(1, 1, new Insets(0, 0, 3, 0)));

    add(versionLabel, buildGridCell(0, 2, new Insets(0, 10, 3, 5)));
    add(versionText, buildGridCell(1, 2, new Insets(0, 0, 3, 0)));

    add(roundLabel, buildGridCell(0, 3, new Insets(0, 10, 3, 5)));
    add(roundText, buildGridCell(1, 3, new Insets(0, 0, 3, 0)));

    add(fileNameLabel, buildGridCell(0, 4, new Insets(20, 10, 3, 5)));

    add(fileNameText, buildGridRow(0, 5, new Insets(0, 10, 3, 5)));

    add(loadNewGame, buildGridRow(0, 6, new Insets(25, 10, 10, 10)));

    add(loadSavedGame, buildGridRow(0, 7, new Insets(0, 10, 10, 10)));

    final JButton downloadMapButton = JButtonBuilder.builder()
        .title("Download Maps")
        .toolTip("Click this button to install additional maps")
        .actionListener(DownloadMapsWindow::showDownloadMapsWindow)
        .build();
    add(downloadMapButton, buildGridRow(0, 8, new Insets(0, 10, 10, 10)));

    add(gameOptions, buildGridRow(0, 9, new Insets(25, 10, 10, 10)));

    // spacer
    add(new JPanel(), new GridBagConstraints(0, 10, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0));
  }


  private static GridBagConstraints buildGridCell(final int x, final int y, final Insets insets) {
    return buildGrid(x, y, insets, 1);
  }

  private static GridBagConstraints buildGridRow(final int x, final int y, final Insets insets) {
    return buildGrid(x, y, insets, 2);
  }

  private static GridBagConstraints buildGrid(final int x, final int y, final Insets insets, final int width) {
    final int gridWidth = width;
    final int gridHeight = 1;
    final double weigthX = 0;
    final double weigthY = 0;
    final int anchor = GridBagConstraints.WEST;
    final int fill = GridBagConstraints.NONE;
    final int ipadx = 0;
    final int ipady = 0;

    return new GridBagConstraints(x, y, gridWidth, gridHeight, weigthX, weigthY, anchor, fill, insets, ipadx, ipady);
  }


  private void setupListeners() {
    loadNewGame.addActionListener(e -> {
      if (canSelectLocalGameData()) {
        selectGameFile(false);
      } else if (canChangeHostBotGameData()) {
        final ClientModel clientModelForHostBots = model.getClientModelForHostBots();
        if (clientModelForHostBots != null) {
          clientModelForHostBots.getHostBotSetMapClientAction(GameSelectorPanel.this).actionPerformed(e);
        }
      }
    });
    loadSavedGame.addActionListener(e -> {
      if (canSelectLocalGameData()) {
        selectGameFile(true);
      } else if (canChangeHostBotGameData()) {
        final ClientModel clientModelForHostBots = model.getClientModelForHostBots();
        if (clientModelForHostBots != null) {
          final JPopupMenu menu = new JPopupMenu();
          menu.add(clientModelForHostBots.getHostBotChangeGameToSaveGameClientAction());
          menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this,
              SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE));
          menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this,
              SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE2));
          menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this,
              SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_ODD));
          menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this,
              SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_EVEN));
          menu.add(clientModelForHostBots.getHostBotGetGameSaveClientAction(GameSelectorPanel.this));
          final Point point = loadSavedGame.getLocation();
          menu.show(GameSelectorPanel.this, point.x + loadSavedGame.getWidth(), point.y);
        }
      }
    });
    gameOptions.addActionListener(e -> {
      if (canSelectLocalGameData()) {
        selectGameOptions();
      } else if (canChangeHostBotGameData()) {
        final ClientModel clientModelForHostBots = model.getClientModelForHostBots();
        if (clientModelForHostBots != null) {
          clientModelForHostBots.getHostBotChangeGameOptionsClientAction(GameSelectorPanel.this).actionPerformed(e);
        }
      }
    });
  }

  private void setOriginalPropertiesMap(final GameData data) {
    originalPropertiesMap.clear();
    if (data != null) {
      for (final IEditableProperty property : data.getProperties().getEditableProperties()) {
        originalPropertiesMap.put(property.getName(), property.getValue());
      }
    }
  }

  private void selectGameOptions() {
    // backup current game properties before showing dialog
    final Map<String, Object> currentPropertiesMap = new HashMap<>();
    for (final IEditableProperty property : model.getGameData().getProperties().getEditableProperties()) {
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
    final JDialog window = pane.createDialog(JOptionPane.getFrameForComponent(this), "Map Options");
    window.setVisible(true);
    final Object buttonPressed = pane.getValue();
    if ((buttonPressed == null) || buttonPressed.equals(cancel)) {
      // restore properties, if cancel was pressed, or window was closed
      for (final IEditableProperty property : model.getGameData().getProperties().getEditableProperties()) {
        property.setValue(currentPropertiesMap.get(property.getName()));
      }
    } else if (buttonPressed.equals(reset)) {
      if (!originalPropertiesMap.isEmpty()) {
        // restore properties, if cancel was pressed, or window was closed
        for (final IEditableProperty property : model.getGameData().getProperties().getEditableProperties()) {
          property.setValue(originalPropertiesMap.get(property.getName()));
        }
        selectGameOptions();
      }
    } else if (buttonPressed.equals(makeDefault)) {
      gamePropertiesCache.cacheGameProperties(model.getGameData());
    } else {
      // ok was clicked, and we have modified the properties already
    }
  }

  private void setWidgetActivation() {
    SwingAction.invokeNowOrLater(() -> {
      final boolean canSelectGameData = canSelectLocalGameData();
      final boolean canChangeHostBotGameData = canChangeHostBotGameData();
      loadSavedGame.setEnabled(canSelectGameData || canChangeHostBotGameData);
      loadNewGame.setEnabled(canSelectGameData || canChangeHostBotGameData);
      // Disable game options if there are none.

      gameOptions.setEnabled(canChangeHostBotGameData || (canSelectGameData
          && (model.getGameData() != null)
          && (model.getGameData().getProperties().getEditableProperties().size() > 0)));
    });
  }

  private boolean canSelectLocalGameData() {
    return (model != null) && model.canSelect();
  }

  private boolean canChangeHostBotGameData() {
    return (model != null) && model.isHostHeadlessBot();
  }

  @Override
  public void update(final Observable o, final Object arg) {
    updateGameData();
    setWidgetActivation();
  }

  public static File selectGameFile() {
    if (SystemProperties.isMac()) {
      final FileDialog fileDialog = GameRunner.newFileDialog();
      fileDialog.setMode(FileDialog.LOAD);
      fileDialog.setDirectory(new File(ClientSetting.SAVE_GAMES_FOLDER_PATH.value()).getPath());
      fileDialog.setFilenameFilter((dir, name) -> GameDataFileUtils.isCandidateFileName(name));
      fileDialog.setVisible(true);
      final String fileName = fileDialog.getFile();
      final String dirName = fileDialog.getDirectory();
      return (fileName == null) ? null : new File(dirName, fileName);
    }

    return GameRunner.showSaveGameFileChooser().orElse(null);
  }

  private void selectGameFile(final boolean saved) {
    // For some strange reason,
    // the only way to get a Mac OS X native-style file dialog
    // is to use an AWT FileDialog instead of a Swing JDialog
    if (saved) {
      final File file = selectGameFile();
      if ((file == null) || !file.exists()) {
        return;
      }

      Interruptibles.await(() -> GameRunner.newBackgroundTaskRunner().runInBackground("Loading savegame...", () -> {
        model.load(file, this);
        setOriginalPropertiesMap(model.getGameData());
      }));
    } else {
      try {
        final GameChooserEntry entry =
            GameChooser.chooseGame(JOptionPane.getFrameForComponent(this), model.getGameName());
        if (entry != null) {
          GameRunner.newBackgroundTaskRunner().runInBackground("Loading map...", () -> {
            if (!entry.isGameDataLoaded()) {
              try {
                entry.fullyParseGameData();
              } catch (final GameParseException e) {
                // TODO remove bad entries from the underlying model
                return;
              }
            }
            model.load(entry);
          });
          // warning: NPE check is not to protect against concurrency, another thread could still null out game data.
          // The NPE check is to protect against the case where there are errors loading game, in which case
          // we'll have a null game data.
          if (model.getGameData() != null) {
            setOriginalPropertiesMap(model.getGameData());
            // only for new games, not saved games, we set the default options, and set them only once
            // (the first time it is loaded)
            gamePropertiesCache.loadCachedGamePropertiesInto(model.getGameData());
          }
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
