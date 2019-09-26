package games.strategy.engine.framework.startup.ui;

import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.CliProperties.LOBBY_HOST;
import static games.strategy.engine.framework.CliProperties.LOBBY_HTTPS_PORT;
import static games.strategy.engine.framework.CliProperties.LOBBY_PORT;
import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.ClientMessengerFactory;
import games.strategy.net.IClientMessenger;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Node;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Observer;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.java.Log;
import org.triplea.game.server.HeadlessGameServer;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.ILobbyGameController;
import org.triplea.lobby.common.IRemoteHostUtils;

/**
 * Watches a game in progress, and updates the Lobby with the state of the game.
 *
 * <p>This class opens its own connection to the lobby, and its own messenger.
 */
@Log
public class InGameLobbyWatcher {
  // this is the messenger used by the game
  // it is different than the messenger we use to connect to the game lobby
  private final IServerMessenger serverMessenger;
  private boolean isShutdown = false;
  private final UUID gameId = UUID.randomUUID();
  private GameSelectorModel gameSelectorModel;
  private final Observer gameSelectorModelObserver = (o, arg) -> gameSelectorModelUpdated();
  private IGame game;
  // we create this messenger, and use it to connect to the game lobby
  private final IMessenger lobbyMessenger;
  @Getter private final IRemoteMessenger remoteMessenger;
  private final Object postMutex = new Object();
  private GameDescription gameDescription;
  private final IConnectionChangeListener connectionChangeListener;
  private final IMessengerErrorListener messengerErrorListener = e -> shutDown();
  private final boolean humanPlayer;

  private InGameLobbyWatcher(
      final IMessenger lobbyMessenger,
      final IRemoteMessenger remoteMessenger,
      final IServerMessenger serverMessenger,
      @Nullable final InGameLobbyWatcher oldWatcher) {
    this(
        lobbyMessenger,
        remoteMessenger,
        serverMessenger,
        Optional.ofNullable(oldWatcher).map(old -> old.gameDescription).orElse(null),
        Optional.ofNullable(oldWatcher).map(old -> old.game).orElse(null));
  }

  private InGameLobbyWatcher(
      final IMessenger lobbyMessenger,
      final IRemoteMessenger remoteMessenger,
      final IServerMessenger serverMessenger,
      @Nullable final GameDescription oldGameDescription,
      @Nullable final IGame oldGame) {
    this.lobbyMessenger = lobbyMessenger;
    this.remoteMessenger = remoteMessenger;
    this.serverMessenger = serverMessenger;
    humanPlayer = !HeadlessGameServer.headless();

    final boolean passworded = !Strings.nullToEmpty(System.getProperty(SERVER_PASSWORD)).isEmpty();

    final Instant startDateTime =
        Optional.ofNullable(oldGameDescription)
            .map(GameDescription::getStartDateTime)
            .orElseGet(Instant::now);

    final int playerCount =
        Optional.ofNullable(oldGameDescription)
            .map(GameDescription::getPlayerCount)
            .orElseGet(() -> humanPlayer ? 1 : 0);

    final GameDescription.GameStatus gameStatus =
        Optional.ofNullable(oldGameDescription)
            .map(GameDescription::getStatus)
            .orElse(GameDescription.GameStatus.WAITING_FOR_PLAYERS);

    final int gameRound =
        Optional.ofNullable(oldGameDescription).map(GameDescription::getRound).orElse(0);

    final Optional<Integer> customPort = Optional.ofNullable(Integer.getInteger("customPort"));
    final InetSocketAddress publicView =
        Optional.ofNullable(System.getProperty("customHost"))
            .map(s -> new InetSocketAddress(s, customPort.orElse(3300)))
            .orElse(
                new InetSocketAddress(
                    lobbyMessenger.getLocalNode().getSocketAddress().getHostName(),
                    serverMessenger.getLocalNode().getPort()));
    final INode publicNode = new Node(lobbyMessenger.getLocalNode().getName(), publicView);
    gameDescription =
        GameDescription.builder()
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
            .build();
    final ILobbyGameController controller =
        (ILobbyGameController) this.remoteMessenger.getRemote(ILobbyGameController.REMOTE_NAME);
    synchronized (postMutex) {
      controller.postGame(gameId, gameDescription);
    }
    if (this.lobbyMessenger instanceof IClientMessenger) {
      ((IClientMessenger) this.lobbyMessenger).addErrorListener(messengerErrorListener);
    }
    connectionChangeListener =
        new IConnectionChangeListener() {
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
    if (oldGameDescription != null && oldGame != null) {
      this.setGameStatus(oldGameDescription.getStatus(), oldGame);
    }
  }

  /**
   * Reads system properties to see if we should connect to a lobby server.
   *
   * <p>After creation, those properties are cleared, since we should watch the first start game.
   *
   * @return Empty if no watcher should be created
   */
  public static Optional<InGameLobbyWatcher> newInGameLobbyWatcher(
      final IServerMessenger gameMessenger, final InGameLobbyWatcher oldWatcher) {
    final String host = Preconditions.checkNotNull(getLobbySystemProperty(LOBBY_HOST));
    final String port = Preconditions.checkNotNull(getLobbySystemProperty(LOBBY_PORT));
    // TODO: Project#12 use https port
    @SuppressWarnings("unused")
    final String httpsPort = Preconditions.checkNotNull(getLobbySystemProperty(LOBBY_HTTPS_PORT));
    final String hostedBy = Preconditions.checkNotNull(getLobbySystemProperty(TRIPLEA_NAME));

    try {
      final IClientMessenger messenger =
          ClientMessengerFactory.newLobbyWatcherMessenger(host, Integer.parseInt(port), hostedBy);
      final var unifiedMessenger = new UnifiedMessenger(messenger);
      final var remoteMessenger = new RemoteMessenger(unifiedMessenger);
      final var remoteHostUtils = new RemoteHostUtils(messenger.getServerNode(), gameMessenger);
      remoteMessenger.registerRemote(
          remoteHostUtils,
          IRemoteHostUtils.Companion.newRemoteNameForNode(unifiedMessenger.getLocalNode()));
      return Optional.of(
          new InGameLobbyWatcher(messenger, remoteMessenger, gameMessenger, oldWatcher));
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Failed to create in-game lobby watcher", e);
      return Optional.empty();
    }
  }

  @VisibleForTesting
  static String getLobbySystemProperty(final String key) {
    final String backupKey = key + ".backup";
    final @Nullable String value = System.getProperty(key);
    if (value != null) {
      System.clearProperty(key);
      System.setProperty(backupKey, value);
      return value;
    }

    return System.getProperty(backupKey);
  }

  private void setGame(@Nullable final IGame game) {
    this.game = game;
    if (game != null) {
      game.getData()
          .addGameDataEventListener(
              GameDataEvent.GAME_STEP_CHANGED,
              () -> gameStepChanged(game.getData().getSequence().getRound()));
    }
  }

  private void gameStepChanged(final int round) {
    synchronized (postMutex) {
      postUpdate(gameDescription.withRound(round));
    }
  }

  private void gameSelectorModelUpdated() {
    synchronized (postMutex) {
      postUpdate(
          gameDescription
              .withGameName(gameSelectorModel.getGameName())
              .withGameVersion(gameSelectorModel.getGameVersion()));
    }
  }

  void setGameSelectorModel(@Nullable final GameSelectorModel model) {
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
    synchronized (postMutex) {
      postUpdate(
          gameDescription.withPlayerCount(
              serverMessenger.getNodes().size() - (humanPlayer ? 0 : 1)));
    }
  }

  private void postUpdate(final GameDescription newDescription) {
    if (isShutdown || newDescription.equals(gameDescription)) {
      return;
    }
    gameDescription = newDescription;
    final ILobbyGameController controller =
        (ILobbyGameController) remoteMessenger.getRemote(ILobbyGameController.REMOTE_NAME);
    controller.updateGame(gameId, newDescription);
  }

  void shutDown() {
    isShutdown = true;
    if (lobbyMessenger instanceof IClientMessenger) {
      ((IClientMessenger) this.lobbyMessenger).removeErrorListener(messengerErrorListener);
    }
    lobbyMessenger.shutDown();
    serverMessenger.removeConnectionChangeListener(connectionChangeListener);
    cleanUpGameModelListener();
  }

  public boolean isActive() {
    return !isShutdown;
  }

  void setGameStatus(final GameDescription.GameStatus status, final IGame game) {
    setGame(game);
    synchronized (postMutex) {
      postUpdate(
          gameDescription
              .withStatus(status)
              .withRound(game == null ? 0 : game.getData().getSequence().getRound()));
    }
  }

  public String getComments() {
    return gameDescription.getComment();
  }

  void setGameComments(final String comments) {
    synchronized (postMutex) {
      postUpdate(gameDescription.withComment(comments));
    }
  }

  void setPassworded(final boolean passworded) {
    synchronized (postMutex) {
      postUpdate(gameDescription.withPassworded(passworded));
    }
  }
}
