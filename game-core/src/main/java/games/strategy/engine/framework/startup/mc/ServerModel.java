package games.strategy.engine.framework.startup.mc;

import static games.strategy.engine.framework.CliProperties.LOBBY_URI;
import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.ChatMessagePanel.ChatSoundProfile;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.HeadlessChat;
import games.strategy.engine.chat.MessengersChatTransmitter;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameState;
import games.strategy.engine.framework.HeadlessAutoSaveType;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.LobbyWatcherThread;
import games.strategy.engine.framework.startup.WatcherThreadMessaging;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.lobby.connection.GameToLobbyConnection;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.triplea.settings.ClientSetting;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lombok.Getter;
import lombok.extern.java.Log;
import org.triplea.domain.data.UserName;
import org.triplea.game.chat.ChatModel;
import org.triplea.game.server.HeadlessGameServer;
import org.triplea.game.startup.ServerSetupModel;
import org.triplea.http.client.lobby.game.hosting.GameHostingClient;
import org.triplea.http.client.lobby.game.hosting.GameHostingResponse;
import org.triplea.http.client.remote.actions.messages.server.RemoteActionListeners;
import org.triplea.http.client.remote.actions.messages.server.ServerRemoteActionMessageType;
import org.triplea.http.client.web.socket.WebsocketListenerBinding;
import org.triplea.http.client.web.socket.WebsocketListenerFactory;
import org.triplea.http.client.web.socket.WebsocketPaths;
import org.triplea.io.IoUtils;
import org.triplea.java.Interruptibles;
import org.triplea.swing.SwingAction;
import org.triplea.util.ExitStatus;
import org.triplea.util.Version;

/** Represents a network-aware game server to which multiple clients may connect. */
@Log
public class ServerModel extends Observable implements IConnectionChangeListener {
  public static final RemoteName SERVER_REMOTE_NAME =
      new RemoteName(
          "games.strategy.engine.framework.ui.ServerStartup.SERVER_REMOTE",
          IServerStartupRemote.class);

  static final String CHAT_NAME = "games.strategy.engine.framework.ui.ServerStartup.CHAT_NAME";

  private final GameObjectStreamFactory objectStreamFactory = new GameObjectStreamFactory(null);
  private final ServerSetupModel serverSetupModel;
  private IServerMessenger serverMessenger;
  private Messengers messengers;
  private GameData data;
  private Map<String, String> playersToNodeListing = new HashMap<>();
  private Map<String, Boolean> playersEnabledListing = new HashMap<>();
  private Collection<String> playersAllowedToBeDisabled = new HashSet<>();
  private Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder =
      new LinkedHashMap<>();
  private IRemoteModelListener remoteModelListener = IRemoteModelListener.NULL_LISTENER;
  private final GameSelectorModel gameSelectorModel;
  @Nullable private final JFrame ui;
  private final LaunchAction launchAction;
  private ChatModel chatModel;
  private Runnable chatModelCancel;
  private ChatController chatController;
  private final Map<String, PlayerType> localPlayerTypes = new HashMap<>();
  // while our server launcher is not null, delegate new/lost connections to it
  private volatile ServerLauncher serverLauncher;
  private CountDownLatch removeConnectionsLatch = null;
  private final Observer gameSelectorObserver = (observable, value) -> gameDataChanged();
  @Getter @Nullable private LobbyWatcherThread lobbyWatcherThread;
  private @Nullable WebsocketListenerBinding<ServerRemoteActionMessageType, RemoteActionListeners>
      remoteActionsListener;

  private final IServerStartupRemote serverStartupRemote =
      new IServerStartupRemote() {
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
          if (ui != null) {
            return;
          }
          // we don't want the client's changing stuff for anyone but a bot
          setPlayerEnabled(playerName, false);
        }

        @Override
        public void enablePlayer(final String playerName) {
          if (ui != null) {
            return;
          }
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
        public boolean getIsServerHeadless() {
          return HeadlessGameServer.headless();
        }

        /**
         * This should not be called from within game, only from the game setup screen, while
         * everyone is waiting for game to start.
         */
        @Override
        public byte[] getSaveGame() {
          try {
            return IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, data));
          } catch (final IOException e) {
            throw new IllegalStateException(e);
          }
        }

        @Override
        public byte[] getGameOptions() {
          if (data == null
              || data.getProperties() == null
              || data.getProperties().getEditableProperties() == null
              || data.getProperties().getEditableProperties().isEmpty()) {
            return null;
          }
          final List<IEditableProperty<?>> currentEditableProperties =
              data.getProperties().getEditableProperties();

          try {
            return GameProperties.writeEditableProperties(currentEditableProperties);
          } catch (final IOException e) {
            log.log(Level.SEVERE, "Failed to write game properties", e);
          }
          return null;
        }

        @Override
        public Set<String> getAvailableGames() {
          final HeadlessGameServer headless = HeadlessGameServer.getInstance();
          if (headless == null) {
            return null;
          }
          return headless.getAvailableGames();
        }

        @Override
        public void changeServerGameTo(final String gameName) {
          final HeadlessGameServer headless = HeadlessGameServer.getInstance();
          if (headless == null) {
            return;
          }
          headless.setGameMapTo(gameName);
        }

        @Override
        public void changeToLatestAutosave(final HeadlessAutoSaveType autoSaveType) {
          final @Nullable HeadlessGameServer headlessGameServer = HeadlessGameServer.getInstance();
          if (headlessGameServer != null) {
            headlessGameServer.loadGameSave(autoSaveType.getFile());
          }
        }

        @Override
        public void changeToGameSave(final byte[] bytes, final String fileName) {
          // TODO: change to a string message return, so we can tell the user/requestor if it was
          // successful or not, and why
          // if not.
          final HeadlessGameServer headless = HeadlessGameServer.getInstance();
          if (headless == null || bytes == null) {
            return;
          }
          try {
            IoUtils.consumeFromMemory(
                bytes,
                is -> {
                  try (InputStream inputStream = new BufferedInputStream(is)) {
                    headless.loadGameSave(inputStream, fileName);
                  }
                });
          } catch (final Exception e) {
            log.log(Level.SEVERE, "Failed to load save game: " + fileName, e);
          }
        }

        @Override
        public void changeToGameOptions(final byte[] bytes) {
          // TODO: change to a string message return, so we can tell the user/requestor if it was
          // successful or not, and why
          // if not.
          final HeadlessGameServer headless = HeadlessGameServer.getInstance();
          if (headless == null || bytes == null) {
            return;
          }
          try {
            headless.loadGameOptions(bytes);
          } catch (final Exception e) {
            log.log(Level.SEVERE, "Failed to load game options", e);
          }
        }
      };

  public ServerModel(
      final GameSelectorModel gameSelectorModel,
      final ServerSetupModel serverSetupModel,
      @Nullable final JFrame ui,
      final LaunchAction launchAction,
      final Consumer<String> errorHandler) {
    this.gameSelectorModel = Preconditions.checkNotNull(gameSelectorModel);
    this.serverSetupModel = Preconditions.checkNotNull(serverSetupModel);
    this.gameSelectorModel.addObserver(gameSelectorObserver);
    this.ui = ui;
    this.launchAction = launchAction;
    getServerProps().ifPresent(props -> this.createServerMessenger(props, errorHandler));
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
    Optional.ofNullable(chatModelCancel).ifPresent(Runnable::run);
    Optional.ofNullable(remoteActionsListener).ifPresent(WebsocketListenerBinding::close);
  }

  public void setRemoteModelListener(final @Nullable IRemoteModelListener listener) {
    remoteModelListener = Optional.ofNullable(listener).orElse(IRemoteModelListener.NULL_LISTENER);
  }

  public synchronized void setLocalPlayerType(final String player, final PlayerType type) {
    localPlayerTypes.put(player, type);
  }

  private void gameDataChanged() {
    synchronized (this) {
      data = gameSelectorModel.getGameData();
      if (data != null) {
        playersToNodeListing = new HashMap<>();
        playersEnabledListing = new HashMap<>();
        playersAllowedToBeDisabled =
            new HashSet<>(data.getPlayerList().getPlayersThatMayBeDisabled());
        playerNamesAndAlliancesInTurnOrder = new LinkedHashMap<>();
        for (final GamePlayer player : data.getPlayerList().getPlayers()) {
          final String name = player.getName();
          if (ui == null) {
            if (player.getIsDisabled()) {
              playersToNodeListing.put(name, messengers.getLocalNode().getName());
              localPlayerTypes.put(name, PlayerType.WEAK_AI);
            } else {
              // we generally do not want a headless host bot to be doing any AI turns, since that
              // is taxing on the system
              playersToNodeListing.put(name, null);
            }
          } else {
            Optional.ofNullable(messengers)
                .ifPresent(
                    messenger ->
                        playersToNodeListing.put(name, messenger.getLocalNode().getName()));
          }
          playerNamesAndAlliancesInTurnOrder.put(
              name, data.getAllianceTracker().getAlliancesPlayerIsIn(player));
          playersEnabledListing.put(name, !player.getIsDisabled());
        }
      }
      objectStreamFactory.setData(data);
      localPlayerTypes.clear();
    }
    notifyChannelPlayersChanged();
    remoteModelListener.playerListChanged();
  }

  private Optional<ServerConnectionProps> getServerProps() {
    if (System.getProperty(TRIPLEA_SERVER, "false").equals("true") && GameState.notStarted()) {
      GameState.setStarted();
      return Optional.of(
          ServerConnectionProps.builder()
              .name(System.getProperty(TRIPLEA_NAME))
              .port(Integer.parseInt(System.getProperty(TRIPLEA_PORT)))
              .password(System.getProperty(SERVER_PASSWORD))
              .build());
    }
    final UserName userName = UserName.of(ClientSetting.playerName.getValueOrThrow());
    final Interruptibles.Result<ServerOptions> optionsResult =
        Interruptibles.awaitResult(
            () ->
                SwingAction.invokeAndWaitResult(
                    () -> {
                      final ServerOptions options =
                          new ServerOptions(ui, userName, GameRunner.PORT, false);
                      options.setLocationRelativeTo(ui);
                      options.setVisible(true);
                      options.dispose();
                      if (!options.getOkPressed()) {
                        return null;
                      }
                      final String name = options.getName();
                      log.fine("Server playing as:" + name);
                      ClientSetting.playerName.setValue(name);
                      ClientSetting.flush();
                      final int port = options.getPort();
                      if (port >= 65536 || port == 0) {
                        if (ui == null) {
                          throw new IllegalStateException("Invalid Port: " + port);
                        }
                        JOptionPane.showMessageDialog(
                            ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                      }
                      return options;
                    }));
    if (!optionsResult.completed) {
      throw new IllegalArgumentException("Error while gathering connection details");
    }
    if (optionsResult.result.isEmpty()) {
      cancel();
    }
    return optionsResult.result.map(
        options ->
            ServerConnectionProps.builder()
                .name(options.getName())
                .port(options.getPort())
                .password(options.getPassword())
                .build());
  }

  private void createServerMessenger(
      final ServerConnectionProps props, final Consumer<String> errorHandler) {
    try {
      this.serverMessenger =
          new ServerMessenger(props.getName(), props.getPort(), objectStreamFactory);
      serverMessenger.addConnectionChangeListener(this);

      messengers = new Messengers(serverMessenger);
      messengers.registerRemote(serverStartupRemote, SERVER_REMOTE_NAME);

      @Nullable final GameHostingResponse gameHostingResponse;

      if (System.getProperty(LOBBY_URI) != null) {
        final URI lobbyUri = URI.create(System.getProperty(LOBBY_URI));
        gameHostingResponse = GameHostingClient.newClient(lobbyUri).sendGameHostingRequest();

        ExitStatus.SUCCESS.addExitAction(this::cancel);
        remoteActionsListener =
            WebsocketListenerFactory.newListener(
                lobbyUri,
                WebsocketPaths.GAME_CONNECTIONS,
                ServerRemoteActionMessageType::valueOf,
                errorHandler,
                RemoteActionListeners.builder()
                    .bannedPlayerListener(new PlayerDisconnectAction(serverMessenger, this::cancel))
                    .shutdownListener(
                        emptyStringMessage -> {
                          ExitStatus.SUCCESS.exit();
                        })
                    .build());
        lobbyWatcherThread =
            new LobbyWatcherThread(
                gameSelectorModel,
                serverMessenger,
                ui == null
                    ? new WatcherThreadMessaging.HeadlessWatcherThreadMessaging()
                    : new WatcherThreadMessaging.HeadedWatcherThreadMessaging(ui));

        final GameToLobbyConnection gameToLobbyConnection =
            new GameToLobbyConnection(lobbyUri, gameHostingResponse, errorHandler);
        lobbyWatcherThread.createLobbyWatcher(gameToLobbyConnection);
      } else {
        gameHostingResponse = null;
      }

      chatController = new ChatController(CHAT_NAME, messengers, node -> false);

      if (ui == null) {
        chatModel =
            new HeadlessChat(new Chat(new MessengersChatTransmitter(CHAT_NAME, messengers)));
      } else {
        final var chatPanel = ChatPanel.newChatPanel(messengers, CHAT_NAME, ChatSoundProfile.GAME);
        chatModelCancel = chatPanel::deleteChat;
        chatModel = chatPanel;
      }

      serverMessenger.setAcceptNewConnections(true);
      gameDataChanged();
      serverSetupModel.onServerMessengerCreated(this, gameHostingResponse);
    } catch (final IOException ioe) {
      log.log(Level.SEVERE, "Unable to create server socket", ioe);
      cancel();
    }
  }

  private PlayerListing getPlayerListingInternal() {
    synchronized (this) {
      if (data == null) {
        return new PlayerListing(
            new HashMap<>(),
            new HashMap<>(playersEnabledListing),
            getLocalPlayerTypes(),
            new Version(0, 0, 0),
            gameSelectorModel.getGameName(),
            gameSelectorModel.getGameRound(),
            new HashSet<>(playersAllowedToBeDisabled),
            new LinkedHashMap<>());
      }
      return new PlayerListing(
          new HashMap<>(playersToNodeListing),
          new HashMap<>(playersEnabledListing),
          getLocalPlayerTypes(),
          data.getGameVersion(),
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
      if (ui == null) {
        // we do not want the host bot to actually play, so set to null if enabled, and set to weak
        // ai if disabled
        if (enabled) {
          playersToNodeListing.put(playerName, null);
        } else {
          localPlayerTypes.put(playerName, PlayerType.WEAK_AI);
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
              channel.playerListingChanged(getPlayerListingInternal());
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
  public void connectionAdded(final INode to) {}

  @Override
  public void connectionRemoved(final INode node) {
    if (removeConnectionsLatch != null) {
      Interruptibles.await(() -> removeConnectionsLatch.await(6, TimeUnit.SECONDS));
    }
    // will be handled elsewhere
    if (serverLauncher != null) {
      serverLauncher.connectionLost(node);
      return;
    }
    // we lost a node. Remove the players he plays.
    final List<String> free = new ArrayList<>();
    synchronized (this) {
      for (final String player : playersToNodeListing.keySet()) {
        final String playedBy = playersToNodeListing.get(player);
        if (playedBy != null && playedBy.equals(node.getName())) {
          free.add(player);
        }
      }
    }
    for (final String player : free) {
      takePlayerInternal(node, false, player);
    }
  }

  public ChatModel getChatModel() {
    return chatModel;
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

  private Map<String, PlayerType> getLocalPlayerTypes() {
    if (data == null) {
      return Map.of();
    }

    final Map<String, PlayerType> localPlayerMappings = new HashMap<>();
    // local player default = humans (for bots = weak ai)
    final PlayerType defaultLocalType = ui == null ? PlayerType.WEAK_AI : PlayerType.HUMAN_PLAYER;
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
      // -1 since we dont count ourselves
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
              launchAction);
      Optional.ofNullable(lobbyWatcherThread)
          .map(LobbyWatcherThread::getLobbyWatcher)
          .ifPresent(serverLauncher::setInGameLobbyWatcher);
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
}
