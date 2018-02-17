package games.strategy.engine.framework.startup.mc;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Observable;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.ui.GameChooserEntry;
import games.strategy.engine.framework.ui.GameChooserModel;
import games.strategy.triplea.ai.proAI.ProAi;
import games.strategy.triplea.settings.ClientSetting;

public class GameSelectorModel extends Observable {
  private GameData gameData = null;
  private String gameName = "";
  private String gameVersion = "";
  private String gameRound = "";
  private String fileName = "";
  private boolean canSelect = true;
  private boolean isHostHeadlessBot = false;
  // just for host bots, so we can get the actions for loading/saving games on the bots
  // from this model
  private ClientModel clientModelForHostBots = null;

  public GameSelectorModel() {
    setGameData(null);
    fileName = "";
  }

  public void resetGameDataToNull() {
    setGameData(null);
    fileName = "";
  }

  public void load(final GameData data, final String fileName) {
    setGameData(data);
    this.fileName = fileName;
  }

  public void load(final GameChooserEntry entry) {
    fileName = entry.getLocation();
    setGameData(entry.getGameData());
    if (entry.getGameData() != null) {
      ClientSetting.DEFAULT_GAME_NAME_PREF.save(entry.getGameData().getGameName());
    }
    ClientSetting.DEFAULT_GAME_URI_PREF.save(entry.getUri().toString());
    ClientSetting.flush();
  }

  public void load(final File file, final Component ui) {
    if (!file.exists()) {
      if (ui == null) {
        System.out.println("Could not find file:" + file);
      } else {
        error("Could not find file:" + file, ui);
      }
      return;
    }
    if (file.isDirectory()) {
      if (ui == null) {
        System.out.println("Cannot load a directory:" + file);
      } else {
        error("Cannot load a directory:" + file, ui);
      }
      return;
    }
    final GameData newData;
    try {
      // if the file name is xml, load it as a new game
      if (file.getName().toLowerCase().endsWith("xml")) {
        try (InputStream fis = new FileInputStream(file)) {
          newData = GameParser.parse(file.getAbsolutePath(), fis);
        }
      } else {
        // try to load it as a saved game whatever the extension
        newData = GameDataManager.loadGame(file);
      }
      if (newData != null) {
        synchronized (this) {
          fileName = file.getName();
        }
        setGameData(newData);
      }
    } catch (final EngineVersionException e) {
      System.out.println(e.getMessage());
    } catch (final Exception e) {
      String message = e.getMessage();
      if (message == null && e.getStackTrace() != null) {
        message = e.getClass().getName() + "  at  " + e.getStackTrace()[0].toString();
      }
      message = "Exception while parsing: " + file.getName() + " : " + message;
      ClientLogger.logQuietly(message, e);
      if (ui != null) {
        error(message + "\r\nPlease see console for full error log!", ui);
      }
    }
  }

  public GameData getGameData(final InputStream input) {
    try {
      final GameData newData = GameDataManager.loadGame(input);
      if (newData != null) {
        return newData;
      }
    } catch (final IOException e) {
      ClientLogger.logQuietly("Failed to load game", e);
    }
    return null;
  }

  public synchronized GameData getGameData() {
    return gameData;
  }

  public synchronized boolean isSavedGame() {
    return !fileName.endsWith(".xml");
  }

  private static void error(final String message, final Component ui) {
    SwingUtilities.invokeLater(
        () -> JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ui), message, "Could not load Game",
            JOptionPane.ERROR_MESSAGE));
  }

  void setCanSelect(final boolean canSelect) {
    synchronized (this) {
      this.canSelect = canSelect;
    }
    notifyObs();
  }

  public synchronized boolean canSelect() {
    return canSelect;
  }

  void setIsHostHeadlessBot(final boolean isHostHeadlessBot) {
    synchronized (this) {
      this.isHostHeadlessBot = isHostHeadlessBot;
    }
    notifyObs();
  }

  public synchronized boolean isHostHeadlessBot() {
    return isHostHeadlessBot;
  }

  void setClientModelForHostBots(final ClientModel clientModel) {
    synchronized (this) {
      clientModelForHostBots = clientModel;
    }
  }

  public synchronized ClientModel getClientModelForHostBots() {
    return clientModelForHostBots;
  }

  /**
   * We dont have a gane data (ie we are a remote player and the data has not been sent yet), but
   * we still want to display game info.
   */
  public void clearDataButKeepGameInfo(final String gameName, final String gameRound, final String gameVersion) {
    synchronized (this) {
      gameData = null;
      this.gameName = gameName;
      this.gameRound = gameRound;
      this.gameVersion = gameVersion;
    }
    notifyObs();
  }

  public synchronized String getFileName() {
    return (gameData == null) ? "-" : fileName;
  }

  public synchronized String getGameName() {
    return gameName;
  }

  public synchronized String getGameRound() {
    return gameRound;
  }

  public synchronized String getGameVersion() {
    return gameVersion;
  }

  void setGameData(final GameData data) {
    synchronized (this) {
      if (data == null) {
        gameName = gameRound = gameVersion = "-";
      } else {
        gameName = data.getGameName();
        gameRound = "" + data.getSequence().getRound();
        gameVersion = data.getGameVersion().toString();
      }
      gameData = data;
    }
    notifyObs();
  }

  private void notifyObs() {
    super.setChanged();
    super.notifyObservers(gameData);
    super.clearChanged();
  }

  public void loadDefaultGame() {
    // clear out ai cached properties (this ended up being the best place to put it, as we have definitely left a game
    // at this point)
    ProAi.gameOverClearCache();
    new Thread(() -> loadDefaultGame(false)).start();
  }

  /**
   * @param forceFactoryDefault
   *        - False is default behavior and causes the new game chooser model to be cleared (and refreshed if needed).
   *        True causes the default game preference to be reset, but the model does not get cleared/refreshed (because
   *        we only call with
   *        'true' if loading the user preferred map failed).
   */
  public void loadDefaultGame(final boolean forceFactoryDefault) {
    // load the previously saved value
    if (forceFactoryDefault) {
      // we don't refresh the game chooser model because we have just removed a bad map from it
      ClientSetting.DEFAULT_GAME_URI_PREF.save(ClientSetting.DEFAULT_GAME_URI_PREF.defaultValue);
      ClientSetting.flush();
    }
    final String userPreferredDefaultGameUri = ClientSetting.DEFAULT_GAME_URI_PREF.value();

    // we don't want to load a game file by default that is not within the map folders we can load. (ie: if a previous
    // version of triplea
    // was using running a game within its root folder, we shouldn't open it)
    GameChooserEntry selectedGame;
    final String user = ClientFileSystemHelper.getUserRootFolder().toURI().toString();
    if (!forceFactoryDefault && userPreferredDefaultGameUri != null && userPreferredDefaultGameUri.length() > 0
        && userPreferredDefaultGameUri.contains(user)) {
      // if the user has a preferred URI, then we load it, and don't bother parsing or doing anything with the whole
      // game model list
      try {
        final URI defaultUri = new URI(userPreferredDefaultGameUri);
        selectedGame = new GameChooserEntry(defaultUri);
      } catch (final Exception e) {
        selectedGame = selectByName(forceFactoryDefault);
        if (selectedGame == null) {
          return;
        }
      }
      if (!selectedGame.isGameDataLoaded()) {
        try {
          selectedGame.fullyParseGameData();
        } catch (final GameParseException e) {
          loadDefaultGame(true);
          return;
        }
      }
    } else {
      selectedGame = selectByName(forceFactoryDefault);
      if (selectedGame == null) {
        return;
      }
    }
    load(selectedGame);
  }

  private GameChooserEntry selectByName(final boolean forceFactoryDefault) {
    if (forceFactoryDefault) {
      ClientSetting.DEFAULT_GAME_NAME_PREF.save(ClientSetting.DEFAULT_GAME_NAME_PREF.defaultValue);
      ClientSetting.flush();
    }
    final String userPreferredDefaultGameName = ClientSetting.DEFAULT_GAME_NAME_PREF.value();

    final GameChooserModel model = new GameChooserModel();
    GameChooserEntry selectedGame = model.findByName(userPreferredDefaultGameName)
        .orElse(null);

    if (selectedGame == null && model.size() > 0) {
      selectedGame = model.get(0);
    }
    if (selectedGame == null) {
      return null;
    }
    if (!selectedGame.isGameDataLoaded()) {
      try {
        selectedGame.fullyParseGameData();
      } catch (final GameParseException e) {
        model.removeEntry(selectedGame);
        loadDefaultGame(true);
        return null;
      }
    }
    return selectedGame;
  }
}
