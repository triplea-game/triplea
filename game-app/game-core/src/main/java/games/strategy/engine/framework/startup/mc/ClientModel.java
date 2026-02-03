package games.strategy.engine.framework.startup.mc;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_HOST;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;

import com.google.common.base.Preconditions;
import games.strategy.engine.chat.ChatMessagePanel.ChatSoundProfile;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameState;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.network.ui.ChangeGameOptionsClientAction;
import games.strategy.engine.framework.network.ui.ChangeGameToSaveGameClientAction;
import games.strategy.engine.framework.network.ui.SetMapClientAction;
import games.strategy.engine.framework.startup.launcher.IServerReady;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.ui.ClientOptions;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.ui.background.WaitWindow;
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
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.Component;
import java.awt.Frame;
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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.java.Interruptibles;
import org.triplea.java.ThreadRunner;
import org.triplea.java.concurrency.AsyncRunner;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/** Represents a network aware game client connecting to another game that is acting as a server. */
@Slf4j
public class ClientModel implements IMessengerErrorListener {

  public static final RemoteName CLIENT_READY_CHANNEL =
      new RemoteName(
          "games.strategy.engine.framework.startup.mc.ClientModel.CLIENT_READY_CHANNEL",
          IServerReady.class);
  private final GameObjectStreamFactory objectStreamFactory = new GameObjectStreamFactory(null);
  private final GameSelectorModel gameSelectorModel;
  private final Runnable showSelectType;
  private final WaitWindow gameLoadingWindow;
  private final LaunchAction launchAction;
  private final PlayerTypes.Type clientType;
  private IRemoteModelListener listener = IRemoteModelListener.NULL_LISTENER;
  private Messengers messengers;
  private IClientMessenger messenger;
  private ClientNetworkBridge clientNetworkBridge;
  private Component ui;
  @Getter private ChatPanel chatPanel;
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
          SwingUtilities.invokeLater(
              () -> {
                showSelectType.run();
                EventThreadJOptionPane.showMessageDialog(ui, "Could not join game: " + reason);
              });
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
      final Runnable showSelectType,
      final LaunchAction launchAction,
      final Runnable showMainFrame,
      final Runnable clientLeftGame,
      final PlayerTypes.Type clientType) {
    this.launchAction = launchAction;
    this.showSelectType = showSelectType;
    this.gameSelectorModel = gameSelectorModel;
    this.showMainFrame = showMainFrame;
    this.clientType = clientType;
    this.clientLeftGame = clientLeftGame;
    final Interruptibles.Result<WaitWindow> window =
        Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(WaitWindow::new));
    if (!window.completed) {
      throw new IllegalStateException("Error while creating WaitWindow");
    }
    gameLoadingWindow =
        window.result.orElseThrow(
            () -> new IllegalStateException("Constructor did not return instance"));
  }

  public void setRemoteModelListener(@Nonnull final IRemoteModelListener listener) {
    this.listener = checkNotNull(listener);
    AsyncRunner.runAsync(() -> internalPlayerListingChanged(getServerStartup().getPlayerListing()))
        .exceptionally(e -> log.warn("Network communication error", e));
  }

  private static ClientProps getProps(final Component ui) {
    if (System.getProperty(TRIPLEA_CLIENT, "false").equals("true") && GameState.notStarted()) {
      final ClientProps props =
          ClientProps.builder()
              .host(checkNotNull(System.getProperty(TRIPLEA_HOST)))
              .name(checkNotNull(System.getProperty(TRIPLEA_NAME)))
              .port(Integer.parseInt(checkNotNull(System.getProperty(TRIPLEA_PORT))))
              .build();
      GameState.setStarted();
      return props;
    }
    // load in the saved name!
    final String playername = ClientSetting.playerName.getValueOrThrow();
    final Interruptibles.Result<ClientProps> result =
        Interruptibles.awaitResult(
            () ->
                SwingAction.invokeAndWaitResult(
                    () -> {
                      final ClientOptions options =
                          new ClientOptions(ui, playername, GameRunner.PORT, "127.0.0.1");
                      options.setLocationRelativeTo(ui);
                      options.setVisible(true);
                      options.dispose();
                      if (!options.getOkPressed()) {
                        return null;
                      }
                      return ClientProps.builder()
                          .host(options.getAddress())
                          .name(options.getName())
                          .port(options.getPort())
                          .build();
                    }));
    if (!result.completed) {
      throw new IllegalStateException("Error during component creation of ClientOptions.");
    }
    return result.result.orElse(null);
  }

  /**
   * Factory method to create and connect a client messenger to server, returns false if not
   * connected (user messaging will be handled by this method). Method returns true if successfully
   * connected.
   */
  public boolean createClientMessenger(final Component ui) {
    this.ui = JOptionPane.getFrameForComponent(ui);
    gameDataOnStartup = gameSelectorModel.getGameData();
    gameSelectorModel.setCanSelect(false);
    // load in the saved name!
    final ClientProps props = getProps(this.ui);
    if (props == null) {
      return false;
    }
    ClientSetting.playerName.setValueAndFlush(props.getName());
    final int port = props.getPort();
    if (port >= 65536 || port <= 0) {
      EventThreadJOptionPane.showMessageDialog(
          this.ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    try {
      messenger =
          ClientMessengerFactory.newClientMessenger(
              props,
              objectStreamFactory,
              new ClientLogin(this.ui, ProductVersionReader.getCurrentVersion()));

      if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
        final URI serverURI =
            URI.create("ws://" + props.getHost() + ":" + ServerLauncher.RELAY_SERVER_PORT);
        clientNetworkBridge = new WebsocketNetworkBridge(serverURI);
      } else {
        clientNetworkBridge = ClientNetworkBridge.NO_OP_SENDER;
      }
    } catch (final CouldNotLogInException e) {
      EventThreadJOptionPane.showMessageDialog(this.ui, e.getMessage());
      return false;
    } catch (final Exception ioe) {
      log.info("Error connecting to host", ioe);
      SwingComponents.showError(
          ui,
          "Error Connecting to Host",
          "Error: "
              + ioe.getMessage()
              + "\n\nCheck:\n"
              + "- The host is running\n"
              + "- You have the right port and IP address\n"
              + "- The host should use and can check their public IP address by "
              + "visiting 'whatismyip.com'\n"
              + "- The host can check that they are able to connect to their own game\n"
              + "using their public IP\n"
              + "Additional help can be found on the user-guide: "
              + UrlConstants.USER_GUIDE);
      return false;
    }
    messenger.addErrorListener(this);

    this.messengers = new Messengers(messenger);
    messengers.registerChannelSubscriber(channelListener, IClientChannel.CHANNEL_NAME);

    chatPanel =
        ChatPanel.newChatPanel(
            messengers, ServerModel.CHAT_NAME, ChatSoundProfile.GAME, clientNetworkBridge);
    if (getIsServerHeadlessTest()) {
      gameSelectorModel.setClientModelForHostBots(this);
      chatPanel
          .getChatMessagePanel()
          .addServerMessage(
              "Welcome to an automated dedicated host service (a host bot). "
                  + "\nIf anyone disconnects, the autosave will be reloaded (a save might "
                  + "be loaded right now). "
                  + "\nYou can get the current save, or you can load a save (only saves that "
                  + "it has the map for).");
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

  private IServerStartupRemote getServerStartup() {
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
      if (chatPanel != null) {
        chatPanel.deleteChat();
      }
      hostIsHeadlessBot = false;
      gameSelectorModel.setIsHostHeadlessBot(false);
      gameSelectorModel.setClientModelForHostBots(null);
    }
  }

  private void startGame(
      final byte[] gameData, final Map<String, INode> players, final boolean gameRunning) {
    SwingUtilities.invokeLater(
        () -> {
          gameLoadingWindow.setVisible(true);
          gameLoadingWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(ui));
          gameLoadingWindow.showWait();
        });
    try {
      startGameInNewThread(gameData, players, gameRunning);
    } catch (final RuntimeException e) {
      gameLoadingWindow.doneWait();
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
    ThreadRunner.runInNewThread(
        () -> {
          SwingUtilities.invokeLater(() -> JOptionPane.getFrameForComponent(ui).setVisible(false));
          try {
            // game will be null if we loose the connection
            if (game != null) {
              try {
                data.getGameLoader()
                    .startGame(game, playerSet, launchAction, getChatPanel().getChat());
              } catch (final Exception e) {
                log.error("Failed to start Game", e);
                game.shutDown();
                messenger.shutDown();
                gameLoadingWindow.doneWait();
                // an ugly hack, we need a better way to get the main frame
                clientLeftGame.run();
              }
            }
            if (!gameRunning) {
              ((IServerReady) messengers.getRemote(CLIENT_READY_CHANNEL)).clientReady();
            }
          } finally {
            gameLoadingWindow.doneWait();
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
    if (chatPanel != null) {
      Optional.ofNullable(chatPanel.getChat())
          .ifPresent(chat -> chat.sendMessage("*** Was Disconnected ***"));
    }
    EventThreadJOptionPane.showMessageDialog(
        ui,
        "Connection to game host lost.\nPlease save and restart.",
        "Connection Lost!",
        JOptionPane.ERROR_MESSAGE);
  }

  private boolean getIsServerHeadlessTest() {
    final IServerStartupRemote serverRemote = getServerStartup();
    hostIsHeadlessBot = serverRemote != null && serverRemote.getIsServerHeadless();
    return hostIsHeadlessBot;
  }

  public boolean getIsServerHeadlessCached() {
    return hostIsHeadlessBot;
  }

  public void setMap(final Component parent) {
    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "Should be run on EDT!");
    ThreadRunner.runInNewThread(
        () -> {
          final var action = new SetMapClientAction(parent, getServerStartup());
          SwingUtilities.invokeLater(action::run);
        });
  }

  public void changeGameOptions(final Component parent) {
    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "Should be run on EDT!");
    ThreadRunner.runInNewThread(
        () -> {
          final IServerStartupRemote startupRemote = getServerStartup();
          final byte[] oldBytes = startupRemote.getGameOptions();
          SwingUtilities.invokeLater(
              () -> ChangeGameOptionsClientAction.run(parent, oldBytes, startupRemote));
        });
  }

  public void executeChangeGameToSaveGameClientAction(final Frame owner) {
    ChangeGameToSaveGameClientAction.execute(getServerStartup(), owner);
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
