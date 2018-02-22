package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.framework.ArgParser.CliProperties.DO_NOT_CHECK_FOR_UPDATES;
import static games.strategy.engine.framework.ArgParser.CliProperties.GAME_HOST_CONSOLE;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_HOSTED_BY;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_RECONNECTION;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_SUPPORT_EMAIL;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_SUPPORT_PASSWORD;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_HOST;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_PORT;
import static games.strategy.engine.framework.ArgParser.CliProperties.MAP_FOLDER;
import static games.strategy.engine.framework.ArgParser.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_HOST;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_MAP_DOWNLOAD;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_SERVER;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_STARTED;

import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.triplea.client.ui.javafx.TripleA;

import games.strategy.debug.ClientLogger;
import games.strategy.debug.ErrorConsole;
import games.strategy.debug.ErrorMessage;
import games.strategy.debug.LoggingConfiguration;
import games.strategy.engine.ClientContext;
import games.strategy.engine.GameEngineVersion;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.download.MapDownloadController;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.MainPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.framework.systemcheck.LocalSystemChecker;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.net.Messengers;
import games.strategy.triplea.ai.proAI.ProAi;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.ProgressWindow;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Interruptibles;
import games.strategy.util.Version;
import javafx.application.Application;

/**
 * GameRunner - The entrance class with the main method.
 * In this class commonly used constants are getting defined and the Game is being launched
 */
public class GameRunner {

  public static final String TRIPLEA_HEADLESS = "triplea.headless";
  public static final int LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM = 21600;
  public static final int LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT = 2 * LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM;
  public static final String NO_REMOTE_REQUESTS_ALLOWED = "noRemoteRequestsAllowed";

  // not arguments:
  public static final int PORT = 3300;
  // do not include this in the getProperties list. they are only for loading an old savegame.
  public static final String OLD_EXTENSION = ".old";

  public static final int MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME = 20;

  private static final GameSelectorModel gameSelectorModel = new GameSelectorModel();
  private static final SetupPanelModel setupPanelModel = new SetupPanelModel(gameSelectorModel);
  private static JFrame mainFrame;

  private static final Set<String> COMMAND_LINE_ARGS = new HashSet<>(Arrays.asList(
      TRIPLEA_GAME, TRIPLEA_MAP_DOWNLOAD, TRIPLEA_SERVER, TRIPLEA_CLIENT,
      TRIPLEA_HOST, TRIPLEA_PORT, TRIPLEA_NAME, SERVER_PASSWORD,
      TRIPLEA_STARTED, LOBBY_PORT, LOBBY_HOST, LOBBY_GAME_COMMENTS, LOBBY_GAME_HOSTED_BY,
      DO_NOT_CHECK_FOR_UPDATES, MAP_FOLDER));


  /**
   * Launches the "main" TripleA gui enabled game client.
   * No args will launch a client, additional args can be supplied to specify additional behavior.
   * Warning: game engine code invokes this method to spawn new game clients.
   */
  public static void main(final String[] args) throws InterruptedException {
    LoggingConfiguration.initialize();
    ClientSetting.initialize();

    if (!ClientSetting.USE_EXPERIMENTAL_JAVAFX_UI.booleanValue()) {
      SwingAction.invokeAndWait(() -> {
        LookAndFeel.setupLookAndFeel();
        ErrorConsole.createConsole();
        ErrorMessage.INSTANCE.init();
      });
    }

    if (!new ArgParser(COMMAND_LINE_ARGS).handleCommandLineArgs(args)) {
      usage();
      return;
    }

    if (HttpProxy.isUsingSystemProxy()) {
      HttpProxy.updateSystemProxy();
    }

    final Version engineVersion = ClientContext.engineVersion();
    System.out.println("TripleA engine version " + engineVersion.getExactVersion());

    if (ClientSetting.USE_EXPERIMENTAL_JAVAFX_UI.booleanValue()) {
      Application.launch(TripleA.class, args);
    } else {
      SwingUtilities.invokeLater(() -> {
        setupPanelModel.showSelectType();
        mainFrame = newMainFrame();
      });

      showMainFrame();
      new Thread(GameRunner::checkLocalSystem).start();
      new Thread(GameRunner::checkForUpdates).start();
    }
  }

  private static JFrame newMainFrame() {
    final JFrame frame = new JFrame("TripleA");

    frame.add(new MainPanel(setupPanelModel));
    frame.pack();

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setIconImage(getGameIcon(frame));
    frame.setLocationRelativeTo(null);

    return frame;
  }

  /**
   * Creates a new modeless dialog with the specified title whose parent is the main frame window.
   *
   * @param title The dialog title.
   *
   * @return A new modeless dialog.
   */
  public static JDialog newDialog(final String title) {
    checkNotNull(title);

    return new JDialog(mainFrame, title);
  }

  public static FileDialog newFileDialog() {
    return new FileDialog(mainFrame);
  }

  /**
   * Opens a Swing FileChooser menu.
   *
   * @return Empty optional if dialog is closed without selection, otherwise returns the user
   *         selection.
   */
  public static Optional<File> showFileChooser(final FileFilter fileFilter) {
    final JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(fileFilter);
    final int returnCode = fileChooser.showOpenDialog(mainFrame);

    if (returnCode == JFileChooser.APPROVE_OPTION) {
      return Optional.of(fileChooser.getSelectedFile());
    }
    return Optional.empty();
  }


  /**
   * Opens a file selection dialog where a user can select/create a file for TripleA save game.
   * An empty optional is returned if user just closes down the dialog window.
   */
  public static Optional<File> showSaveGameFileChooser() {
    // Non-Mac platforms should use the normal Swing JFileChooser
    final JFileChooser fileChooser = SaveGameFileChooser.getInstance();
    final int selectedOption = fileChooser.showOpenDialog(mainFrame);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      return Optional.of(fileChooser.getSelectedFile());
    }
    return Optional.empty();
  }

  public static ProgressWindow newProgressWindow(final String title) {
    return new ProgressWindow(mainFrame, title);
  }

  public static BackgroundTaskRunner newBackgroundTaskRunner() {
    return new BackgroundTaskRunner(mainFrame);
  }

  public static GameSelectorModel getGameSelectorModel() {
    return gameSelectorModel;
  }

  public static SetupPanelModel getSetupPanelModel() {
    return setupPanelModel;
  }

  /**
   * Strong type for dialog titles. Keeps clear which data is for message body and title, avoids parameter swapping
   * problem and makes refactoring easier.
   */
  public static class Title {
    public String value;

    private Title(final String value) {
      this.value = value;
    }

    public static Title of(final String value) {
      return new Title(value);
    }
  }

  public static int showConfirmDialog(final String message, final Title title, final int optionType,
      final int messageType) {
    return JOptionPane.showConfirmDialog(mainFrame, message, title.value, optionType, messageType);
  }


  public static void showMessageDialog(final String message, final Title title, final int messageType) {
    JOptionPane.showMessageDialog(mainFrame, message, title.value, messageType);
  }


  public static void hideMainFrame() {
    SwingUtilities.invokeLater(() -> mainFrame.setVisible(false));
  }


  /**
   * Sets the 'main frame' to visible. In this context the main frame is the initial
   * welcome (launch lobby/single player game etc..) screen presented to GUI enabled clients.
   */
  public static void showMainFrame() {
    SwingUtilities.invokeLater(() -> {
      mainFrame.requestFocus();
      mainFrame.toFront();
      mainFrame.setVisible(true);

      SwingComponents.addWindowClosingListener(mainFrame, GameRunner::exitGameIfFinished);

      ProAi.gameOverClearCache();

      loadGame();

      if (System.getProperty(TRIPLEA_SERVER, "false").equals("true")) {
        setupPanelModel.showServer(mainFrame);
      } else if (System.getProperty(TRIPLEA_CLIENT, "false").equals("true")) {
        setupPanelModel.showClient(mainFrame);
      }
    });
  }

  private static void loadGame() {
    Interruptibles.await(() -> newBackgroundTaskRunner().runInBackground("Loading game...", () -> {
      gameSelectorModel.loadDefaultGameSameThread();
      final String fileName = System.getProperty(TRIPLEA_GAME, "");
      if (fileName.length() > 0) {
        gameSelectorModel.load(new File(fileName), mainFrame);
      }

      final String downloadableMap = System.getProperty(TRIPLEA_MAP_DOWNLOAD, "");
      if (!downloadableMap.isEmpty()) {
        SwingUtilities.invokeLater(() -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(downloadableMap));
      }
    }));
  }

  private static void checkLocalSystem() {
    final LocalSystemChecker localSystemChecker = new LocalSystemChecker();
    final Collection<Exception> exceptions = localSystemChecker.getExceptions();
    if (!exceptions.isEmpty()) {
      final String msg = String.format(
          "Warning!! %d system checks failed. Some game features may not be available or may not work correctly.\n%s",
          exceptions.size(), localSystemChecker.getStatusMessage());
      ClientLogger.logError(msg);
    }
  }


  private static void usage() {
    System.out.println("\nUsage and Valid Arguments:\n"
        + "   " + TRIPLEA_GAME + "=<FILE_NAME>\n"
        + "   " + GAME_HOST_CONSOLE + "=<true/false>\n"
        + "   " + TRIPLEA_SERVER + "=true\n"
        + "   " + TRIPLEA_PORT + "=<PORT>\n"
        + "   " + TRIPLEA_NAME + "=<PLAYER_NAME>\n"
        + "   " + LOBBY_HOST + "=<LOBBY_HOST>\n"
        + "   " + LOBBY_PORT + "=<LOBBY_PORT>\n"
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

  private static void checkForUpdates() {
    new Thread(() -> {
      if (System.getProperty(TRIPLEA_SERVER, "false").equalsIgnoreCase("true")) {
        return;
      }
      if (System.getProperty(TRIPLEA_CLIENT, "false").equalsIgnoreCase("true")) {
        return;
      }
      if (System.getProperty(DO_NOT_CHECK_FOR_UPDATES, "false").equalsIgnoreCase("true")) {
        return;
      }

      // if we are joining a game online, or hosting, or loading straight into a savegame, do not check
      final String fileName = System.getProperty(TRIPLEA_GAME, "");
      if (fileName.trim().length() > 0) {
        return;
      }

      boolean busy = checkForTutorialMap();
      if (!busy) {
        busy = checkForLatestEngineVersionOut();
      }
      if (!busy) {
        checkForUpdatedMaps();
      }
    }, "Checking Latest TripleA Engine Version").start();
  }

  /**
   * Returns true if we are out of date or this is the first time this triplea has ever been run.
   */
  private static boolean checkForLatestEngineVersionOut() {
    try {
      final boolean firstTimeThisVersion = ClientSetting.TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY.booleanValue();
      // check at most once per 2 days (but still allow a 'first run message' for a new version of triplea)
      final LocalDateTime localDateTime = LocalDateTime.now();
      final int year = localDateTime.get(ChronoField.YEAR);
      final int day = localDateTime.get(ChronoField.DAY_OF_YEAR);
      // format year:day
      final String lastCheckTime = ClientSetting.TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE.value();
      if (!firstTimeThisVersion && lastCheckTime.trim().length() > 0) {
        final String[] yearDay = lastCheckTime.split(":");
        if (Integer.parseInt(yearDay[0]) >= year && Integer.parseInt(yearDay[1]) + 1 >= day) {
          return false;
        }
      }

      ClientSetting.TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE.save(year + ":" + day);
      ClientSetting.flush();

      final EngineVersionProperties latestEngineOut = EngineVersionProperties.contactServerForEngineVersionProperties();
      if (latestEngineOut == null) {
        return false;
      }
      if (ClientContext.engineVersion().isLessThan(latestEngineOut.getLatestVersionOut())) {
        SwingUtilities
            .invokeLater(() -> EventThreadJOptionPane.showMessageDialog(null, latestEngineOut.getOutOfDateComponent(),
                "Please Update TripleA", JOptionPane.INFORMATION_MESSAGE));
        return true;
      }
    } catch (final Exception e) {
      ClientLogger.logError("Error while checking for engine updates", e);
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

    final String message = "<html>Would you like to download the tutorial map?<br><br>"
        + "(You can always download it later using the Download Maps<br>"
        + "command if you don't want to do it now.)</html>";
    SwingComponents.promptUser("Welcome to TripleA", message, () -> {
      DownloadMapsWindow.showDownloadMapsWindowAndDownload("Tutorial");
    });
    return true;
  }

  private static void checkForUpdatedMaps() {
    final MapDownloadController downloadController = ClientContext.mapDownloadController();
    downloadController.checkDownloadedMapsAreLatest();
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
      Thread.currentThread().interrupt();
    }
    return img;
  }

  public static void hostGame(final int port, final String playerName, final String comments, final String password,
      final Messengers messengers) {
    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands);
    commands.add("-D" + TRIPLEA_SERVER + "=true");
    commands.add("-D" + TRIPLEA_PORT + "=" + port);
    commands.add("-D" + TRIPLEA_NAME + "=" + playerName);
    commands.add("-D" + LOBBY_HOST + "="
        + messengers.getMessenger().getRemoteServerSocketAddress().getAddress().getHostAddress());
    commands.add("-D" + LOBBY_PORT + "="
        + messengers.getMessenger().getRemoteServerSocketAddress().getPort());
    commands.add("-D" + LOBBY_GAME_COMMENTS + "=" + comments);
    commands.add("-D" + LOBBY_GAME_HOSTED_BY + "=" + messengers.getMessenger().getLocalNode().getName());
    if (password != null && password.length() > 0) {
      commands.add("-D" + SERVER_PASSWORD + "=" + password);
    }
    final String fileName = System.getProperty(TRIPLEA_GAME, "");
    if (fileName.length() > 0) {
      commands.add("-D" + TRIPLEA_GAME + "=" + fileName);
    }
    final String javaClass = GameRunner.class.getName();
    commands.add(javaClass);
    ProcessRunnerUtil.exec(commands);
  }

  public static void joinGame(final GameDescription description, final Messengers messengers, final Container parent) {
    final GameDescription.GameStatus status = description.getStatus();
    if (GameDescription.GameStatus.LAUNCHING == status) {
      return;
    }
    final Version engineVersionOfGameToJoin = new Version(description.getEngineVersion());
    final String newClassPath = null;
    if (!GameEngineVersion.of(ClientContext.engineVersion()).isCompatibleWithEngineVersion(engineVersionOfGameToJoin)) {
      JOptionPane.showMessageDialog(parent,
          "Host is using version " + engineVersionOfGameToJoin.toStringFull()
              + ". You need to have a compatible engine version in order to join this game.",
          "Incompatible TripleA engine", JOptionPane.ERROR_MESSAGE);
      return;
    }
    joinGame(description.getPort(), description.getHostedBy().getAddress().getHostAddress(), newClassPath, messengers);
  }

  private static void joinGame(final int port, final String hostAddressIp, final @Nullable String newClassPath,
      final Messengers messengers) {
    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands, newClassPath);
    final String prefix = "-D";
    commands.add(prefix + TRIPLEA_CLIENT + "=true");
    commands.add(prefix + TRIPLEA_PORT + "=" + port);
    commands.add(prefix + TRIPLEA_HOST + "=" + hostAddressIp);
    commands.add(prefix + TRIPLEA_NAME + "=" + messengers.getMessenger().getLocalNode().getName());
    commands.add(GameRunner.class.getName());
    ProcessRunnerUtil.exec(commands);
  }


  public static void exitGameIfFinished() {
    SwingUtilities.invokeLater(() -> {
      boolean allFramesClosed = true;
      for (final Frame f : Frame.getFrames()) {
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

  /**
   * Get the chat for the game, or empty if there is no chat (eg: headless).
   */
  public static Optional<Chat> getChat() {
    final ISetupPanel model = setupPanelModel.getPanel();
    return ((model instanceof ServerSetupPanel) || (model instanceof ClientSetupPanel))
      ? Optional.ofNullable(model.getChatPanel().getChat())
      : Optional.empty();
  }

  /**
   * After the game has been left, call this.
   */
  public static void clientLeftGame() {
    Interruptibles.await(() -> SwingAction.invokeAndWait(() -> {
      // having an oddball issue with the zip stream being closed while parsing to load default game. might be caused by
      // closing of stream while unloading map resources.
      Interruptibles.sleep(100);
      setupPanelModel.showSelectType();
      showMainFrame();
    }));
  }

  public static void quitGame() {
    mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
  }
}
