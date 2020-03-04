package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkState;
import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.CliProperties.LOBBY_URI;
import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_HOST;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_MAP_DOWNLOAD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import games.strategy.engine.auto.update.UpdateChecks;
import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.panels.main.MainPanelBuilder;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.triplea.ai.pro.ProAi;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.triplea.domain.data.UserName;
import org.triplea.game.ApplicationContext;
import org.triplea.java.Interruptibles;
import org.triplea.lobby.common.GameDescription;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.util.ExitStatus;
import org.triplea.util.Services;

/**
 * GameRunner - The entrance class with the main method. In this class commonly used constants are
 * getting defined and the Game is being launched
 */
public final class GameRunner {
  public static final String TRIPLEA_HEADLESS = "triplea.headless";
  public static final int PORT = 3300;
  public static final int MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME = 20;

  private static final GameSelectorModel gameSelectorModel = new GameSelectorModel();
  private static SetupPanelModel setupPanelModel;
  private static JFrame mainFrame;

  private GameRunner() {}

  /**
   * Starts a new UI-enabled game client. This method will return before the game client UI exits.
   * The game client UI will continue to run until it is shut down by the user.
   *
   * <p>No command-line arguments will launch a client; additional arguments can be supplied to
   * specify additional behavior.
   *
   * @throws IllegalStateException If called from a headless environment.
   */
  public static void start() {
    SwingUtilities.invokeLater(
        () -> {
          newMainFrame();
          setupPanelModel = new SetupPanelModel(gameSelectorModel, mainFrame);
          mainFrame.add(new MainPanelBuilder().buildMainPanel(setupPanelModel, gameSelectorModel));
          mainFrame.pack();
          setupPanelModel.showSelectType();
          new Thread(GameRunner::showMainFrame).start();
        });

    UpdateChecks.launch();
  }

  public static void newMainFrame() {
    mainFrame =
        JFrameBuilder.builder()
            .title("TripleA")
            .windowClosedAction(GameRunner::exitGameIfFinished)
            .build();
    LookAndFeelSwingFrameListener.register(mainFrame);

    mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
  }

  public static BackgroundTaskRunner newBackgroundTaskRunner() {
    return new BackgroundTaskRunner(mainFrame);
  }

  /**
   * Strong type for dialog titles. Keeps clear which data is for message body and title, avoids
   * parameter swapping problem and makes refactoring easier.
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

  public static int showConfirmDialog(
      final String message, final Title title, final int optionType, final int messageType) {
    return JOptionPane.showConfirmDialog(mainFrame, message, title.value, optionType, messageType);
  }

  public static void showMessageDialog(
      final String message, final Title title, final int messageType) {
    JOptionPane.showMessageDialog(mainFrame, message, title.value, messageType);
  }

  public static void hideMainFrame() {
    SwingUtilities.invokeLater(() -> mainFrame.setVisible(false));
  }

  /**
   * Sets the 'main frame' to visible. In this context the main frame is the initial welcome (launch
   * lobby/single player game etc..) screen presented to GUI enabled clients.
   */
  public static void showMainFrame() {
    SwingUtilities.invokeLater(
        () -> {
          mainFrame.requestFocus();
          mainFrame.toFront();
          mainFrame.setVisible(true);
        });
    ProAi.gameOverClearCache();

    loadGame();

    if (System.getProperty(TRIPLEA_SERVER, "false").equals("true")) {
      setupPanelModel.showServer();
      System.clearProperty(TRIPLEA_SERVER);
    } else if (System.getProperty(TRIPLEA_CLIENT, "false").equals("true")) {
      setupPanelModel.showClient();
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
      SwingUtilities.invokeLater(
          () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(downloadableMap));
    }
  }

  /** Spawns a new process to host a network game. */
  public static void hostGame(
      final int port,
      final String playerName,
      final String comments,
      final String password,
      final URI lobbyUri) {
    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands);
    commands.add("-D" + TRIPLEA_SERVER + "=true");
    commands.add("-D" + TRIPLEA_PORT + "=" + port);
    commands.add("-D" + TRIPLEA_NAME + "=" + playerName);
    commands.add("-D" + LOBBY_URI + "=" + lobbyUri);
    commands.add("-D" + LOBBY_GAME_COMMENTS + "=" + comments);
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

  /** Spawns a new process to join a network game. */
  public static void joinGame(final GameDescription description, final UserName userName) {
    final GameDescription.GameStatus status = description.getStatus();
    if (GameDescription.GameStatus.LAUNCHING == status) {
      return;
    }

    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands);
    final String prefix = "-D";
    commands.add(prefix + TRIPLEA_CLIENT + "=true");
    commands.add(prefix + TRIPLEA_PORT + "=" + description.getHostedBy().getPort());
    commands.add(
        prefix + TRIPLEA_HOST + "=" + description.getHostedBy().getAddress().getHostAddress());
    commands.add(prefix + TRIPLEA_NAME + "=" + userName.getValue());
    commands.add(Services.loadAny(ApplicationContext.class).getMainClass().getName());
    ProcessRunnerUtil.exec(commands);
  }

  public static void exitGameIfFinished() {
    SwingUtilities.invokeLater(
        () -> {
          final boolean allFramesClosed =
              Arrays.stream(Frame.getFrames()).noneMatch(Component::isVisible);
          if (allFramesClosed) {
            ExitStatus.SUCCESS.exit();
          }
        });
  }

  /** After the game has been left, call this. */
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
