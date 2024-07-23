package games.strategy.engine.framework.startup.mc;

import static games.strategy.engine.framework.CliProperties.LOBBY_URI;
import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import games.strategy.engine.chat.ChatController;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameState;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.LobbyWatcherThread;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.net.websocket.ClientNetworkBridge;
import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.game.chat.ChatModel;
import org.triplea.http.client.lobby.game.hosting.request.GameHostingClient;
import org.triplea.http.client.lobby.game.hosting.request.GameHostingResponse;
import org.triplea.http.client.web.socket.client.connections.GameToLobbyConnection;
import org.triplea.http.client.web.socket.messages.envelopes.remote.actions.PlayerBannedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.remote.actions.ShutdownServerMessage;
import org.triplea.java.Interruptibles;
import org.triplea.java.ThreadRunner;
import org.triplea.java.concurrency.AsyncRunner;
import org.triplea.util.ExitStatus;

/** Represents a network-aware game server to which multiple clients may connect. */
@Slf4j
public class ServerModel extends Observable implements IConnectionChangeListener {
  public static final RemoteName SERVER_REMOTE_NAME =
      new RemoteName(
          "games.strategy.engine.framework.ui.ServerStartup.SERVER_REMOTE",
          IServerStartupRemote.class);

  static final String CHAT_NAME = "games.strategy.engine.framework.ui.ServerStartup.CHAT_NAME";

  private final GameObjectStreamFactory objectStreamFactory = new GameObjectStreamFactory(null);
  private ServerMessenger serverMessenger;
  private Messengers messengers;
  private GameData data;
  private Map<String, String> playersToNodeListing = new HashMap<>();
  private boolean playersToNodesMappingPersisted = false;
  private Map<String, Boolean> playersEnabledListing = new HashMap<>();
  private Collection<String> playersAllowedToBeDisabled = new HashSet<>();
  private Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder =
      new LinkedHashMap<>();
  private IRemoteModelListener remoteModelListener = IRemoteModelListener.NULL_LISTENER;
  private final GameSelectorModel gameSelectorModel;
  private final LaunchAction launchAction;
  @Getter private ChatModel chatModel;
  private ChatController chatController;
  private final Map<String, PlayerTypes.Type> localPlayerTypes = new HashMap<>();
  // while our server launcher is not null, delegate new/lost connections to it
  private volatile ServerLauncher serverLauncher;
  private CountDownLatch removeConnectionsLatch = null;

  private final Observer gameSelectorObserver = (observable, value) -> gameDataChanged();

  @Getter @Nullable private LobbyWatcherThread lobbyWatcherThread;
  @Nullable private GameToLobbyConnection gameToLobbyConnection;

  public ServerModel(final GameSelectorModel gameSelectorModel, final LaunchAction launchAction) {
    this.gameSelectorModel = Preconditions.checkNotNull(gameSelectorModel);
    this.launchAction = launchAction;
  }

  public Optional<GameHostingResponse> initialize() {
    this.gameSelectorModel.addObserver(gameSelectorObserver);
    return getServerProps().map(this::createServerMessenger);
  }

  static RemoteName getObserverWaitingToStartName(final INode node) {
    return new RemoteName(
        "games.strategy.engine.framework.startup.mc.ServerModel.OBSERVER" + node.getName(),
        IObserverWaitingToJoin.class);
  }

  public void cancel() {
    gameSelectorModel.deleteObserver(gameSelectorObserver);
    Optional.ofNullable(lobbyWatcherThread)
        .map(LobbyWatcherThread::getLobbyWatcher)
        .ifPresent(InGameLobbyWatcherWrapper::shutDown);
    Optional.ofNullable(chatController).ifPresent(ChatController::deactivate);
    Optional.ofNullable(messengers).ifPresent(Messengers::shutDown);
    Optional.ofNullable(chatModel).ifPresent(ChatModel::cancel);
  }

  public void setRemoteModelListener(final @Nullable IRemoteModelListener listener) {
    remoteModelListener = Optional.ofNullable(listener).orElse(IRemoteModelListener.NULL_LISTENER);
  }

  public synchronized void setLocalPlayerType(final String player, final PlayerTypes.Type type) {
    localPlayerTypes.put(player, type);
  }

  /**
   * Persists the players mappings to re-use upon a game data change. Used to persist the previous
   * game's player setting for the game restart if a connection is lost.
   */
  public void persistPlayersToNodesMapping() {
    this.playersToNodesMappingPersisted = true;
  }

  private void gameDataChanged() {
    synchronized (this) {
      data = gameSelectorModel.getGameData();
      if (data != null) {
        updatePlayersOnGameDataChanged(data);
      }
      objectStreamFactory.setData(data);
      localPlayerTypes.clear();
    }
    notifyChannelPlayersChanged();
    remoteModelListener.playerListChanged();
  }

  private void updatePlayersOnGameDataChanged(final GameData data) {
    // If specified, keep the previous player data.
    if (playersToNodesMappingPersisted) {
      playersToNodesMappingPersisted = false;
      final Set<String> dataPlayers =
          data.getPlayerList().stream().map(GamePlayer::getName).collect(Collectors.toSet());
      // Sanity check that the list of countries matches.
      if (dataPlayers.equals(playersToNodeListing.keySet())) {
        // Don't regenerate the mappings, persist existing ones.
        return;
      }
      throw new IllegalStateException("Expected countries to match when persisting seatings");
    }

    // Reset setting based on game data.
    playersToNodeListing = new HashMap<>();
    playersEnabledListing = new HashMap<>();
    playersAllowedToBeDisabled = new HashSet<>(data.getPlayerList().getPlayersThatMayBeDisabled());
    playerNamesAndAlliancesInTurnOrder = new LinkedHashMap<>();
    for (final GamePlayer player : data.getPlayerList().getPlayers()) {
      final String name = player.getName();
      if (launchAction.shouldMinimizeExpensiveAiUse()) {
        if (player.getIsDisabled()) {
          playersToNodeListing.put(name, messengers.getLocalNode().getName());
          localPlayerTypes.put(name, PlayerTypes.WEAK_AI);
        } else {
          // we generally do not want a headless host bot to be doing any AI turns, since that
          // is taxing on the system
          playersToNodeListing.put(name, null);
        }
      } else {
        playersToNodeListing.put(name, messengers.getLocalNode().getName());
      }
      playerNamesAndAlliancesInTurnOrder.put(
          name, data.getAllianceTracker().getAlliancesPlayerIsIn(player));
      playersEnabledListing.put(name, !player.getIsDisabled());
    }
  }

  private Optional<ServerConnectionProps> getServerProps() {
    if (System.getProperty(TRIPLEA_SERVER, "false").equals("true") && GameState.notStarted()) {
      GameState.setStarted();
      return Optional.of(
          ServerConnectionProps.builder()
              .name(System.getProperty(TRIPLEA_NAME))
              .port(Integer.parseInt(System.getProperty(TRIPLEA_PORT)))
              .password(
                  Optional.ofNullable(System.getProperty(SERVER_PASSWORD))
                      .map(String::toCharArray)
                      .orElse(null))
              .build());
    }
    return launchAction.getFallbackConnection(this::cancel);
  }

  @Nullable
  private GameHostingResponse createServerMessenger(final ServerConnectionProps props) {
    try {
      this.serverMessenger =
          new ServerMessenger(props.getName(), props.getPort(), objectStreamFactory);
      serverMessenger.addConnectionChangeListener(this);

      messengers = new Messengers(serverMessenger);
      messengers.registerRemote(
          launchAction.getStartupRemote(new DefaultServerModelView()), SERVER_REMOTE_NAME);

      @Nullable final GameHostingResponse gameHostingResponse;

      if (System.getProperty(LOBBY_URI) != null) {
        final URI lobbyUri = URI.create(System.getProperty(LOBBY_URI));
        gameHostingResponse = GameHostingClient.newClient(lobbyUri).sendGameHostingRequest();

        lobbyWatcherThread =
            new LobbyWatcherThread(
                gameSelectorModel, serverMessenger, launchAction.createThreadMessaging());

        gameToLobbyConnection =
            new GameToLobbyConnection(lobbyUri, gameHostingResponse, launchAction::handleError);

        serverMessenger.setGameToLobbyConnection(gameToLobbyConnection);

        gameToLobbyConnection.addMessageListener(
            PlayerBannedMessage.TYPE,
            bannedPlayerMessage ->
                new PlayerDisconnectAction(serverMessenger, this::cancel)
                    .accept(bannedPlayerMessage.getIpAddress()));

        ExitStatus.addExitAction(this::cancel);
        gameToLobbyConnection.addMessageListener(
            ShutdownServerMessage.TYPE,
            shutdownServerMessage -> {
              if (shutdownServerMessage
                  .getGameId()
                  .equals(lobbyWatcherThread.getGameId().orElse(""))) {
                ExitStatus.SUCCESS.exit();
              }
            });

        lobbyWatcherThread.createLobbyWatcher(
            gameToLobbyConnection, !launchAction.shouldMinimizeExpensiveAiUse());
      } else {
        gameHostingResponse = null;
      }

      chatController = new ChatController(CHAT_NAME, messengers);

      // TODO: Project#4 Change no-op network sender to a real network bridge
      chatModel =
          launchAction.createChatModel(CHAT_NAME, messengers, ClientNetworkBridge.NO_OP_SENDER);

      serverMessenger.setAcceptNewConnections(true);
      gameDataChanged();
      return gameHostingResponse;
    } catch (final BindException e) {
      log.warn(
          "Could not open network port, please close any other TripleA games you are\n"
              + "hosting or choose a different network port. If that is not the problem\n"
              + "then check your firewall rules.",
          e);
      cancel();
    } catch (final IOException e) {
      log.error("Unable to create server socket.", e);
      cancel();
      if (GameRunner.headless()) {
        log.error("Failed to connect to lobby, shutting down.");
        ExitStatus.FAILURE.exit();
      }
    }
    return null;
  }

  private PlayerListing getPlayerListingInternal() {
    synchronized (this) {
      if (data == null) {
        return new PlayerListing(
            new HashMap<>(),
            new HashMap<>(playersEnabledListing),
            getLocalPlayerTypes(),
            gameSelectorModel.getGameName(),
            gameSelectorModel.getGameRound(),
            new HashSet<>(playersAllowedToBeDisabled),
            new LinkedHashMap<>());
      }
      return new PlayerListing(
          new HashMap<>(playersToNodeListing),
          new HashMap<>(playersEnabledListing),
          getLocalPlayerTypes(),
          data.getGameName(),
          data.getSequence().getRound() + "",
          new HashSet<>(playersAllowedToBeDisabled),
          playerNamesAndAlliancesInTurnOrder);
    }
  }

  private void takePlayerInternal(final INode from, final boolean take, final String playerName) {
    // synchronize to make sure two adds aren't executed at once
    synchronized (this) {
      if (!playersToNodeListing.containsKey(playerName)) {
        return;
      }
      playersToNodeListing.put(playerName, take ? from.getName() : null);
    }
    notifyChannelPlayersChanged();
    remoteModelListener.playersTakenChanged();
  }

  private void setPlayerEnabled(final String playerName, final boolean enabled) {
    takePlayerInternal(messengers.getLocalNode(), true, playerName);
    // synchronize
    synchronized (this) {
      if (!playersEnabledListing.containsKey(playerName)) {
        return;
      }
      playersEnabledListing.put(playerName, enabled);
      if (launchAction.shouldMinimizeExpensiveAiUse()) {
        // we do not want the host bot to actually play, so set to null if enabled,
        // and set to weak ai if disabled
        if (enabled) {
          playersToNodeListing.put(playerName, null);
        } else {
          localPlayerTypes.put(playerName, PlayerTypes.WEAK_AI);
        }
      }
    }
    notifyChannelPlayersChanged();
    remoteModelListener.playersTakenChanged();
  }

  public void setAllPlayersToNullNodes() {
    if (playersToNodeListing != null) {
      playersToNodeListing.replaceAll((key, value) -> null);
    }
  }

  private void notifyChannelPlayersChanged() {
    Optional.ofNullable(messengers)
        .ifPresent(
            messenger -> {
              final IClientChannel channel =
                  (IClientChannel) messenger.getChannelBroadcaster(IClientChannel.CHANNEL_NAME);
              AsyncRunner.runAsync(() -> channel.playerListingChanged(getPlayerListingInternal()))
                  .exceptionally(e -> log.warn("Network communication error", e));
            });
  }

  public void takePlayer(final String playerName) {
    takePlayerInternal(messengers.getLocalNode(), true, playerName);
  }

  public void releasePlayer(final String playerName) {
    takePlayerInternal(messengers.getLocalNode(), false, playerName);
  }

  public void disablePlayer(final String playerName) {
    setPlayerEnabled(playerName, false);
  }

  public void enablePlayer(final String playerName) {
    setPlayerEnabled(playerName, true);
  }

  public IServerMessenger getMessenger() {
    return serverMessenger;
  }

  public synchronized Map<String, String> getPlayersToNodeListing() {
    return new HashMap<>(playersToNodeListing);
  }

  public synchronized Map<String, Boolean> getPlayersEnabledListing() {
    return new HashMap<>(playersEnabledListing);
  }

  public synchronized Collection<String> getPlayersAllowedToBeDisabled() {
    return new HashSet<>(playersAllowedToBeDisabled);
  }

  public synchronized Map<String, Collection<String>> getPlayerNamesAndAlliancesInTurnOrder() {
    return new LinkedHashMap<>(playerNamesAndAlliancesInTurnOrder);
  }

  @Override
  public void connectionAdded(final INode to) {
    notifyLobby(
        (lobbyConnection, gameId) -> lobbyConnection.playerJoined(gameId, to.getPlayerName()));
  }

  /**
   * If there is a connection to lobby, and we have established a lobby watcher, and that lobby
   * watcher has a game-id, then the provided parameter is executed passing to it as arguments the
   * lobby connection and game-id.
   */
  private void notifyLobby(
      final BiConsumer<GameToLobbyConnection, String> connectionAndGameIdAction) {
    if (gameToLobbyConnection != null && lobbyWatcherThread != null) {
      lobbyWatcherThread
          .getGameId()
          .ifPresent(
              gameId ->
                  ThreadRunner.runInNewThread(
                      () -> connectionAndGameIdAction.accept(gameToLobbyConnection, gameId)));
    }
  }

  @Override
  public void connectionRemoved(final INode node) {
    notifyLobby(
        (lobbyConnection, gameId) -> lobbyConnection.playerLeft(gameId, node.getPlayerName()));
    if (removeConnectionsLatch != null) {
      Interruptibles.await(() -> removeConnectionsLatch.await(6, TimeUnit.SECONDS));
    }
    // will be handled elsewhere
    if (serverLauncher != null) {
      serverLauncher.connectionLost(node);
      return;
    }
    // we lost a node. Remove the player they play.
    final List<String> free = new ArrayList<>();
    synchronized (this) {
      for (final Map.Entry<String, String> entryPlayerToNode : playersToNodeListing.entrySet()) {
        if (node.getName().equals(entryPlayerToNode.getValue())) {
          free.add(entryPlayerToNode.getKey());
        }
      }
    }
    for (final String player : free) {
      takePlayerInternal(node, false, player);
    }
  }

  private void disallowRemoveConnections() {
    while (removeConnectionsLatch != null && removeConnectionsLatch.getCount() > 0) {
      removeConnectionsLatch.countDown();
    }
    removeConnectionsLatch = new CountDownLatch(1);
  }

  public void allowRemoveConnections() {
    while (removeConnectionsLatch != null && removeConnectionsLatch.getCount() > 0) {
      removeConnectionsLatch.countDown();
    }
    removeConnectionsLatch = null;
  }

  private Map<String, PlayerTypes.Type> getLocalPlayerTypes() {
    if (data == null) {
      return Map.of();
    }

    final Map<String, PlayerTypes.Type> localPlayerMappings = new HashMap<>();
    // local player default = humans (for bots = weak ai)
    final PlayerTypes.Type defaultLocalType = launchAction.getDefaultLocalPlayerType();
    for (final Map.Entry<String, String> entry : playersToNodeListing.entrySet()) {
      final String player = entry.getKey();
      final String playedBy = entry.getValue();
      if (playedBy != null && playedBy.equals(serverMessenger.getLocalNode().getName())) {
        localPlayerMappings.put(player, localPlayerTypes.getOrDefault(player, defaultLocalType));
      }
    }
    return ImmutableMap.copyOf(localPlayerMappings);
  }

  /**
   * Returns the game launcher or empty if all players are not assigned to a node (e.g. if a player
   * drops out before starting the game).
   */
  public Optional<ServerLauncher> getLauncher() {
    synchronized (this) {
      disallowRemoveConnections();
      // -1 since we don't count ourselves
      final int clientCount = serverMessenger.getNodes().size() - 1;
      final Map<String, INode> remotePlayers = new HashMap<>();
      for (final Entry<String, String> entry : playersToNodeListing.entrySet()) {
        final String playedBy = entry.getValue();
        if (playedBy == null) {
          return Optional.empty();
        }
        if (!playedBy.equals(serverMessenger.getLocalNode().getName())) {
          serverMessenger.getNodes().stream()
              .filter(node -> node.getName().equals(playedBy))
              .findAny()
              .ifPresent(node -> remotePlayers.put(entry.getKey(), node));
        }
      }

      final ServerLauncher serverLauncher =
          new ServerLauncher(
              clientCount,
              messengers,
              gameSelectorModel,
              getPlayerListingInternal(),
              remotePlayers,
              this,
              launchAction,
              Optional.ofNullable(lobbyWatcherThread)
                  .map(LobbyWatcherThread::getLobbyWatcher)
                  .orElse(null));

      return Optional.of(serverLauncher);
    }
  }

  public void newGame() {
    serverMessenger.setAcceptNewConnections(true);
    final IClientChannel channel =
        (IClientChannel) messengers.getChannelBroadcaster(IClientChannel.CHANNEL_NAME);
    notifyChannelPlayersChanged();
    channel.gameReset();
  }

  public void setServerLauncher(final ServerLauncher launcher) {
    serverLauncher = launcher;
  }

  class DefaultServerModelView implements IServerStartupRemote.ServerModelView {
    @Override
    public PlayerListing getPlayerListing() {
      return getPlayerListingInternal();
    }

    @Override
    public void takePlayer(final INode who, final String playerName) {
      takePlayerInternal(who, true, playerName);
    }

    @Override
    public void releasePlayer(final INode who, final String playerName) {
      takePlayerInternal(who, false, playerName);
    }

    @Override
    public void disablePlayer(final String playerName) {
      // we don't want the client's changing stuff for anyone but a bot
      setPlayerEnabled(playerName, false);
    }

    @Override
    public void enablePlayer(final String playerName) {
      // we don't want the client's changing stuff for anyone but a bot
      setPlayerEnabled(playerName, true);
    }

    @Override
    public boolean isGameStarted(final INode newNode) {
      if (serverLauncher != null) {
        final RemoteName remoteName = getObserverWaitingToStartName(newNode);
        final IObserverWaitingToJoin observerWaitingToJoinBlocking =
            (IObserverWaitingToJoin) messengers.getRemote(remoteName);
        final IObserverWaitingToJoin observerWaitingToJoinNonBlocking =
            (IObserverWaitingToJoin) messengers.getRemote(remoteName, true);
        serverLauncher.addObserver(
            observerWaitingToJoinBlocking, observerWaitingToJoinNonBlocking, newNode);
        return true;
      }
      return false;
    }

    @Override
    public byte[] getGameOptions() {
      if (data == null
          || data.getProperties() == null
          || data.getProperties().getEditableProperties() == null
          || data.getProperties().getEditableProperties().isEmpty()) {
        return new byte[0];
      }
      final List<IEditableProperty<?>> currentEditableProperties =
          data.getProperties().getEditableProperties();

      try {
        return GameProperties.writeEditableProperties(currentEditableProperties);
      } catch (final IOException e) {
        log.error("Failed to write game properties", e);
      }
      return new byte[0];
    }
  }
}
