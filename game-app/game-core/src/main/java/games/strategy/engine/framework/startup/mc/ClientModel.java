package games.strategy.engine.framework.startup.mc;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_HOST;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.GameState;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.IServerReady;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.player.Player;
import games.strategy.net.ClientMessengerFactory;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IClientMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.net.websocket.WebsocketNetworkBridge;
import games.strategy.triplea.settings.ClientSetting;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.game.chat.ChatModel;
import org.triplea.java.ThreadRunner;
import org.triplea.java.concurrency.AsyncRunner;

/** Represents a network aware game client connecting to another game that is acting as a server. */
@Slf4j
public class ClientModel implements IMessengerErrorListener {

  public static final RemoteName CLIENT_READY_CHANNEL =
      new RemoteName(
          "games.strategy.engine.framework.startup.mc.ClientModel.CLIENT_READY_CHANNEL",
          IServerReady.class);
  private final GameObjectStreamFactory objectStreamFactory = new GameObjectStreamFactory(null);
  private final GameSelectorModel gameSelectorModel;
  private final ClientModelUi ui;
  private final LaunchAction launchAction;
  private final PlayerTypes.Type clientType;
  private IRemoteModelListener listener = IRemoteModelListener.NULL_LISTENER;
  private Messengers messengers;
  private IClientMessenger messenger;
  private ClientNetworkBridge clientNetworkBridge;
  @Getter private ChatModel chatModel;
  private ClientGame game;
  private boolean hostIsHeadlessBot = false;
  // we set the game data to be null, since we are a client game, and the game data lives on the
  // server
  // however, if we cancel, we want to restore the old game data.
  private GameData gameDataOnStartup;
  private Map<String, String> playersToNodes = new HashMap<>();
  private final IObserverWaitingToJoin observerWaitingToJoin =
      new IObserverWaitingToJoin() {
        @Override
        public void joinGame(final byte[] gameData, final Map<String, INode> players) {
          messengers.unregisterRemote(
              ServerModel.getObserverWaitingToStartName(messenger.getLocalNode()));
          startGame(gameData, players, true);
        }

        @Override
        public void cannotJoinGame(final String reason) {
          ui.showCannotJoinGame(reason);
        }
      };
  private Map<String, Boolean> playersEnabledListing = new HashMap<>();
  private Collection<String> playersAllowedToBeDisabled = new HashSet<>();
  private Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder =
      new LinkedHashMap<>();
  private final IClientChannel channelListener =
      new IClientChannel() {
        @Override
        public void playerListingChanged(final PlayerListing listing) {
          internalPlayerListingChanged(listing);
        }

        @Override
        public void gameReset() {
          objectStreamFactory.setData(null);
          showMainFrame.run();
        }

        @Override
        public void doneSelectingPlayers(final byte[] gameData, final Map<String, INode> players) {
          startGame(gameData, players, false);
        }
      };

  private final Runnable showMainFrame;
  private final Runnable clientLeftGame;

  public ClientModel(
      final GameSelectorModel gameSelectorModel,
      final ClientModelUi ui,
      final LaunchAction launchAction,
      final Runnable showMainFrame,
      final Runnable clientLeftGame,
      final PlayerTypes.Type clientType) {
    this.launchAction = launchAction;
    this.ui = ui;
    this.gameSelectorModel = gameSelectorModel;
    this.showMainFrame = showMainFrame;
    this.clientType = clientType;
    this.clientLeftGame = clientLeftGame;
  }

  public void setRemoteModelListener(@Nonnull final IRemoteModelListener listener) {
    this.listener = checkNotNull(listener);
    AsyncRunner.runAsync(() -> internalPlayerListingChanged(getServerStartup().getPlayerListing()))
        .exceptionally(e -> log.warn("Network communication error", e));
  }

  private Optional<ClientProps> getProps() {
    if (System.getProperty(TRIPLEA_CLIENT, "false").equals("true") && GameState.notStarted()) {
      final ClientProps props =
          ClientProps.builder()
              .host(checkNotNull(System.getProperty(TRIPLEA_HOST)))
              .name(checkNotNull(System.getProperty(TRIPLEA_NAME)))
              .port(Integer.parseInt(checkNotNull(System.getProperty(TRIPLEA_PORT))))
              .build();
      GameState.setStarted();
      return Optional.of(props);
    }
    // load in the saved name!
    final String playername = ClientSetting.playerName.getValueOrThrow();
    return ui.promptForClientProps(playername);
  }

  /**
   * Factory method to create and connect a client messenger to server, returns false if not
   * connected (user messaging will be handled by this method). Method returns true if successfully
   * connected.
   */
  public boolean createClientMessenger() {
    gameDataOnStartup = gameSelectorModel.getGameData();
    gameSelectorModel.setCanSelect(false);
    // load in the saved name!
    final ClientProps props = getProps().orElse(null);
    if (props == null) {
      return false;
    }
    ClientSetting.playerName.setValueAndFlush(props.getName());
    final int port = props.getPort();
    if (port >= 65_536 || port <= 0) {
      ui.showInvalidPort(port);
      return false;
    }
    try {
      messenger =
          ClientMessengerFactory.newClientMessenger(
              props, objectStreamFactory, new ClientLogin(ui));

      if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
        final URI serverURI =
            URI.create("ws://" + props.getHost() + ":" + ServerLauncher.RELAY_SERVER_PORT);
        clientNetworkBridge = new WebsocketNetworkBridge(serverURI);
      } else {
        clientNetworkBridge = ClientNetworkBridge.NO_OP_SENDER;
      }
    } catch (final CouldNotLogInException e) {
      ui.showLoginFailed(e.getMessage());
      return false;
    } catch (final Exception ioe) {
      log.info("Error connecting to host", ioe);
      ui.showConnectionError(ioe.getMessage());
      return false;
    }
    messenger.addErrorListener(this);

    this.messengers = new Messengers(messenger);
    messengers.registerChannelSubscriber(channelListener, IClientChannel.CHANNEL_NAME);

    chatModel =
        launchAction.createChatModel(ServerModel.CHAT_NAME, messengers, clientNetworkBridge);
    if (getIsServerHeadlessTest()) {
      gameSelectorModel.setClientModelForHostBots(this);
      ui.showHostBotWelcome(chatModel);
    }
    messengers.registerRemote(
        observerWaitingToJoin, ServerModel.getObserverWaitingToStartName(messenger.getLocalNode()));
    // save this, it will be cleared later
    gameDataOnStartup = gameSelectorModel.getGameData();
    final IServerStartupRemote serverStartup = getServerStartup();
    final PlayerListing players = serverStartup.getPlayerListing();
    internalPlayerListingChanged(players);
    if (!serverStartup.isGameStarted(messenger.getLocalNode())) {
      messengers.unregisterRemote(
          ServerModel.getObserverWaitingToStartName(messenger.getLocalNode()));
    }
    gameSelectorModel.setIsHostHeadlessBot(hostIsHeadlessBot);
    return true;
  }

  public IServerStartupRemote getServerStartup() {
    return (IServerStartupRemote) messengers.getRemote(ServerModel.SERVER_REMOTE_NAME);
  }

  /** Resets stats and nulls out references, keeps chat alive. */
  public void cancel() {
    gameSelectorModel.setGameData(gameDataOnStartup);
    gameSelectorModel.setCanSelect(true);
    if (messenger != null) {
      messenger.shutDown();
      messenger.removeErrorListener(this);
      objectStreamFactory.setData(null);
      if (chatModel != null) {
        chatModel.cancel();
      }
      hostIsHeadlessBot = false;
      gameSelectorModel.setIsHostHeadlessBot(false);
      gameSelectorModel.setClientModelForHostBots(null);
    }
  }

  private void startGame(
      final byte[] gameData, final Map<String, INode> players, final boolean gameRunning) {
    ui.showGameLoading();
    try {
      startGameInNewThread(gameData, players, gameRunning);
    } catch (final RuntimeException e) {
      ui.hideGameLoading();
      throw e;
    }
  }

  private void startGameInNewThread(
      final byte[] gameData, final Map<String, INode> players, final boolean gameRunning) {
    // this normally takes a couple seconds, but can take up to 60 seconds for a huge game
    final GameData data = GameDataManager.loadGame(new ByteArrayInputStream(gameData)).orElse(null);
    if (data == null) {
      return;
    }
    objectStreamFactory.setData(data);
    final Map<String, PlayerTypes.Type> playerMapping =
        playersToNodes.entrySet().stream()
            .filter(e -> e.getValue() != null)
            .filter(e -> e.getValue().equals(messenger.getLocalNode().getName()))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> clientType));
    final Set<Player> playerSet = data.getGameLoader().newPlayers(playerMapping);
    game = new ClientGame(data, playerSet, players, messengers, clientNetworkBridge);
    ui.hideMainFrame();
    ThreadRunner.runInNewThread(
        () -> {
          try {
            // game will be null if we loose the connection
            if (game != null) {
              try {
                data.getGameLoader().startGame(game, playerSet, launchAction, chatModel.getChat());
              } catch (final Exception e) {
                log.error("Failed to start Game", e);
                game.shutDown();
                messenger.shutDown();
                ui.hideGameLoading();
                // an ugly hack, we need a better way to get the main frame
                clientLeftGame.run();
              }
            }
            if (!gameRunning) {
              ((IServerReady) messengers.getRemote(CLIENT_READY_CHANNEL)).clientReady();
            }
          } finally {
            ui.hideGameLoading();
          }
        });
  }

  public void takePlayer(final String playerName) {
    AsyncRunner.runAsync(() -> getServerStartup().takePlayer(messenger.getLocalNode(), playerName))
        .exceptionally(e -> log.warn("Network communication error", e));
  }

  public void releasePlayer(final String playerName) {
    AsyncRunner.runAsync(
            () -> getServerStartup().releasePlayer(messenger.getLocalNode(), playerName))
        .exceptionally(e -> log.warn("Network communication error", e));
  }

  public void disablePlayer(final String playerName) {
    AsyncRunner.runAsync(() -> getServerStartup().disablePlayer(playerName))
        .exceptionally(e -> log.warn("Network communication error", e));
  }

  public void enablePlayer(final String playerName) {
    AsyncRunner.runAsync(() -> getServerStartup().enablePlayer(playerName))
        .exceptionally(e -> log.warn("Network communication error", e));
  }

  private void internalPlayerListingChanged(final PlayerListing listing) {
    gameSelectorModel.clearDataButKeepGameInfo(listing.getGameName(), listing.getGameRound());
    synchronized (this) {
      playersToNodes = listing.getPlayerToNodeListing();
      playersEnabledListing = listing.getPlayersEnabledListing();
      playersAllowedToBeDisabled = listing.getPlayersAllowedToBeDisabled();
      playerNamesAndAlliancesInTurnOrder = listing.getPlayerNamesAndAlliancesInTurnOrder();
    }
    listener.playerListChanged();
  }

  /** Local player name, eg: US, to remote node (player physical name, eg: "bob) mapping. */
  public synchronized Map<String, String> getPlayerToNodesMapping() {
    return new HashMap<>(playersToNodes);
  }

  /** Returns a map of player node name -> enabled. */
  public synchronized Map<String, Boolean> getPlayersEnabledListing() {
    return new HashMap<>(playersEnabledListing);
  }

  /** Returns the set of players that can be disabled. */
  public synchronized Collection<String> getPlayersAllowedToBeDisabled() {
    return new HashSet<>(playersAllowedToBeDisabled);
  }

  public synchronized Map<String, Collection<String>> getPlayerNamesAndAlliancesInTurnOrder() {
    return new LinkedHashMap<>(playerNamesAndAlliancesInTurnOrder);
  }

  public IClientMessenger getClientMessenger() {
    return messenger;
  }

  @Override
  public void messengerInvalid(final Throwable reason) {
    // The self chat disconnect notification is simply so we have an on-screen notification of the
    // disconnect.
    // In case for example there are many game windows open, it may not be clear which game
    // disconnected.
    if (chatModel != null) {
      Optional.ofNullable(chatModel.getChat())
          .ifPresent(chat -> chat.sendMessage("*** Was Disconnected ***"));
    }
    ui.showConnectionLost();
  }

  private boolean getIsServerHeadlessTest() {
    final IServerStartupRemote serverRemote = getServerStartup();
    hostIsHeadlessBot = serverRemote != null && serverRemote.getIsServerHeadless();
    return hostIsHeadlessBot;
  }

  public boolean getIsServerHeadlessCached() {
    return hostIsHeadlessBot;
  }

  /** Simple data object for which host we are connecting to and with which name. */
  @Getter
  @Builder
  public static class ClientProps {
    @Nonnull private Integer port;
    @Nonnull private String name;
    @Nonnull private String host;
  }
}
