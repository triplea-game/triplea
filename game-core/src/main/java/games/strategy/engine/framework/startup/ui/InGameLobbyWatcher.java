package games.strategy.engine.framework.startup.ui;

import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_HOSTED_BY;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_HOST;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_PORT;
import static games.strategy.engine.framework.ArgParser.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_PORT;

import java.awt.Frame;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.GameDescription.GameStatus;
import games.strategy.engine.lobby.server.ILobbyGameController;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.RemoteHostUtils;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;

/**
 * Watches a game in progress, and updates the Lobby with the state of the game.
 *
 * <p>
 * This class opens its own connection to the lobby, and its own messenger.
 * </p>
 */
public class InGameLobbyWatcher {
  public static final String LOBBY_WATCHER_NAME = "lobby_watcher";
  // this is the messenger used by the game
  // it is different than the messenger we use to connect to
  // the game lobby
  private final IServerMessenger serverMessenger;
  private boolean isShutdown = false;
  private final GUID gameId = new GUID();
  private GameSelectorModel gameSelectorModel;
  private final Observer gameSelectorModelObserver = (o, arg) -> gameSelectorModelUpdated();
  private IGame game;
  private final GameStepListener gameStepListener =
      (stepName, delegateName, player, round, displayName) -> InGameLobbyWatcher.this.gameStepChanged(round);
  // we create this messenger, and use it to connect to the
  // game lobby
  private final IMessenger messenger;
  private final IRemoteMessenger remoteMessenger;
  private final GameDescription gameDescription;
  private final Object mutex = new Object();
  private final IConnectionChangeListener connectionChangeListener;
  private final IMessengerErrorListener messengerErrorListener;

  /**
   * Reads SystemProperties to see if we should connect to a lobby server
   *
   * <p>
   * After creation, those properties are cleared, since we should watch the first start game.
   * </p>
   *
   * @return null if no watcher should be created
   */
  public static InGameLobbyWatcher newInGameLobbyWatcher(final IServerMessenger gameMessenger, final JComponent parent,
      final InGameLobbyWatcher oldWatcher) {
    final String host = System.getProperty(LOBBY_HOST);
    final String port = System.getProperty(LOBBY_PORT);
    if ((host == null) || (port == null)) {
      return null;
    }
    // clear the properties
    System.clearProperty(LOBBY_HOST);
    System.clearProperty(LOBBY_PORT);
    System.clearProperty(LOBBY_GAME_HOSTED_BY);
    // add them as temporary properties (in case we load an old savegame and need them again)
    System.setProperty(LOBBY_HOST + GameRunner.OLD_EXTENSION, host);
    System.setProperty(LOBBY_PORT + GameRunner.OLD_EXTENSION, port);
    final String hostedBy = System.getProperty(LOBBY_GAME_HOSTED_BY);
    System.setProperty(LOBBY_GAME_HOSTED_BY + GameRunner.OLD_EXTENSION, hostedBy);
    final IConnectionLogin login = new IConnectionLogin() {
      @Override
      public Map<String, String> getProperties(final Map<String, String> challengeProperties) {
        final Map<String, String> properties = new HashMap<>();
        properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
        properties.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
        properties.put(LobbyLoginValidator.LOBBY_WATCHER_LOGIN, Boolean.TRUE.toString());
        return properties;
      }
    };
    try {
      System.out.println("host:" + host + " port:" + port);
      final String mac = MacFinder.getHashedMacAddress();
      final ClientMessenger messenger = new ClientMessenger(host, Integer.parseInt(port),
          getRealName(hostedBy) + "_" + LOBBY_WATCHER_NAME, mac, login);
      final UnifiedMessenger um = new UnifiedMessenger(messenger);
      final RemoteMessenger rm = new RemoteMessenger(um);
      final RemoteHostUtils rhu = new RemoteHostUtils(messenger.getServerNode(), gameMessenger);
      rm.registerRemote(rhu, RemoteHostUtils.getRemoteHostUtilsName(um.getLocalNode()));
      return new InGameLobbyWatcher(messenger, rm, gameMessenger, parent, oldWatcher);
    } catch (final Exception e) {
      ClientLogger.logQuietly("Failed to create in-game lobby watcher", e);
      return null;
    }
  }

  private static String getRealName(final String uniqueName) {
    // Remove any (n) that is added to distinguish duplicate names
    final String name = uniqueName.split(" ")[0];
    return name;
  }

  void setGame(final IGame game) {
    if (this.game != null) {
      this.game.removeGameStepListener(gameStepListener);
    }
    this.game = game;
    if (game != null) {
      game.addGameStepListener(gameStepListener);
      gameStepChanged(game.getData().getSequence().getRound());
    }
  }

  private void gameStepChanged(final int round) {
    synchronized (mutex) {
      if (!gameDescription.getRound().equals(Integer.toString(round))) {
        gameDescription.setRound(round + "");
      }
      postUpdate();
    }
  }

  private void gameSelectorModelUpdated() {
    synchronized (mutex) {
      gameDescription.setGameName(gameSelectorModel.getGameName());
      gameDescription.setGameVersion(gameSelectorModel.getGameVersion());
      postUpdate();
    }
  }

  InGameLobbyWatcher(final IMessenger messenger, final IRemoteMessenger remoteMessenger,
      final IServerMessenger serverMessenger, final JComponent parent, final InGameLobbyWatcher oldWatcher) {
    this.messenger = messenger;
    this.remoteMessenger = remoteMessenger;
    this.serverMessenger = serverMessenger;
    final String password = System.getProperty(SERVER_PASSWORD);
    final boolean passworded = (password != null) && (password.length() > 0);
    final Instant startDateTime = ((oldWatcher == null)
        || (oldWatcher.gameDescription == null)
        || (oldWatcher.gameDescription.getStartDateTime() == null))
            ? Instant.now()
            : oldWatcher.gameDescription.getStartDateTime();
    final int playerCount = ((oldWatcher == null) || (oldWatcher.gameDescription == null))
        ? (HeadlessGameServer.headless() ? 0 : 1)
        : oldWatcher.gameDescription.getPlayerCount();
    final GameStatus gameStatus = ((oldWatcher == null)
        || (oldWatcher.gameDescription == null)
        || (oldWatcher.gameDescription.getStatus() == null))
            ? GameStatus.WAITING_FOR_PLAYERS
            : oldWatcher.gameDescription.getStatus();
    final String gameRound = ((oldWatcher == null)
        || (oldWatcher.gameDescription == null)
        || (oldWatcher.gameDescription.getRound() == null))
            ? "-"
            : oldWatcher.gameDescription.getRound();
    gameDescription = new GameDescription(
        messenger.getLocalNode(),
        serverMessenger.getLocalNode().getPort(),
        startDateTime,
        "???",
        playerCount,
        gameStatus,
        gameRound,
        serverMessenger.getLocalNode().getName(),
        System.getProperty(LOBBY_GAME_COMMENTS),
        passworded,
        ClientContext.engineVersion().toString(), "0");
    final ILobbyGameController controller =
        (ILobbyGameController) this.remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
    synchronized (mutex) {
      controller.postGame(gameId, (GameDescription) gameDescription.clone());
    }
    messengerErrorListener = (messenger1, reason) -> shutDown();
    this.messenger.addErrorListener(messengerErrorListener);
    connectionChangeListener = new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        updatePlayerCount();
      }

      @Override
      public void connectionAdded(final INode to) {
        updatePlayerCount();
      }
    };
    // when players join or leave the game
    // update the connection count
    this.serverMessenger.addConnectionChangeListener(connectionChangeListener);
    if ((oldWatcher != null) && (oldWatcher.gameDescription != null)) {
      this.setGameStatus(oldWatcher.gameDescription.getStatus(), oldWatcher.game);
    }
    // if we loose our connection, then shutdown
    new Thread(() -> {
      final String addressUsed = controller.testGame(gameId);
      // if the server cannot connect to us, then quit
      if (addressUsed != null) {
        if (isActive()) {
          shutDown();
          SwingUtilities.invokeLater(() -> {
            String portString = System.getProperty(TRIPLEA_PORT);
            if ((portString == null) || (portString.trim().length() <= 0)) {
              portString = "3300";
            }
            final String message = "Your computer is not reachable from the internet.\n"
                + "Please make sure your Firewall allows incoming connections (hosting) for TripleA.\n"
                + "(The firewall exception must be updated every time a new version of TripleA comes out.)\n"
                + "And that your Router is configured to send TCP traffic on port " + portString
                + " to your local ip address.\r\n"
                + "See 'How To Host...' in the help menu, at the top of the lobby screen.\n"
                + "The server tried to connect to your external ip: " + addressUsed;
            if (HeadlessGameServer.headless()) {
              System.out.println(message);
              System.exit(-1);
            }
            final Frame parentComponent = JOptionPane.getFrameForComponent(parent);
            if (JOptionPane.showConfirmDialog(parentComponent,
                message + "\nDo you want to view the tutorial on how to host? This will open in your internet browser.",
                "View Help Website?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
              OpenFileUtility.openUrl(UrlConstants.HOSTING_GUIDE.toString());
            }
            System.exit(-1);
          });
        }
      }
    }).start();
  }

  void setGameSelectorModel(final GameSelectorModel model) {
    cleanUpGameModelListener();
    if (model != null) {
      gameSelectorModel = model;
      gameSelectorModel.addObserver(gameSelectorModelObserver);
      gameSelectorModelUpdated();
    }
  }

  private void cleanUpGameModelListener() {
    if (gameSelectorModel != null) {
      gameSelectorModel.deleteObserver(gameSelectorModelObserver);
    }
  }

  protected void updatePlayerCount() {
    synchronized (mutex) {
      gameDescription.setPlayerCount(serverMessenger.getNodes().size() - (HeadlessGameServer.headless() ? 1 : 0));
      postUpdate();
    }
  }

  private void postUpdate() {
    if (isShutdown) {
      return;
    }
    synchronized (mutex) {
      final ILobbyGameController controller =
          (ILobbyGameController) remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
      controller.updateGame(gameId, (GameDescription) gameDescription.clone());
    }
  }

  void shutDown() {
    isShutdown = true;
    messenger.removeErrorListener(messengerErrorListener);
    messenger.shutDown();
    serverMessenger.removeConnectionChangeListener(connectionChangeListener);
    cleanUpGameModelListener();
    if (game != null) {
      game.removeGameStepListener(gameStepListener);
    }
  }

  public boolean isActive() {
    return !isShutdown;
  }

  void setGameStatus(final GameDescription.GameStatus status, final IGame game) {
    synchronized (mutex) {
      gameDescription.setStatus(status);
      if (game == null) {
        gameDescription.setRound("-");
      } else {
        gameDescription.setRound(game.getData().getSequence().getRound() + "");
      }
      setGame(game);
      postUpdate();
    }
  }

  public String getComments() {
    return gameDescription.getComment();
  }

  public GameDescription getGameDescription() {
    return gameDescription;
  }

  void setGameComments(final String comments) {
    synchronized (mutex) {
      gameDescription.setComment(comments);
      postUpdate();
    }
  }

  void setPassworded(final boolean passworded) {
    synchronized (mutex) {
      gameDescription.setPassworded(passworded);
      postUpdate();
    }
  }
}
