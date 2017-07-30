package games.strategy.engine.framework.startup.mc;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.ui.NewGameChooser;
import games.strategy.engine.framework.ui.NewGameChooserEntry;
import games.strategy.engine.framework.ui.NewGameChooserModel;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.settings.ClientSetting;

public class GameSelectorModel extends Observable {
  /**
   * Returns the name of the directory within a map's directory where the game xml is held.
   * Example: returns "games" which would be the games folder of "triplea/maps/someMapFooBar/games"
   */
  public static final String DEFAULT_GAME_XML_DIRECTORY_NAME = "games";
  private GameData m_data = null;
  private String m_gameName = "";
  private String m_gameVersion = "";
  private String m_gameRound = "";
  private String m_fileName = "";
  private boolean m_canSelect = true;
  private boolean m_isHostHeadlessBot = false;
  // just for host bots, so we can get the actions for loading/saving games on the bots
  // from this model
  private ClientModel m_clientModelForHostBots = null;

  public GameSelectorModel() {
    setGameData(null);
    m_fileName = "";
  }

  public void resetGameDataToNull() {
    setGameData(null);
    m_fileName = "";
  }

  public void load(final GameData data, final String fileName) {
    setGameData(data);
    m_fileName = fileName;
  }

  public void load(final NewGameChooserEntry entry) {
    // we don't want to load anything if we are an older jar, because otherwise the user may get confused on which
    // version of triplea they
    // are using right now,
    // and then start a game with an older jar when they should be using the newest jar (we want user to be using the
    // normal default
    // [newest] triplea.jar for new games)
    if (ClientFileSystemHelper.areWeOldExtraJar()) {
      return;
    }
    m_fileName = entry.getLocation();
    setGameData(entry.getGameData());
    if (entry.getGameData() != null) {
      ClientSetting.DEFAULT_GAME_NAME_PREF.save(entry.getGameData().getGameName());
    }
    ClientSetting.DEFAULT_GAME_URI_PREF.save(entry.getURI().toString());
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
    final AtomicReference<String> gameName = new AtomicReference<>();
    try {
      // if the file name is xml, load it as a new game
      if (file.getName().toLowerCase().endsWith("xml")) {
        try (FileInputStream fis = new FileInputStream(file)) {
          newData = new GameParser(file.getAbsolutePath()).parse(fis, gameName, false);
        }
      } else {
        // try to load it as a saved game whatever the extension
        newData = GameDataManager.loadGame(file);
      }
      if (newData != null) {
        m_fileName = file.getName();
        setGameData(newData);
      }
    } catch (final EngineVersionException e) {
      System.out.println(e.getMessage());
    } catch (final Exception e) {
      String message = e.getMessage();
      if (message == null && e.getStackTrace() != null) {
        message = e.getClass().getName() + "  at  " + e.getStackTrace()[0].toString();
      }
      message = "Exception while parsing: " + file.getName() + " : "
          + (gameName.get() != null ? gameName.get() + " : " : "") + message;
      System.out.println(message);
      if (ui != null) {
        error(message + "\r\nPlease see console for full error log!", ui);
      }
    }
  }

  public GameData getGameData(final InputStream input) {
    final GameData newData;
    try {
      newData = GameDataManager.loadGame(input, null);
      if (newData != null) {
        return newData;
      }
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
    return null;
  }

  public synchronized GameData getGameData() {
    return m_data;
  }

  public boolean isSavedGame() {
    return !m_fileName.endsWith(".xml");
  }

  private static void error(final String message, final Component ui) {
    SwingUtilities.invokeLater(
        () -> JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ui), message, "Could not load Game",
            JOptionPane.ERROR_MESSAGE));
  }

  void setCanSelect(final boolean canSelect) {
    synchronized (this) {
      m_canSelect = canSelect;
    }
    notifyObs();
  }

  public synchronized boolean canSelect() {
    return m_canSelect;
  }

  void setIsHostHeadlessBot(final boolean isHostHeadlessBot) {
    synchronized (this) {
      m_isHostHeadlessBot = isHostHeadlessBot;
    }
    notifyObs();
  }

  public synchronized boolean isHostHeadlessBot() {
    return m_isHostHeadlessBot;
  }

  void setClientModelForHostBots(final ClientModel clientModel) {
    synchronized (this) {
      m_clientModelForHostBots = clientModel;
    }
  }

  public synchronized ClientModel getClientModelForHostBots() {
    return m_clientModelForHostBots;
  }

  /**
   * We dont have a gane data (ie we are a remote player and the data has not been sent yet), but
   * we still want to display game info.
   */
  public void clearDataButKeepGameInfo(final String gameName, final String gameRound, final String gameVersion) {
    synchronized (this) {
      m_data = null;
      m_gameName = gameName;
      m_gameRound = gameRound;
      m_gameVersion = gameVersion;
    }
    notifyObs();
  }

  public synchronized String getFileName() {
    if (m_data == null) {
      return "-";
    } else {
      return m_fileName;
    }
  }

  public synchronized String getGameName() {
    return m_gameName;
  }

  public synchronized String getGameRound() {
    return m_gameRound;
  }

  public synchronized String getGameVersion() {
    return m_gameVersion;
  }

  void setGameData(final GameData data) {
    synchronized (this) {
      if (data == null) {
        m_gameName = m_gameRound = m_gameVersion = "-";
      } else {
        m_gameName = data.getGameName();
        m_gameRound = "" + data.getSequence().getRound();
        m_gameVersion = data.getGameVersion().toString();
      }
      m_data = data;
    }
    notifyObs();
  }

  private void notifyObs() {
    super.setChanged();
    super.notifyObservers(m_data);
    super.clearChanged();
  }

  private void resetDefaultGame() {
    ClientSetting.DEFAULT_GAME_NAME_PREF.restoreToDefaultValue();
    ClientSetting.DEFAULT_GAME_URI_PREF.restoreToDefaultValue();
    ClientSetting.flush();
  }

  public void loadDefaultGame(final Component ui) {
    // clear out ai cached properties (this ended up being the best place to put it, as we have definitely left a game
    // at this point)
    ProAI.gameOverClearCache();
    new Thread(() -> loadDefaultGame(ui, false)).start();
  }

  /**
   * @param forceFactoryDefault
   *        - False is default behavior and causes the new game chooser model to be cleared (and refreshed if needed).
   *        True causes the default game preference to be reset, but the model does not get cleared/refreshed (because
   *        we only call with
   *        'true' if loading the user preferred map failed).
   */
  public void loadDefaultGame(final Component ui, final boolean forceFactoryDefault) {
    // load the previously saved value
    if (forceFactoryDefault) {
      // we don't refresh the game chooser model because we have just removed a bad map from it
      resetDefaultGame();
      ClientSetting.DEFAULT_GAME_URI_PREF.restoreToDefaultValue();
      ClientSetting.flush();
    }
    final String userPreferredDefaultGameUri = ClientSetting.DEFAULT_GAME_URI_PREF.value();

    // we don't want to load a game file by default that is not within the map folders we can load. (ie: if a previous
    // version of triplea
    // was using running a game within its root folder, we shouldn't open it)
    NewGameChooserEntry selectedGame = null;
    final String user = ClientFileSystemHelper.getUserRootFolder().toURI().toString();
    if (!forceFactoryDefault && userPreferredDefaultGameUri != null && userPreferredDefaultGameUri.length() > 0
        && userPreferredDefaultGameUri.contains(user)) {
      // if the user has a preferred URI, then we load it, and don't bother parsing or doing anything with the whole
      // game model list
      try {
        final URI defaultUri = new URI(userPreferredDefaultGameUri);
        selectedGame = new NewGameChooserEntry(defaultUri);
      } catch (final Exception e) {
        selectedGame = selectByName(ui, forceFactoryDefault);
        if (selectedGame == null) {
          return;
        }
      }
      if (!selectedGame.isGameDataLoaded()) {
        try {
          selectedGame.fullyParseGameData();
        } catch (final GameParseException e) {
          loadDefaultGame(ui, true);
          return;
        }
      }
    } else {
      selectedGame = selectByName(ui, forceFactoryDefault);
      if (selectedGame == null) {
        return;
      }
    }
    load(selectedGame);
  }

  private NewGameChooserEntry selectByName(final Component ui, final boolean forceFactoryDefault) {
    if (forceFactoryDefault) {
      ClientSetting.DEFAULT_GAME_NAME_PREF.restoreToDefaultValue();
      ClientSetting.flush();
    }
    final String userPreferredDefaultGameName = ClientSetting.DEFAULT_GAME_NAME_PREF.value();

    final NewGameChooserModel model = NewGameChooser.getNewGameChooserModel();
    NewGameChooserEntry selectedGame = model.findByName(userPreferredDefaultGameName);
    if (selectedGame == null) {
      selectedGame = model.findByName(userPreferredDefaultGameName);
    }
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
        // Load real default game...
        selectedGame.delayParseGameData();
        model.removeEntry(selectedGame);
        loadDefaultGame(ui, true);
        return null;
      }
    }
    return selectedGame;
  }
}
