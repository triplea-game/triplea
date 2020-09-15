package games.strategy.engine.framework.startup.mc;

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
import games.strategy.engine.framework.HeadlessAutoSaveType;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.network.ui.ChangeGameOptionsClientAction;
import games.strategy.engine.framework.network.ui.ChangeGameToSaveGameClientAction;
import games.strategy.engine.framework.network.ui.ChangeToAutosaveClientAction;
import games.strategy.engine.framework.network.ui.GetGameSaveClientAction;
import games.strategy.engine.framework.network.ui.SetMapClientAction;
import games.strategy.engine.framework.startup.launcher.IServerReady;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.ui.ClientOptions;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.framework.startup.ui.panels.main.SetupPanelModel;
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
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.Component;
import java.awt.Frame;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.java.Log;
import org.triplea.io.IoUtils;
import org.triplea.java.Interruptibles;
import org.triplea.java.concurrency.AsyncRunner;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/** Represents a network aware game client connecting to another game that is acting as a server. */
@Log
public class ClientModel implements IMessengerErrorListener {

  public static final RemoteName CLIENT_READY_CHANNEL =
      new RemoteName(
          "games.strategy.engine.framework.startup.mc.ClientModel.CLIENT_READY_CHANNEL",
          IServerReady.class);
  private final GameObjectStreamFactory objectStreamFactory = new GameObjectStreamFactory(null);
  private final GameSelectorModel gameSelectorModel;
  private final SetupPanelModel typePanelModel;
  private final WaitWindow gameLoadingWindow;
  private final LaunchAction launchAction;
  private IRemoteModelListener listener = IRemoteModelListener.NULL_LISTENER;
  private Messengers messengers;
  private IClientMessenger messenger;
  private Component ui;
  private ChatPanel chatPanel;
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
          final CountDownLatch latch = new CountDownLatch(1);
          startGame(gameData, players, latch, true);
          try {
            latch.await(GameRunner.MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME, TimeUnit.SECONDS);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }

        @Override
        public void cannotJoinGame(final String reason) {
          SwingUtilities.invokeLater(
              () -> {
                typePanelModel.showSelectType();
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
          GameRunner.showMainFrame();
        }

        @Override
        public void doneSelectingPlayers(final byte[] gameData, final Map<String, INode> players) {
          final CountDownLatch latch = new CountDownLatch(1);
          startGame(gameData, players, latch, false);
          try {
            latch.await(GameRunner.MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME, TimeUnit.SECONDS);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      };

  public ClientModel(
      final GameSelectorModel gameSelectorModel,
      final SetupPanelModel typePanelModel,
      final LaunchAction launchAction) {
    this.launchAction = launchAction;
    this.typePanelModel = typePanelModel;
    this.gameSelectorModel = gameSelectorModel;
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
    this.listener = Preconditions.checkNotNull(listener);
    AsyncRunner.runAsync(() -> internalPlayerListingChanged(getServerStartup().getPlayerListing()))
        .exceptionally(e -> log.log(Level.WARNING, "Network communication error", e));
  }

  private static ClientProps getProps(final Component ui) {
    if (System.getProperty(TRIPLEA_CLIENT, "false").equals("true") && GameState.notStarted()) {
      final ClientProps props =
          ClientProps.builder()
              .host(System.getProperty(TRIPLEA_HOST))
              .name(System.getProperty(TRIPLEA_NAME))
              .port(Integer.parseInt(System.getProperty(TRIPLEA_PORT)))
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
              props, objectStreamFactory, new ClientLogin(this.ui));
    } catch (final CouldNotLogInException e) {
      EventThreadJOptionPane.showMessageDialog(this.ui, e.getMessage());
      return false;
    } catch (final Exception ioe) {
      log.log(Level.INFO, "Error connecting to host", ioe);
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

    chatPanel = ChatPanel.newChatPanel(messengers, ServerModel.CHAT_NAME, ChatSoundProfile.GAME);
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

  private Collection<String> getAvailableServerGames() {
    return getServerStartup().getAvailableGames();
  }

  /** Resets stats and nulls out references, keeps chat alive. */
  public void cancel() {
    gameSelectorModel.setGameData(gameDataOnStartup);
    gameSelectorModel.setCanSelect(true);
    if (messenger != null) {
      messenger.shutDown();
      messenger.removeErrorListener(this);
      objectStreamFactory.setData(null);
      chatPanel.deleteChat();
      hostIsHeadlessBot = false;
      gameSelectorModel.setIsHostHeadlessBot(false);
      gameSelectorModel.setClientModelForHostBots(null);
    }
  }

  private void startGame(
      final byte[] gameData,
      final Map<String, INode> players,
      final CountDownLatch onDone,
      final boolean gameRunning) {
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
    } finally {
      if (onDone != null) {
        onDone.countDown();
      }
    }
  }

  private void startGameInNewThread(
      final byte[] gameData, final Map<String, INode> players, final boolean gameRunning) {
    final GameData data;
    try {
      // this normally takes a couple seconds, but can take up to 60 seconds for a freaking huge
      // game
      data = IoUtils.readFromMemory(gameData, GameDataManager::loadGame);
    } catch (final IOException ex) {
      log.log(Level.SEVERE, "Failed to load game", ex);
      return;
    }
    objectStreamFactory.setData(data);
    final Map<String, PlayerType> playerMapping =
        playersToNodes.entrySet().stream()
            .filter(e -> e.getValue() != null)
            .filter(e -> e.getValue().equals(messenger.getLocalNode().getName()))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> PlayerType.CLIENT_PLAYER));
    final Set<Player> playerSet = data.getGameLoader().newPlayers(playerMapping);
    game = new ClientGame(data, playerSet, players, messengers);
    new Thread(
            () -> {
              SwingUtilities.invokeLater(
                  () -> JOptionPane.getFrameForComponent(ui).setVisible(false));
              try {
                // game will be null if we loose the connection
                if (game != null) {
                  try {
                    data.getGameLoader()
                        .startGame(game, playerSet, launchAction, getChatPanel().getChat());
                  } catch (final Exception e) {
                    log.log(Level.SEVERE, "Failed to start Game", e);
                    game.shutDown();
                    messenger.shutDown();
                    gameLoadingWindow.doneWait();
                    // an ugly hack, we need a better way to get the main frame
                    GameRunner.clientLeftGame();
                  }
                }
                if (!gameRunning) {
                  ((IServerReady) messengers.getRemote(CLIENT_READY_CHANNEL)).clientReady();
                }
              } finally {
                gameLoadingWindow.doneWait();
              }
            },
            "Client Game Launcher")
        .start();
  }

  public void takePlayer(final String playerName) {
    AsyncRunner.runAsync(() -> getServerStartup().takePlayer(messenger.getLocalNode(), playerName))
        .exceptionally(e -> log.log(Level.WARNING, "Network communication error", e));
  }

  public void releasePlayer(final String playerName) {
    AsyncRunner.runAsync(
            () -> getServerStartup().releasePlayer(messenger.getLocalNode(), playerName))
        .exceptionally(e -> log.log(Level.WARNING, "Network communication error", e));
  }

  public void disablePlayer(final String playerName) {
    AsyncRunner.runAsync(() -> getServerStartup().disablePlayer(playerName))
        .exceptionally(e -> log.log(Level.WARNING, "Network communication error", e));
  }

  public void enablePlayer(final String playerName) {
    AsyncRunner.runAsync(() -> getServerStartup().enablePlayer(playerName))
        .exceptionally(e -> log.log(Level.WARNING, "Network communication error", e));
  }

  private void internalPlayerListingChanged(final PlayerListing listing) {
    gameSelectorModel.clearDataButKeepGameInfo(
        listing.getGameName(), listing.getGameRound(), listing.getGameVersion().toString());
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

  public ChatPanel getChatPanel() {
    return chatPanel;
  }

  private boolean getIsServerHeadlessTest() {
    final IServerStartupRemote serverRemote = getServerStartup();
    hostIsHeadlessBot = serverRemote != null && serverRemote.getIsServerHeadless();
    return hostIsHeadlessBot;
  }

  public boolean getIsServerHeadlessCached() {
    return hostIsHeadlessBot;
  }

  public Action getHostBotSetMapClientAction(final Component parent) {
    return new SetMapClientAction(parent, getServerStartup(), getAvailableServerGames());
  }

  public Action getHostBotChangeGameOptionsClientAction(final Component parent) {
    return new ChangeGameOptionsClientAction(parent, getServerStartup());
  }

  public Action getHostBotChangeGameToSaveGameClientAction(final Frame owner) {
    return new ChangeGameToSaveGameClientAction(getServerStartup(), owner);
  }

  public Action getHostBotChangeToAutosaveClientAction(
      final Component parent, final HeadlessAutoSaveType autosaveType) {
    return new ChangeToAutosaveClientAction(parent, getServerStartup(), autosaveType);
  }

  public Action getHostBotGetGameSaveClientAction(final Component parent) {
    return new GetGameSaveClientAction(parent, getServerStartup(), gameDataOnStartup);
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
