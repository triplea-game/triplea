package games.strategy.engine.framework.startup.ui;

import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.CliProperties.LOBBY_HOST;
import static games.strategy.engine.framework.CliProperties.LOBBY_PORT;
import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.triplea.lobby.common.ILobbyGameController;
import org.triplea.lobby.common.IRemoteHostUtils;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.lobby.server.GameDescription;
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
import games.strategy.net.Node;
import lombok.extern.java.Log;

/**
 * Watches a game in progress, and updates the Lobby with the state of the game.
 *
 * <p>
 * This class opens its own connection to the lobby, and its own messenger.
 * </p>
 */
@Log
public class InGameLobbyWatcher {
  // this is the messenger used by the game
  // it is different than the messenger we use to connect to the game lobby
  private final IServerMessenger serverMessenger;
  private boolean isShutdown = false;
  private final GUID gameId = new GUID();
  private GameSelectorModel gameSelectorModel;
  private final Observer gameSelectorModelObserver = (o, arg) -> gameSelectorModelUpdated();
  private IGame game;
  private final GameStepListener gameStepListener =
      (stepName, delegateName, player, round, displayName) -> InGameLobbyWatcher.this.gameStepChanged(round);
  // we create this messenger, and use it to connect to the game lobby
  private final IMessenger messenger;
  private final IRemoteMessenger remoteMessenger;
  private final AtomicReference<GameDescription> gameDescription;
  private final IConnectionChangeListener connectionChangeListener;
  private final IMessengerErrorListener messengerErrorListener;
  private final boolean isHandlerPlayer;

  private InGameLobbyWatcher(
      final IMessenger messenger,
      final IRemoteMessenger remoteMessenger,
      final IServerMessenger serverMessenger,
      final LobbyWatcherHandler handler,
      final InGameLobbyWatcher oldWatcher) {
    this.messenger = messenger;
    this.remoteMessenger = remoteMessenger;
    this.serverMessenger = serverMessenger;
    this.isHandlerPlayer = handler.isPlayer();
    final String password = System.getProperty(SERVER_PASSWORD);
    final boolean passworded = password != null && password.length() > 0;
    final boolean oldWatcherExists = oldWatcher == null || oldWatcher.gameDescription.get() == null;
    final Instant startDateTime = (oldWatcherExists || oldWatcher.gameDescription.get().getStartDateTime() == null)
        ? Instant.now()
        : oldWatcher.gameDescription.get().getStartDateTime();
    final int playerCount = oldWatcherExists
        ? (isHandlerPlayer ? 1 : 0)
        : oldWatcher.gameDescription.get().getPlayerCount();
    final GameDescription.GameStatus gameStatus =
        (oldWatcherExists || oldWatcher.gameDescription.get().getStatus() == null)
            ? GameDescription.GameStatus.WAITING_FOR_PLAYERS
            : oldWatcher.gameDescription.get().getStatus();
    final int gameRound = oldWatcherExists ? -1 : oldWatcher.gameDescription.get().getRound();

    final Optional<Integer> customPort = Optional.ofNullable(Integer.getInteger("customPort"));
    final InetSocketAddress publicView = Optional.ofNullable(System.getProperty("customHost"))
        .map(s -> new InetSocketAddress(s, customPort.orElse(3300)))
        .orElse(messenger.getLocalNode().getSocketAddress());
    final INode publicNode = new Node(messenger.getLocalNode().getName(), publicView);
    gameDescription = new AtomicReference<>(GameDescription.builder()
        .hostedBy(publicNode)
        .startDateTime(startDateTime)
        .gameName("???")
        .playerCount(playerCount)
        .status(gameStatus)
        .round(gameRound)
        .hostName(serverMessenger.getLocalNode().getName())
        .comment(System.getProperty(LOBBY_GAME_COMMENTS))
        .passworded(passworded)
        .gameVersion("0")
        .build());
    final ILobbyGameController controller =
        (ILobbyGameController) this.remoteMessenger.getRemote(ILobbyGameController.REMOTE_NAME);
    controller.postGame(gameId, gameDescription.get());
    messengerErrorListener = e -> shutDown();
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
    // when players join or leave the game update the connection count
    this.serverMessenger.addConnectionChangeListener(connectionChangeListener);
    if (oldWatcher != null && oldWatcher.gameDescription.get() != null) {
      this.setGameStatus(oldWatcher.gameDescription.get().getStatus(), oldWatcher.game);
    }
    // if we loose our connection, then shutdown
    new Thread(() -> {
      final String addressUsed = controller.testGame(gameId);
      // if the server cannot connect to us, then quit
      if (addressUsed != null) {
        if (isActive()) {
          shutDown();
          String portString = System.getProperty(TRIPLEA_PORT);
          if (portString == null || portString.trim().length() <= 0) {
            portString = "3300";
          }
          final String message = "Your computer is not reachable from the internet.\n"
              + "Please make sure your Firewall allows incoming connections (hosting) for TripleA.\n"
              + "(The firewall exception must be updated every time a new version of TripleA comes out.)\n"
              + "And that your Router is configured to send TCP traffic on port " + portString
              + " to your local ip address.\r\n"
              + "See 'How To Host...' in the help menu, at the top of the lobby screen.\n"
              + "The server tried to connect to your external ip: " + addressUsed;
          handler.reportError(message);
        }
      }
    }).start();
  }

  /**
   * Helper interface to keep the logging logic outside of this class
   * to ensure headless clients don't need to depend on UI classes.
   */
  public interface LobbyWatcherHandler {
    void reportError(String message);

    boolean isPlayer();
  }

  /**
   * Reads system properties to see if we should connect to a lobby server.
   *
   * <p>
   * After creation, those properties are cleared, since we should watch the first start game.
   * </p>
   *
   * @param handler The interface that provides the necessary information to decouple this class from the UI.
   *
   * @return null if no watcher should be created
   */
  public static InGameLobbyWatcher newInGameLobbyWatcher(
      final IServerMessenger gameMessenger,
      final LobbyWatcherHandler handler,
      final InGameLobbyWatcher oldWatcher) {
    Preconditions.checkNotNull(handler);
    final @Nullable String host = getLobbySystemProperty(LOBBY_HOST);
    final @Nullable String port = getLobbySystemProperty(LOBBY_PORT);
    final @Nullable String hostedBy = getLobbySystemProperty(TRIPLEA_NAME);
    if (host == null || port == null) {
      return null;
    }

    final IConnectionLogin login = challenge -> {
      final Map<String, String> response = new HashMap<>();
      response.put(LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
      response.put(LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
      response.put(LobbyLoginResponseKeys.LOBBY_WATCHER_LOGIN, Boolean.TRUE.toString());
      return response;
    };
    try {
      final String mac = MacFinder.getHashedMacAddress();
      final ClientMessenger messenger = new ClientMessenger(host, Integer.parseInt(port),
          IServerMessenger.getRealName(hostedBy) + "_" + LobbyConstants.LOBBY_WATCHER_NAME, mac, login);
      final UnifiedMessenger um = new UnifiedMessenger(messenger);
      final RemoteMessenger rm = new RemoteMessenger(um);
      final RemoteHostUtils rhu = new RemoteHostUtils(messenger.getServerNode(), gameMessenger);
      rm.registerRemote(rhu, IRemoteHostUtils.Companion.newRemoteNameForNode(um.getLocalNode()));
      return new InGameLobbyWatcher(messenger, rm, gameMessenger, handler, oldWatcher);
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Failed to create in-game lobby watcher", e);
      return null;
    }
  }

  @VisibleForTesting
  static @Nullable String getLobbySystemProperty(final String key) {
    final String backupKey = key + ".backup";
    final @Nullable String value = System.getProperty(key);
    if (value != null) {
      System.clearProperty(key);
      System.setProperty(backupKey, value);
      return value;
    }

    return System.getProperty(backupKey);
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
    postUpdate(gameDescription.get().withRound(round));
  }

  private void gameSelectorModelUpdated() {
    postUpdate(gameDescription.get().withGameName(gameSelectorModel.getGameName())
        .withGameVersion(gameSelectorModel.getGameVersion()));
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

  private void updatePlayerCount() {
    postUpdate(gameDescription.get().withPlayerCount(serverMessenger.getNodes().size() - (isHandlerPlayer ? 0 : 1)));
  }

  private void postUpdate(final GameDescription newDescription) {
    if (isShutdown || newDescription.equals(gameDescription.get())) {
      return;
    }
    gameDescription.set(newDescription);
    final ILobbyGameController controller =
        (ILobbyGameController) remoteMessenger.getRemote(ILobbyGameController.REMOTE_NAME);
    controller.updateGame(gameId, newDescription);
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
    setGame(game);
    postUpdate(gameDescription.get()
        .withStatus(status).withRound(game == null ? -1 : game.getData().getSequence().getRound()));
  }

  public String getComments() {
    return gameDescription.get().getComment();
  }

  void setGameComments(final String comments) {
    postUpdate(gameDescription.get().withComment(comments));
  }

  void setPassworded(final boolean passworded) {
    postUpdate(gameDescription.get().withPassworded(passworded));
  }
}
