package games.strategy.engine.framework.startup.ui;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.SystemPropertyReader;
import games.strategy.engine.framework.startup.WatcherThreadMessaging;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Node;
import java.time.Instant;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.game.lobby.watcher.GameListingClient;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingRequest;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingResponse;
import org.triplea.http.client.web.socket.client.connections.GameToLobbyConnection;
import org.triplea.java.timer.ScheduledTimer;
import org.triplea.java.timer.Timers;
import org.triplea.lobby.common.GameDescription;

/**
 * Watches a game in progress, and updates the Lobby with the state of the game.
 *
 * <p>This class opens its own connection to the lobby, and its own messenger.
 */
@Slf4j
public class InGameLobbyWatcher {
  private boolean isShutdown = false;
  @Getter private String gameId;
  private GameSelectorModel gameSelectorModel;

  private final Observer gameSelectorModelObserver = (o, arg) -> gameSelectorModelUpdated();

  private IGame game;
  private GameDescription gameDescription;
  private final IConnectionChangeListener connectionChangeListener;
  private final boolean humanPlayer;
  private final GameToLobbyConnection gameToLobbyConnection;
  private final IServerMessenger serverMessenger;
  private final ScheduledTimer keepAliveTimer;

  private InGameLobbyWatcher(
      final IServerMessenger serverMessenger,
      final GameToLobbyConnection gameToLobbyConnection,
      final WatcherThreadMessaging watcherThreadMessaging,
      @Nullable final InGameLobbyWatcher oldWatcher,
      final boolean isHuman) {
    this(
        serverMessenger,
        gameToLobbyConnection,
        watcherThreadMessaging,
        Optional.ofNullable(oldWatcher).map(old -> old.gameDescription).orElse(null),
        Optional.ofNullable(oldWatcher).map(old -> old.game).orElse(null),
        isHuman);
  }

  private InGameLobbyWatcher(
      final IServerMessenger serverMessenger,
      final GameToLobbyConnection gameToLobbyConnection,
      final WatcherThreadMessaging watcherThreadMessaging,
      @Nullable final GameDescription oldGameDescription,
      @Nullable final IGame oldGame,
      final boolean isHuman) {
    this.serverMessenger = serverMessenger;
    this.gameToLobbyConnection = gameToLobbyConnection;
    humanPlayer = isHuman;

    final boolean passworded = SystemPropertyReader.serverIsPassworded();

    final Instant startDateTime =
        Optional.ofNullable(oldGameDescription)
            .map(GameDescription::getStartDateTime)
            .orElseGet(Instant::now);

    final int playerCount =
        Optional.ofNullable(oldGameDescription)
            .map(GameDescription::getPlayerCount)
            .orElse(humanPlayer ? 1 : 0);

    final GameDescription.GameStatus gameStatus =
        Optional.ofNullable(oldGameDescription)
            .map(GameDescription::getStatus)
            .orElse(GameDescription.GameStatus.WAITING_FOR_PLAYERS);

    final int gameRound =
        Optional.ofNullable(oldGameDescription).map(GameDescription::getRound).orElse(0);

    final INode publicNode =
        new Node(
            serverMessenger.getLocalNode().getName(),
            SystemPropertyReader.customHost().orElseGet(gameToLobbyConnection::getPublicVisibleIp),
            SystemPropertyReader.customPort()
                .orElseGet(() -> serverMessenger.getLocalNode().getPort()));

    gameDescription =
        GameDescription.builder()
            .hostedBy(publicNode)
            .startDateTime(startDateTime)
            .gameName("???")
            .playerCount(playerCount)
            .status(gameStatus)
            .round(gameRound)
            .comment(SystemPropertyReader.gameComments())
            .passworded(passworded)
            .build();

    final GamePostingResponse gamePostingResponse =
        gameToLobbyConnection.postGame(
            GamePostingRequest.builder()
                .playerNames(serverMessenger.getPlayerNames())
                .lobbyGame(gameDescription.toLobbyGame())
                .build());

    if (!gamePostingResponse.isConnectivityCheckSucceeded()) {
      // Shutdown and the handling current error message will call System.exit, this code
      // path will be a dead-end.
      shutDown();
      watcherThreadMessaging.handleCurrentGameHostNotReachable();

      // Using return here to break out as this is a dead end.
      // Initializing keepAliveTimer and connectionChangeListener to null as they won't be needed.
      keepAliveTimer = null;
      connectionChangeListener = null;
      return;
    }

    gameId = gamePostingResponse.getGameId();

    // Period time is chosen to less than half the keep-alive cut-off time. In case a keep-alive
    // message is lost or missed, we have time to send another one before reaching the cut-off time.
    keepAliveTimer =
        Timers.fixedRateTimer("lobby-watcher-keep-alive")
            .period((GameListingClient.KEEP_ALIVE_SECONDS / 2L) - 1, TimeUnit.SECONDS)
            .task(
                LobbyWatcherKeepAliveTask.builder()
                    .gameId(gameId)
                    .gameIdSetter(id -> gameId = id)
                    .keepAliveSender(gameToLobbyConnection::sendKeepAlive)
                    .gamePoster(
                        () ->
                            gameToLobbyConnection.postGame(
                                GamePostingRequest.builder()
                                    .playerNames(serverMessenger.getPlayerNames())
                                    .lobbyGame(gameDescription.toLobbyGame())
                                    .build()))
                    .build())
            .start();

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
      final IServerMessenger serverMessenger,
      final GameToLobbyConnection gameToLobbyConnection,
      final WatcherThreadMessaging watcherThreadMessaging,
      final InGameLobbyWatcher oldWatcher,
      final boolean isHuman) {
    try {
      return Optional.of(
          new InGameLobbyWatcher(
              serverMessenger, gameToLobbyConnection, watcherThreadMessaging, oldWatcher, isHuman));
    } catch (final Exception e) {
      log.error("Failed to create in-game lobby watcher", e);
      return Optional.empty();
    }
  }

  @VisibleForTesting
  static String getLobbySystemProperty(final String key) {
    @NonNls final String backupKey = key + ".backup";
    final @Nullable String value = System.getProperty(key);
    if (value != null) {
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
    postUpdate(gameDescription.withRound(round));
  }

  private void gameSelectorModelUpdated() {
    postUpdate(gameDescription.withGameName(gameSelectorModel.getGameName()));
  }

  public void setGameSelectorModel(@Nullable final GameSelectorModel model) {
    cleanUpGameModelListener();
    if (model != null) {
      gameSelectorModel = model;
      gameSelectorModel.addObserver(gameSelectorModelObserver);
      gameSelectorModelUpdated();
    }
  }

  private void cleanUpGameModelListener() {
    Optional.ofNullable(gameSelectorModel)
        .ifPresent(selectorModel -> selectorModel.deleteObserver(gameSelectorModelObserver));
  }

  private void updatePlayerCount() {
    postUpdate(
        gameDescription.withPlayerCount(serverMessenger.getNodes().size() - (humanPlayer ? 0 : 1)));
  }

  private void postUpdate(final GameDescription newDescription) {
    if (isShutdown || newDescription.equals(gameDescription)) {
      return;
    }
    gameDescription = newDescription;
    gameToLobbyConnection.updateGame(gameId, gameDescription.toLobbyGame());
  }

  void shutDown() {
    isShutdown = true;
    // if gameId is not null (game was created in lobby successfully) send remove game message to
    //  lobby now that game is to be shut down.
    if (gameId != null) {
      gameToLobbyConnection.disconnect(gameId);
    }
    serverMessenger.removeConnectionChangeListener(connectionChangeListener);
    Optional.ofNullable(keepAliveTimer).ifPresent(ScheduledTimer::cancel);
    cleanUpGameModelListener();
  }

  public boolean isActive() {
    return !isShutdown;
  }

  void setGameStatus(final GameDescription.GameStatus status, final IGame game) {
    setGame(game);
    postUpdate(
        gameDescription
            .withStatus(status)
            .withRound(game == null ? 0 : game.getData().getSequence().getRound()));
  }

  public String getComments() {
    return gameDescription.getComment();
  }

  void setGameComments(final String comments) {
    postUpdate(gameDescription.withComment(comments));
  }

  void setPassworded(final boolean passworded) {
    postUpdate(gameDescription.withPassworded(passworded));
  }
}
