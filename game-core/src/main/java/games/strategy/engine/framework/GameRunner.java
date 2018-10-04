package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_HOSTED_BY;
import static games.strategy.engine.framework.CliProperties.LOBBY_HOST;
import static games.strategy.engine.framework.CliProperties.LOBBY_PORT;
import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_HOST;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_MAP_DOWNLOAD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import java.awt.Component;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import org.triplea.common.util.Services;
import org.triplea.game.ApplicationContext;
import org.triplea.game.client.ui.javafx.JavaFxClientRunner;

import games.strategy.debug.Console;
import games.strategy.debug.ConsoleHandler;
import games.strategy.debug.ErrorMessage;
import games.strategy.engine.ClientContext;
import games.strategy.engine.GameEngineVersion;
import games.strategy.engine.auto.health.check.LocalSystemChecker;
import games.strategy.engine.auto.update.UpdateChecks;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.panels.main.MainPanelBuilder;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.net.Messengers;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.ProgressWindow;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.util.ExitStatus;
import games.strategy.util.Interruptibles;
import games.strategy.util.Version;
import lombok.extern.java.Log;

/**
 * GameRunner - The entrance class with the main method.
 * In this class commonly used constants are getting defined and the Game is being launched
 */
@Log
public final class GameRunner {
  public static final String TRIPLEA_HEADLESS = "triplea.headless";
  public static final int PORT = 3300;
  public static final int MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME = 20;

  private static final GameSelectorModel gameSelectorModel = new GameSelectorModel();
  private static final SetupPanelModel setupPanelModel = new SetupPanelModel(gameSelectorModel);
  private static JFrame mainFrame;

  private GameRunner() {}

  /**
   * Starts a new UI-enabled game client. This method will return before the game client UI exits. The game client UI
   * will continue to run until it is shut down by the user.
   *
   * <p>
   * No command-line arguments will launch a client; additional arguments can be supplied to specify additional
   * behavior.
   * </p>
   *
   * @throws IllegalStateException If called from a headless environment.
   */
  public static void start(final String[] args) {
    checkNotNull(args);
    checkState(!GraphicsEnvironment.isHeadless(),
        "UI client launcher invoked from headless environment. This is currently prohibited by design to "
            + "avoid UI rendering errors in the headless environment.");

    Thread.setDefaultUncaughtExceptionHandler((t, e) -> log.log(Level.SEVERE, e.getLocalizedMessage(), e));
    ClientSetting.initialize();

    if (!ClientSetting.useExperimentalJavaFxUi.booleanValue()) {
      Interruptibles.await(() -> SwingAction.invokeAndWait(() -> {
        LookAndFeel.initialize();
        final Console console = new Console();
        final SimpleFormatter formatter = new SimpleFormatter();
        LogManager.getLogManager().getLogger("").addHandler(new ConsoleHandler(
            logMsg -> {
              if (logMsg.getLevel().intValue() > Level.INFO.intValue()) {
                ErrorMessage.show(logMsg);
              }
              console.append(formatter.format(logMsg));
            }));
        ErrorMessage.enable();
      }));
    }
    ArgParser.handleCommandLineArgs(args);

    if (SystemProperties.isMac()) {
      com.apple.eawt.Application.getApplication().setOpenURIHandler(event -> {
        final String encoding = StandardCharsets.UTF_8.displayName();
        try {
          final String mapName = URLDecoder.decode(
              event.getURI().toString().substring(ArgParser.TRIPLEA_PROTOCOL.length()), encoding);
          SwingUtilities.invokeLater(() -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(mapName));
        } catch (final UnsupportedEncodingException e) {
          throw new AssertionError(encoding + " is not a supported encoding!", e);
        }
      });
    }

    if (HttpProxy.isUsingSystemProxy()) {
      HttpProxy.updateSystemProxy();
    }

    if (ClientSetting.useExperimentalJavaFxUi.booleanValue()) {
      Services.loadAny(JavaFxClientRunner.class).start(args);
    } else {
      SwingUtilities.invokeLater(() -> {
        mainFrame = newMainFrame();
        setupPanelModel.showSelectType();
        new Thread(GameRunner::showMainFrame).start();
      });

      LocalSystemChecker.launch();
      UpdateChecks.launch();
    }
  }

  private static JFrame newMainFrame() {
    final JFrame frame = new JFrame("TripleA");
    LookAndFeelSwingFrameListener.register(frame);

    frame.add(new MainPanelBuilder().buildMainPanel(setupPanelModel, gameSelectorModel));
    frame.pack();

    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setIconImage(getGameIcon(frame));
    frame.setLocationRelativeTo(null);

    SwingComponents.addWindowClosingListener(frame, GameRunner::exitGameIfFinished);

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

  /**
   * Strong type for dialog titles. Keeps clear which data is for message body and title, avoids parameter swapping
   * problem and makes refactoring easier.
   */
  public static class Title {
    public final String value;

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
    });
    ProAi.gameOverClearCache();

    loadGame();

    if (System.getProperty(TRIPLEA_SERVER, "false").equals("true")) {
      setupPanelModel.showServer(mainFrame);
      System.clearProperty(TRIPLEA_SERVER);
    } else if (System.getProperty(TRIPLEA_CLIENT, "false").equals("true")) {
      setupPanelModel.showClient(mainFrame);
      System.clearProperty(TRIPLEA_CLIENT);
    }
  }

  private static void loadGame() {
    checkState(!SwingUtilities.isEventDispatchThread());
    gameSelectorModel.loadDefaultGameSameThread();
    final String fileName = System.getProperty(TRIPLEA_GAME, "");
    if (!fileName.isEmpty() && new File(fileName).exists()) {
      gameSelectorModel.load(new File(fileName));
    }

    final String downloadableMap = System.getProperty(TRIPLEA_MAP_DOWNLOAD, "");
    if (!downloadableMap.isEmpty()) {
      SwingUtilities.invokeLater(() -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(downloadableMap));
    }
  }

  /**
   * Returns the standard application icon typically displayed in a window's title bar.
   */
  public static Image getGameIcon(final Window frame) {
    Image img = null;
    try {
      img = frame.getToolkit().getImage(GameRunner.class.getResource("ta_icon.png"));
    } catch (final Exception ex) {
      log.log(Level.SEVERE, "ta_icon.png not loaded", ex);
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

  /**
   * Spawns a new process to host a network game.
   */
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
    commands.add(Services.loadAny(ApplicationContext.class).getMainClass().getName());
    ProcessRunnerUtil.exec(commands);
  }

  /**
   * Spawns a new process to join a network game.
   */
  public static void joinGame(final GameDescription description, final Messengers messengers, final Container parent) {
    final GameDescription.GameStatus status = description.getStatus();
    if (GameDescription.GameStatus.LAUNCHING == status) {
      return;
    }

    final Version engineVersionOfGameToJoin = new Version(description.getEngineVersion());
    if (!GameEngineVersion.of(ClientContext.engineVersion()).isCompatibleWithEngineVersion(engineVersionOfGameToJoin)) {
      JOptionPane.showMessageDialog(parent,
          "Host is using version " + engineVersionOfGameToJoin.toStringFull()
              + ". You need to have a compatible engine version in order to join this game.",
          "Incompatible TripleA engine", JOptionPane.ERROR_MESSAGE);
      return;
    }

    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands);
    final String prefix = "-D";
    commands.add(prefix + TRIPLEA_CLIENT + "=true");
    commands.add(prefix + TRIPLEA_PORT + "=" + description.getPort());
    commands.add(prefix + TRIPLEA_HOST + "=" + description.getHostedBy().getAddress().getHostAddress());
    commands.add(prefix + TRIPLEA_NAME + "=" + messengers.getMessenger().getLocalNode().getName());
    commands.add(Services.loadAny(ApplicationContext.class).getMainClass().getName());
    ProcessRunnerUtil.exec(commands);
  }

  public static void exitGameIfFinished() {
    SwingUtilities.invokeLater(() -> {
      final boolean allFramesClosed = Arrays.stream(Frame.getFrames())
          .noneMatch(Component::isVisible);
      if (allFramesClosed) {
        ExitStatus.SUCCESS.exit();
      }
    });
  }

  /**
   * After the game has been left, call this.
   */
  public static void clientLeftGame() {
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("This method must not be called from the EDT");
    }
    Interruptibles.await(() -> SwingAction.invokeAndWait(setupPanelModel::showSelectType));
    showMainFrame();
  }

  public static void quitGame() {
    mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
  }
}
