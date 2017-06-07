package games.strategy.engine.framework;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.debug.ErrorConsole;
import games.strategy.engine.ClientContext;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.download.MapDownloadController;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.framework.system.Memory;
import games.strategy.engine.framework.systemcheck.LocalSystemChecker;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.net.Messengers;
import games.strategy.triplea.settings.SystemPreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;
import games.strategy.ui.SwingComponents;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Version;

/**
 * GameRunner - The entrance class with the main method.
 * In this class commonly used constants are getting defined and the Game is being launched
 */
public class GameRunner {

  public static final String TRIPLEA_HEADLESS = "triplea.headless";
  public static final String TRIPLEA_GAME_HOST_CONSOLE_PROPERTY = "triplea.game.host.console";
  public static final int LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM = 21600;
  public static final int LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT = 2 * LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM;
  public static final String NO_REMOTE_REQUESTS_ALLOWED = "noRemoteRequestsAllowed";

  // not arguments:
  public static final int PORT = 3300;
  private static final String DELAYED_PARSING = "DelayedParsing";
  // do not include this in the getProperties list. they are only for loading an old savegame.
  public static final String OLD_EXTENSION = ".old";
  // argument options below:
  public static final String TRIPLEA_GAME_PROPERTY = "triplea.game";
  public static final String TRIPLEA_SERVER_PROPERTY = "triplea.server";
  public static final String TRIPLEA_CLIENT_PROPERTY = "triplea.client";
  public static final String TRIPLEA_HOST_PROPERTY = "triplea.host";
  public static final String TRIPLEA_PORT_PROPERTY = "triplea.port";
  public static final String TRIPLEA_NAME_PROPERTY = "triplea.name";
  public static final String TRIPLEA_SERVER_PASSWORD_PROPERTY = "triplea.server.password";
  public static final String TRIPLEA_STARTED = "triplea.started";
  public static final String LOBBY_HOST = "triplea.lobby.host";
  public static final String LOBBY_GAME_COMMENTS = "triplea.lobby.game.comments";
  public static final String LOBBY_GAME_HOSTED_BY = "triplea.lobby.game.hostedBy";
  public static final String LOBBY_GAME_SUPPORT_EMAIL = "triplea.lobby.game.supportEmail";
  public static final String LOBBY_GAME_SUPPORT_PASSWORD = "triplea.lobby.game.supportPassword";
  public static final String LOBBY_GAME_RECONNECTION = "triplea.lobby.game.reconnection";
  public static final String TRIPLEA_ENGINE_VERSION_BIN = "triplea.engine.version.bin";
  private static final String TRIPLEA_DO_NOT_CHECK_FOR_UPDATES = "triplea.doNotCheckForUpdates";
  public static final String TRIPLEA_LOBBY_PORT_PROPERTY = "triplea.lobby.port";

  public static final String TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME = "triplea.server.startGameSyncWaitTime";
  public static final String TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME = "triplea.server.observerJoinWaitTime";
  // non-commandline-argument-properties (for preferences)
  // first time we've run this version of triplea?
  private static final String SYSTEM_INI = "system.ini";
  public static final int MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME = 20;
  private static final int DEFAULT_CLIENT_GAMEDATA_LOAD_GRACE_TIME =
      Math.max(MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME, 25);
  // need time for network transmission of a large game data
  public static final int MINIMUM_SERVER_OBSERVER_JOIN_WAIT_TIME = MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME + 10;
  private static final int DEFAULT_SERVER_OBSERVER_JOIN_WAIT_TIME =
      Math.max(DEFAULT_CLIENT_GAMEDATA_LOAD_GRACE_TIME + 10, 35);
  public static final int ADDITIONAL_SERVER_ERROR_DISCONNECTION_WAIT_TIME = 10;
  public static final int MINIMUM_SERVER_START_GAME_SYNC_WAIT_TIME =
      MINIMUM_SERVER_OBSERVER_JOIN_WAIT_TIME + ADDITIONAL_SERVER_ERROR_DISCONNECTION_WAIT_TIME + 110;
  private static final int DEFAULT_SERVER_START_GAME_SYNC_WAIT_TIME =
      Math.max(Math.max(MINIMUM_SERVER_START_GAME_SYNC_WAIT_TIME, 900),
          DEFAULT_SERVER_OBSERVER_JOIN_WAIT_TIME + ADDITIONAL_SERVER_ERROR_DISCONNECTION_WAIT_TIME + 110);

  public static final String MAP_FOLDER = "mapFolder";

  private static String[] COMMAND_LINE_ARGS =
      {TRIPLEA_GAME_PROPERTY, TRIPLEA_SERVER_PROPERTY, TRIPLEA_CLIENT_PROPERTY, TRIPLEA_HOST_PROPERTY,
          TRIPLEA_PORT_PROPERTY, TRIPLEA_NAME_PROPERTY, TRIPLEA_SERVER_PASSWORD_PROPERTY, TRIPLEA_STARTED,
          TRIPLEA_LOBBY_PORT_PROPERTY,
          LOBBY_HOST, LOBBY_GAME_COMMENTS, LOBBY_GAME_HOSTED_BY, TRIPLEA_ENGINE_VERSION_BIN, HttpProxy.PROXY_HOST,
          HttpProxy.PROXY_PORT, TRIPLEA_DO_NOT_CHECK_FOR_UPDATES, Memory.TRIPLEA_MEMORY_SET, MAP_FOLDER};



  /**
   * Launches the "main" TripleA gui enabled game client.
   * No args will launch a client, additional args can be supplied to specify additional behavior.
   * Warning: game engine code invokes this method to spawn new game clients.
   */
  public static void main(final String[] args) {
    ErrorConsole.getConsole();
    if (!ArgParser.handleCommandLineArgs(args, COMMAND_LINE_ARGS)) {
      usage();
      return;
    }
    // do after we handle command line args
    Memory.checkForMemoryXMX();

    SwingUtilities.invokeLater(LookAndFeel::setupLookAndFeel);
    showMainFrame();
    new Thread(GameRunner::setupLogging).start();
    HttpProxy.setupProxies();
    new Thread(GameRunner::checkLocalSystem).start();
    new Thread(GameRunner::checkForUpdates).start();

    final String version = System.getProperty(TRIPLEA_ENGINE_VERSION_BIN);
    if (version != null && version.length() > 0) {
      final Version testVersion;
      try {
        testVersion = new Version(version);
        // if successful we don't do anything
        System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + version);
        if (!ClientContext.engineVersion().getVersion().equals(testVersion, false)) {
          System.out.println("Current Engine version in use: " + ClientContext.engineVersion());
        }
      } catch (final Exception e) {
        System.getProperties().setProperty(TRIPLEA_ENGINE_VERSION_BIN, ClientContext.engineVersion().toString());
        System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + ClientContext.engineVersion());
      }
    } else {
      System.getProperties().setProperty(TRIPLEA_ENGINE_VERSION_BIN, ClientContext.engineVersion().toString());
      System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + ClientContext.engineVersion());
    }
  }

  private static void usage() {
    System.out.println("\nUsage and Valid Arguments:\n"
        + "   " + TRIPLEA_GAME_PROPERTY + "=<FILE_NAME>\n"
        + "   " + TRIPLEA_GAME_HOST_CONSOLE_PROPERTY + "=<true/false>\n"
        + "   " + TRIPLEA_SERVER_PROPERTY + "=true\n"
        + "   " + TRIPLEA_PORT_PROPERTY + "=<PORT>\n"
        + "   " + TRIPLEA_NAME_PROPERTY + "=<PLAYER_NAME>\n"
        + "   " + LOBBY_HOST + "=<LOBBY_HOST>\n"
        + "   " + LobbyServer.TRIPLEA_LOBBY_PORT_PROPERTY + "=<LOBBY_PORT>\n"
        + "   " + LOBBY_GAME_COMMENTS + "=<LOBBY_GAME_COMMENTS>\n"
        + "   " + LOBBY_GAME_HOSTED_BY + "=<LOBBY_GAME_HOSTED_BY>\n"
        + "   " + LOBBY_GAME_SUPPORT_EMAIL + "=<youremail@emailprovider.com>\n"
        + "   " + LOBBY_GAME_SUPPORT_PASSWORD + "=<password for remote actions, such as remote stop game>\n"
        + "   " + LOBBY_GAME_RECONNECTION + "=<seconds between refreshing lobby connection [min "
        + LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM + "]>\n"
        + "   " + TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME + "=<seconds to wait for all clients to start the game>\n"
        + "   " + TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME + "=<seconds to wait for an observer joining the game>\n"
        + "   " + MAP_FOLDER + "=mapFolder"
        + "\n"
        + "   You must start the Name and HostedBy with \"Bot\".\n"
        + "   Game Comments must have this string in it: \"automated_host\".\n"
        + "   You must include a support email for your host, so that you can be alerted by lobby admins when your "
        + "host has an error."
        + " (For example they may email you when your host is down and needs to be restarted.)\n"
        + "   Support password is a remote access password that will allow lobby admins to remotely take the "
        + "following actions: ban player, stop game, shutdown server."
        + " (Please email this password to one of the lobby moderators, or private message an admin on the "
        + "TripleaWarClub.org website forum.)\n");
  }

  private static void showMainFrame() {
    SwingUtilities.invokeLater(() -> {
      final MainFrame frame = new MainFrame();
      frame.requestFocus();
      frame.toFront();
      frame.setVisible(true);
    });
  }

  private static void checkLocalSystem() {
    final LocalSystemChecker localSystemChecker = new LocalSystemChecker();
    final Collection<Exception> exceptions = localSystemChecker.getExceptions();
    if (!exceptions.isEmpty()) {
      final String msg = String.format(
          "Warning!! %d system checks failed. Some game features may not be available or may not work correctly.\n%s",
          exceptions.size(), localSystemChecker.getStatusMessage());
      ClientLogger.logError(msg, exceptions);
    }
  }



  private static void setupLogging() {
    Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
      @Override
      protected void dispatchEvent(AWTEvent newEvent) {
        try {
          super.dispatchEvent(newEvent);
          // This ensures, that all exceptions/errors inside any swing framework (like substance) are logged correctly
        } catch (Throwable t) {
          ClientLogger.logError(t);
          throw t;
        }
      }
    });
  }


  /**
   * Returns the property set parsed from a 'system.ini' file (as found at the project root).
   */
  public static Properties getSystemIni() {
    final Properties rVal = new Properties();
    final File systemIni = new File(ClientFileSystemHelper.getRootFolder(), SYSTEM_INI);
    if (systemIni.exists()) {
      try (FileInputStream fis = new FileInputStream(systemIni)) {
        rVal.load(fis);
      } catch (final IOException e) {
        ClientLogger.logQuietly(e);
      }
    }
    return rVal;
  }

  /**
   * Writes a set of properties to the system.ini file.
   */
  public static void writeSystemIni(final Properties properties) {
    final Properties toWrite;

    toWrite = getSystemIni();
    for (final Entry<Object, Object> entry : properties.entrySet()) {
      toWrite.put(entry.getKey(), entry.getValue());
    }

    final File systemIni = new File(ClientFileSystemHelper.getRootFolder(), SYSTEM_INI);

    try (FileOutputStream fos = new FileOutputStream(systemIni)) {
      toWrite.store(fos, SYSTEM_INI);
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
  }


  public static boolean getDelayedParsing() {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
    return pref.getBoolean(DELAYED_PARSING, true);
  }

  public static void setDelayedParsing(final boolean delayedParsing) {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
    pref.putBoolean(DELAYED_PARSING, delayedParsing);
    try {
      pref.sync();
    } catch (final BackingStoreException e) {
      ClientLogger.logQuietly(e);
    }
  }

  public static int getServerStartGameSyncWaitTime() {
    return Math.max(MINIMUM_SERVER_START_GAME_SYNC_WAIT_TIME, Preferences.userNodeForPackage(GameRunner.class)
        .getInt(TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME, DEFAULT_SERVER_START_GAME_SYNC_WAIT_TIME));
  }

  public static void resetServerStartGameSyncWaitTime() {
    setServerStartGameSyncWaitTime(DEFAULT_SERVER_START_GAME_SYNC_WAIT_TIME);
  }

  public static void setServerStartGameSyncWaitTime(final int seconds) {
    final int wait = Math.max(MINIMUM_SERVER_START_GAME_SYNC_WAIT_TIME, seconds);
    if (wait == getServerStartGameSyncWaitTime()) {
      return;
    }
    SystemPreferences.put(SystemPreferenceKey.TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME, wait);
  }

  public static int getServerObserverJoinWaitTime() {
    return Math.max(MINIMUM_SERVER_OBSERVER_JOIN_WAIT_TIME, Preferences.userNodeForPackage(GameRunner.class)
        .getInt(TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME, DEFAULT_SERVER_OBSERVER_JOIN_WAIT_TIME));
  }

  public static void resetServerObserverJoinWaitTime() {
    setServerObserverJoinWaitTime(DEFAULT_SERVER_OBSERVER_JOIN_WAIT_TIME);
  }

  public static void setServerObserverJoinWaitTime(final int seconds) {
    final int wait = Math.max(MINIMUM_SERVER_OBSERVER_JOIN_WAIT_TIME, seconds);
    if (wait == getServerObserverJoinWaitTime()) {
      return;
    }
    SystemPreferences.put(SystemPreferenceKey.TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME, wait);
  }

  private static void checkForUpdates() {
    new Thread(() -> {
      // do not check if we are the old extra jar. (a jar kept for backwards compatibility only)
      if (ClientFileSystemHelper.areWeOldExtraJar()) {
        return;
      }
      if (System.getProperty(TRIPLEA_SERVER_PROPERTY, "false").equalsIgnoreCase("true")) {
        return;
      }
      if (System.getProperty(TRIPLEA_CLIENT_PROPERTY, "false").equalsIgnoreCase("true")) {
        return;
      }
      if (System.getProperty(TRIPLEA_DO_NOT_CHECK_FOR_UPDATES, "false").equalsIgnoreCase("true")) {
        return;
      }

      // if we are joining a game online, or hosting, or loading straight into a savegame, do not check
      final String fileName = System.getProperty(TRIPLEA_GAME_PROPERTY, "");
      if (fileName.trim().length() > 0) {
        return;
      }

      boolean busy = false;
      busy = checkForTutorialMap();
      if (!busy) {
        busy = checkForLatestEngineVersionOut();
      }
      if (!busy) {
        busy = checkForUpdatedMaps();
      }
    }, "Checking Latest TripleA Engine Version").start();
  }

  /**
   * @return true if we are out of date or this is the first time this triplea has ever been run.
   */
  private static boolean checkForLatestEngineVersionOut() {
    try {
      final boolean firstTimeThisVersion =
          SystemPreferences.get(SystemPreferenceKey.TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY, true);
      // check at most once per 2 days (but still allow a 'first run message' for a new version of triplea)
      final Calendar calendar = Calendar.getInstance();
      final int year = calendar.get(Calendar.YEAR);
      final int day = calendar.get(Calendar.DAY_OF_YEAR);
      // format year:day
      final String lastCheckTime = SystemPreferences.get(SystemPreferenceKey.TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE, "");
      if (!firstTimeThisVersion && lastCheckTime != null && lastCheckTime.trim().length() > 0) {
        final String[] yearDay = lastCheckTime.split(":");
        if (Integer.parseInt(yearDay[0]) >= year && Integer.parseInt(yearDay[1]) + 1 >= day) {
          return false;
        }
      }

      SystemPreferences.put(SystemPreferenceKey.TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE, year + ":" + day);

      final EngineVersionProperties latestEngineOut = EngineVersionProperties.contactServerForEngineVersionProperties();
      if (latestEngineOut == null) {
        return false;
      }
      if (ClientContext.engineVersion().getVersion().isLessThan(latestEngineOut.getLatestVersionOut())) {
        SwingUtilities
            .invokeLater(() -> EventThreadJOptionPane.showMessageDialog(null, latestEngineOut.getOutOfDateComponent(),
                "Please Update TripleA", JOptionPane.INFORMATION_MESSAGE, false, new CountDownLatchHandler(true)));
        return true;
      }
    } catch (final Exception e) {
      System.out.println("Error while checking for engine updates: " + e.getMessage());
    }
    return false;
  }

  private static boolean checkForTutorialMap() {
    final MapDownloadController mapDownloadController = ClientContext.mapDownloadController();
    final boolean promptToDownloadTutorialMap = mapDownloadController.shouldPromptToDownloadTutorialMap();
    mapDownloadController.preventPromptToDownloadTutorialMap();
    if (!promptToDownloadTutorialMap) {
      return false;
    }

    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("<html>");
    messageBuilder.append("Would you like to download the tutorial map?<br>");
    messageBuilder.append("<br>");
    messageBuilder.append("(You can always download it later using the Download Maps<br>");
    messageBuilder.append("command if you don't want to do it now.)");
    messageBuilder.append("</html>");
    SwingComponents.promptUser("Welcome to TripleA", messageBuilder.toString(), () -> {
      DownloadMapsWindow.showDownloadMapsWindowAndDownload("Tutorial");
    });
    return true;
  }

  /**
   * @return true if we have any out of date maps.
   */
  private static boolean checkForUpdatedMaps() {
    MapDownloadController downloadController = ClientContext.mapDownloadController();
    return downloadController.checkDownloadedMapsAreLatest();
  }


  public static Image getGameIcon(final Window frame) {
    Image img = null;
    try {
      img = frame.getToolkit().getImage(GameRunner.class.getResource("ta_icon.png"));
    } catch (final Exception ex) {
      ClientLogger.logError("ta_icon.png not loaded", ex);
    }
    final MediaTracker tracker = new MediaTracker(frame);
    tracker.addImage(img, 0);
    try {
      tracker.waitForAll();
    } catch (final InterruptedException ex) {
      ClientLogger.logQuietly(ex);
    }
    return img;
  }

  public static void startNewTripleA(final Long maxMemory) {
    startGame(System.getProperty(GameRunner.TRIPLEA_GAME_PROPERTY), null, maxMemory);
  }

  static void startGame(final String savegamePath, final String classpath, final Long maxMemory) {
    final List<String> commands = new ArrayList<>();
    if (maxMemory != null && maxMemory > (32 * 1024 * 1024)) {
      ProcessRunnerUtil.populateBasicJavaArgs(commands, classpath, maxMemory);
    } else {
      ProcessRunnerUtil.populateBasicJavaArgs(commands, classpath);
    }
    if (savegamePath != null && savegamePath.length() > 0) {
      commands.add("-D" + GameRunner.TRIPLEA_GAME_PROPERTY + "=" + savegamePath);
    }
    // add in any existing command line items
    for (final String property : GameRunner.COMMAND_LINE_ARGS) {
      // we add game property above, and we add version bin in the populateBasicJavaArgs
      if (GameRunner.TRIPLEA_GAME_PROPERTY.equals(property)
          || GameRunner.TRIPLEA_ENGINE_VERSION_BIN.equals(property)) {
        continue;
      }
      final String value = System.getProperty(property);
      if (value != null) {
        commands.add("-D" + property + "=" + value);
      } else if (GameRunner.LOBBY_HOST.equals(property) || LobbyServer.TRIPLEA_LOBBY_PORT_PROPERTY.equals(property)
          || GameRunner.LOBBY_GAME_HOSTED_BY.equals(property)) {
        // for these 3 properties, we clear them after hosting, but back them up.
        final String oldValue = System.getProperty(property + GameRunner.OLD_EXTENSION);
        if (oldValue != null) {
          commands.add("-D" + property + "=" + oldValue);
        }
      }
    }
    // classpath for main
    commands.add(GameRunner.class.getName());
    ProcessRunnerUtil.exec(commands);
  }

  public static void hostGame(final int port, final String playerName, final String comments, final String password,
      final Messengers messengers) {
    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands);
    commands.add("-D" + TRIPLEA_SERVER_PROPERTY + "=true");
    commands.add("-D" + TRIPLEA_PORT_PROPERTY + "=" + port);
    commands.add("-D" + TRIPLEA_NAME_PROPERTY + "=" + playerName);
    commands.add("-D" + LOBBY_HOST + "="
        + messengers.getMessenger().getRemoteServerSocketAddress().getAddress().getHostAddress());
    commands
        .add("-D" + LobbyServer.TRIPLEA_LOBBY_PORT_PROPERTY + "="
            + messengers.getMessenger().getRemoteServerSocketAddress().getPort());
    commands.add("-D" + LOBBY_GAME_COMMENTS + "=" + comments);
    commands.add("-D" + LOBBY_GAME_HOSTED_BY + "=" + messengers.getMessenger().getLocalNode().getName());
    if (password != null && password.length() > 0) {
      commands.add("-D" + TRIPLEA_SERVER_PASSWORD_PROPERTY + "=" + password);
    }
    final String fileName = System.getProperty(TRIPLEA_GAME_PROPERTY, "");
    if (fileName.length() > 0) {
      commands.add("-D" + TRIPLEA_GAME_PROPERTY + "=" + fileName);
    }
    final String javaClass = GameRunner.class.getName();
    commands.add(javaClass);
    ProcessRunnerUtil.exec(commands);
  }

  public static void joinGame(final GameDescription description, final Messengers messengers, final Container parent) {
    final GameDescription.GameStatus status = description.getStatus();
    if (GameDescription.GameStatus.LAUNCHING.equals(status)) {
      return;
    }
    final Version engineVersionOfGameToJoin = new Version(description.getEngineVersion());
    String newClassPath = null;
    final boolean sameVersion = ClientContext.engineVersion().getVersion().equals(engineVersionOfGameToJoin);
    if (!sameVersion) {
      try {
        newClassPath = findOldJar(engineVersionOfGameToJoin, false);
      } catch (final Exception e) {
        if (ClientFileSystemHelper.areWeOldExtraJar()) {
          JOptionPane.showMessageDialog(parent,
              "<html>Please run the default TripleA and try joining the online lobby for it instead. "
                  + "<br>This TripleA engine is old and kept only for backwards compatibility and can only play with "
                  + "people using the exact same version as this one. "
                  + "<br><br>Host is using a different engine than you, and cannot find correct engine: "
                  + engineVersionOfGameToJoin.toStringFull("_") + "</html>",
              "Correct TripleA Engine Not Found", JOptionPane.WARNING_MESSAGE);
        } else {
          JOptionPane.showMessageDialog(parent,
              "Host is using a different engine than you, and cannot find correct engine: "
                  + engineVersionOfGameToJoin.toStringFull("_"),
              "Correct TripleA Engine Not Found", JOptionPane.WARNING_MESSAGE);
        }
        return;
      }
      // ask user if we really want to do this?
      final String messageString = "<html>This TripleA engine is version " + ClientContext.engineVersion().getVersion()
          + " and you are trying to join a game made with version " + engineVersionOfGameToJoin.toString()
          + "<br>However, this TripleA can only play with engines that are the exact same version as itself (x_x_x_x)."
          + "<br><br>TripleA now comes with older engines included with it, and has found the engine used by the host. "
          + "This is a new feature and is in 'beta' stage."
          + "<br>It will attempt to run a new instance of TripleA using the older engine jar file, and this instance "
          + "will join the host's game."
          + "<br>Your current instance will not be closed. Please report any bugs or issues."
          + "<br><br>Do you wish to continue?</html>";
      final int answer = JOptionPane.showConfirmDialog(null, messageString, "Run old jar to join hosted game?",
          JOptionPane.YES_NO_OPTION);
      if (answer != JOptionPane.YES_OPTION) {
        return;
      }
    }
    joinGame(description.getPort(), description.getHostedBy().getAddress().getHostAddress(), newClassPath, messengers);
  }

  // newClassPath can be null
  private static void joinGame(final int port, final String hostAddressIp, final String newClassPath,
      final Messengers messengers) {
    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands, newClassPath);
    final String prefix = "-D";
    commands.add(prefix + TRIPLEA_CLIENT_PROPERTY + "=true");
    commands.add(prefix + TRIPLEA_PORT_PROPERTY + "=" + port);
    commands.add(prefix + TRIPLEA_HOST_PROPERTY + "=" + hostAddressIp);
    commands.add(prefix + TRIPLEA_NAME_PROPERTY + "=" + messengers.getMessenger().getLocalNode().getName());
    commands.add(GameRunner.class.getName());
    ProcessRunnerUtil.exec(commands);
  }

  static String findOldJar(final Version oldVersionNeeded, final boolean ignoreMicro) throws IOException {
    if (ClientContext.engineVersion().getVersion().equals(oldVersionNeeded, ignoreMicro)) {
      return System.getProperty("java.class.path");
    }
    // first, see if the default/main triplea can run it
    if (ClientFileSystemHelper.areWeOldExtraJar()) {
      final String version = System.getProperty(GameRunner.TRIPLEA_ENGINE_VERSION_BIN);
      if (version != null && version.length() > 0) {
        Version defaultVersion = null;
        try {
          defaultVersion = new Version(version);
        } catch (final Exception e) {
          // nothing, just continue
        }
        if (defaultVersion != null) {
          if (defaultVersion.equals(oldVersionNeeded, ignoreMicro)) {
            final String jarName = "triplea.jar";
            // windows is in 'bin' folder, mac is in 'Java' folder.
            File binFolder = new File(ClientFileSystemHelper.getRootFolder(), "bin/");
            if (!binFolder.exists()) {
              binFolder = new File(ClientFileSystemHelper.getRootFolder(), "Java/");
            }
            if (binFolder.exists()) {
              final File[] files = binFolder.listFiles();
              if (files == null) {
                throw new IOException("Cannot find 'bin' engine jars folder");
              }
              File ourBinJar = null;
              for (final File f : Arrays.asList(files)) {
                if (!f.exists()) {
                  continue;
                }
                final String jarPath = f.getCanonicalPath();
                if (jarPath.contains(jarName)) {
                  ourBinJar = f;
                  break;
                }
              }
              if (ourBinJar == null) {
                throw new IOException(
                    "Cannot find 'bin' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
              }
              final String newClassPath = ourBinJar.getCanonicalPath();
              if (newClassPath.length() <= 0) {
                throw new IOException(
                    "Cannot find 'bin' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
              }
              return newClassPath;
            } else {
              System.err.println("Cannot find 'bin' or 'Java' folder, where main triplea.jar should be.");
            }
          }
        }
      }
    }
    // so, what we do here is try to see if our installed copy of triplea includes older jars with it that are the same
    // engine as was used
    // for this savegame, and if so try to run it
    // System.out.println("System classpath: " + System.getProperty("java.class.path"));
    // we don't care what the last (micro) number is of the version number. example: triplea 1.5.2.1 can open 1.5.2.0
    // savegames.
    final String jarName = "triplea_" + oldVersionNeeded.toStringFull("_", ignoreMicro);
    final File oldJarsFolder = new File(ClientFileSystemHelper.getRootFolder(), "old/");
    if (!oldJarsFolder.exists()) {
      throw new IOException("Cannot find 'old' engine jars folder");
    }
    final File[] files = oldJarsFolder.listFiles();
    if (files == null) {
      throw new IOException("Cannot find 'old' engine jars folder");
    }
    File ourOldJar = null;
    for (final File f : Arrays.asList(files)) {
      if (!f.exists()) {
        continue;
      }
      // final String jarPath = f.getCanonicalPath();
      final String name = f.getName();
      if (name.contains(jarName) && name.contains(".jar")) {
        ourOldJar = f;
        break;
      }
    }
    if (ourOldJar == null) {
      throw new IOException("Cannot find 'old' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
    }
    final String newClassPath = ourOldJar.getCanonicalPath();
    if (newClassPath.length() <= 0) {
      throw new IOException("Cannot find 'old' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
    }
    return newClassPath;
  }


  public static void exitGameIfFinished() {
    SwingUtilities.invokeLater(() -> {
      boolean allFramesClosed = true;
      for (Frame f : Frame.getFrames()) {
        if (f.isVisible()) {
          allFramesClosed = false;
          break;
        }
      }
      if (allFramesClosed) {
        System.exit(0);
      }
    });
  }

}
