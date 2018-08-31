package games.strategy.engine.framework.startup.mc;

import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JOptionPane;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.HeadlessChat;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameState;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.io.IoUtils;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingAction;
import games.strategy.util.Interruptibles;
import games.strategy.util.Version;
import lombok.extern.java.Log;

@Log
public class ServerModel extends Observable implements IMessengerErrorListener, IConnectionChangeListener {
  public static final RemoteName SERVER_REMOTE_NAME =
      new RemoteName("games.strategy.engine.framework.ui.ServerStartup.SERVER_REMOTE", IServerStartupRemote.class);

  public enum InteractionMode {
    HEADLESS, SWING_CLIENT_UI
  }

  static final String CHAT_NAME = "games.strategy.engine.framework.ui.ServerStartup.CHAT_NAME";

  static RemoteName getObserverWaitingToStartName(final INode node) {
    return new RemoteName("games.strategy.engine.framework.startup.mc.ServerModel.OBSERVER" + node.getName(),
        IObserverWaitingToJoin.class);
  }

  private final GameObjectStreamFactory objectStreamFactory = new GameObjectStreamFactory(null);
  private final SetupPanelModel typePanelModel;
  private final boolean headless;
  private IServerMessenger serverMessenger;
  private IRemoteMessenger remoteMessenger;
  private IChannelMessenger channelMessenger;
  private GameData data;
  private Map<String, String> playersToNodeListing = new HashMap<>();
  private Map<String, Boolean> playersEnabledListing = new HashMap<>();
  private Collection<String> playersAllowedToBeDisabled = new HashSet<>();
  private Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder =
      new LinkedHashMap<>();
  private IRemoteModelListener remoteModelListener = IRemoteModelListener.NULL_LISTENER;
  private final GameSelectorModel gameSelectorModel;
  private Component ui;
  private IChatPanel chatPanel;
  private ChatController chatController;
  private final Map<String, PlayerType> localPlayerTypes = new HashMap<>();
  // while our server launcher is not null, delegate new/lost connections to it
  private volatile ServerLauncher serverLauncher;
  private CountDownLatch removeConnectionsLatch = null;
  private final Observer gameSelectorObserver = (observable, value) -> gameDataChanged();

  ServerModel(final GameSelectorModel gameSelectorModel, final SetupPanelModel typePanelModel) {
    this(gameSelectorModel, typePanelModel, InteractionMode.SWING_CLIENT_UI);
  }

  public ServerModel(final GameSelectorModel gameSelectorModel, final SetupPanelModel typePanelModel,
      final InteractionMode interactionMode) {
    this.gameSelectorModel = gameSelectorModel;
    this.typePanelModel = typePanelModel;
    this.gameSelectorModel.addObserver(gameSelectorObserver);
    headless = (interactionMode == InteractionMode.HEADLESS);
  }

  public void cancel() {
    gameSelectorModel.deleteObserver(gameSelectorObserver);
    if (serverMessenger != null) {
      chatController.deactivate();
      serverMessenger.shutDown();
      serverMessenger.removeErrorListener(this);
      chatPanel.setChat(null);
    }
  }

  public void setRemoteModelListener(final @Nullable IRemoteModelListener listener) {
    remoteModelListener = Optional.ofNullable(listener).orElse(IRemoteModelListener.NULL_LISTENER);
  }

  public void setLocalPlayerType(final String player, final PlayerType type) {
    synchronized (this) {
      localPlayerTypes.put(player, type);
    }
  }

  private void gameDataChanged() {
    synchronized (this) {
      data = gameSelectorModel.getGameData();
      if (data != null) {
        playersToNodeListing = new HashMap<>();
        playersEnabledListing = new HashMap<>();
        playersAllowedToBeDisabled = new HashSet<>(data.getPlayerList().getPlayersThatMayBeDisabled());
        playerNamesAndAlliancesInTurnOrder = new LinkedHashMap<>();
        for (final PlayerID player : data.getPlayerList().getPlayers()) {
          final String name = player.getName();
          if (headless) {
            if (player.getIsDisabled()) {
              playersToNodeListing.put(name, serverMessenger.getLocalNode().getName());
              localPlayerTypes.put(name, PlayerType.WEAK_AI);
            } else {
              // we generally do not want a headless host bot to be doing any AI turns, since that
              // is taxing on the system
              playersToNodeListing.put(name, null);
            }
          } else {
            Optional.ofNullable(serverMessenger)
                .ifPresent(messenger -> playersToNodeListing.put(name, messenger.getLocalNode().getName()));
          }
          playerNamesAndAlliancesInTurnOrder.put(name, data.getAllianceTracker().getAlliancesPlayerIsIn(player));
          playersEnabledListing.put(name, !player.getIsDisabled());
        }
      }
      objectStreamFactory.setData(data);
      localPlayerTypes.clear();
    }
    notifyChanellPlayersChanged();
    remoteModelListener.playerListChanged();
  }

  private Optional<ServerConnectionProps> getServerProps(final Component ui) {
    if (System.getProperty(TRIPLEA_SERVER, "false").equals("true")
        && GameState.notStarted()) {
      GameState.setStarted();
      return Optional.of(ServerConnectionProps.builder()
          .name(System.getProperty(TRIPLEA_NAME))
          .port(Integer.parseInt(System.getProperty(TRIPLEA_PORT)))
          .password(System.getProperty(SERVER_PASSWORD))
          .build());
    }
    final String playername = ClientSetting.PLAYER_NAME.value();
    final Interruptibles.Result<ServerOptions> optionsResult = Interruptibles
        .awaitResult(() -> SwingAction.invokeAndWaitResult(() -> {
          final ServerOptions options = new ServerOptions(ui, playername, GameRunner.PORT, false);
          options.setLocationRelativeTo(ui);
          options.setVisible(true);
          options.dispose();
          if (!options.getOkPressed()) {
            return null;
          }
          final String name = options.getName();
          log.fine("Server playing as:" + name);
          ClientSetting.PLAYER_NAME.save(name);
          ClientSetting.flush();
          final int port = options.getPort();
          if (port >= 65536 || port == 0) {
            if (headless) {
              throw new IllegalStateException("Invalid Port: " + port);
            }
            JOptionPane.showMessageDialog(ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
            return null;
          }
          return options;
        }));
    if (!optionsResult.completed) {
      throw new IllegalArgumentException("Error while gathering connection details");
    }
    return optionsResult.result.map(options -> ServerConnectionProps.builder()
        .name(options.getName())
        .port(options.getPort())
        .password(options.getPassword())
        .build());
  }

  /**
   * Creates a server messenger, returns false if any errors happen.
   *
   * @param ui In non-headless environments we get input from user, the component is for window placements.
   */
  public boolean createServerMessenger(final Component ui) {
    return getServerProps(ui)
        .map(props -> createServerMessenger(ui, props))
        .orElse(false);
  }

  /**
   * UI can be null. We use it as the parent for message dialogs we show.
   * If you have a component displayed, use it.
   */
  boolean createServerMessenger(
      @Nullable final Component ui,
      @Nonnull final ServerConnectionProps props) {
    this.ui = (ui == null) ? null : JOptionPane.getFrameForComponent(ui);

    try {
      serverMessenger = new GameServerMessenger(props.getName(), props.getPort(), objectStreamFactory);
      final ClientLoginValidator clientLoginValidator = new ClientLoginValidator(serverMessenger);
      clientLoginValidator.setGamePassword(props.getPassword());
      serverMessenger.setLoginValidator(clientLoginValidator);
      serverMessenger.addErrorListener(this);
      serverMessenger.addConnectionChangeListener(this);
      final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(serverMessenger);
      remoteMessenger = new RemoteMessenger(unifiedMessenger);
      remoteMessenger.registerRemote(serverStartupRemote, SERVER_REMOTE_NAME);
      channelMessenger = new ChannelMessenger(unifiedMessenger);
      chatController = new ChatController(CHAT_NAME, serverMessenger, remoteMessenger, channelMessenger, node -> false);

      if (ui == null && headless) {
        chatPanel = new HeadlessChat(serverMessenger, channelMessenger, remoteMessenger, CHAT_NAME,
            Chat.ChatSoundProfile.GAME_CHATROOM);
      } else {
        chatPanel = ChatPanel.createChatPanel(serverMessenger, channelMessenger, remoteMessenger, CHAT_NAME,
            Chat.ChatSoundProfile.GAME_CHATROOM);
      }

      serverMessenger.setAcceptNewConnections(true);
      gameDataChanged();
      return true;
    } catch (final IOException ioe) {
      log.log(Level.SEVERE, "Unable to create server socket", ioe);
      return false;
    }
  }

  private final IServerStartupRemote serverStartupRemote = new IServerStartupRemote() {
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
      if (!headless) {
        return;
      }
      // we don't want the client's changing stuff for anyone but a bot
      setPlayerEnabled(playerName, false);
    }

    @Override
    public void enablePlayer(final String playerName) {
      if (!headless) {
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
            (IObserverWaitingToJoin) remoteMessenger.getRemote(remoteName);
        final IObserverWaitingToJoin observerWaitingToJoinNonBlocking =
            (IObserverWaitingToJoin) remoteMessenger.getRemote(remoteName, true);
        serverLauncher.addObserver(observerWaitingToJoinBlocking, observerWaitingToJoinNonBlocking, newNode);
        return true;
      }
      return false;
    }

    @Override
    public boolean getIsServerHeadless() {
      return HeadlessGameServer.headless();
    }

    /**
     * This should not be called from within game, only from the game setup screen, while everyone is waiting for game
     * to start.
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
      if (data == null || data.getProperties() == null || data.getProperties().getEditableProperties() == null
          || data.getProperties().getEditableProperties().isEmpty()) {
        return null;
      }
      final List<IEditableProperty> currentEditableProperties = data.getProperties().getEditableProperties();

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
    public void changeToLatestAutosave(final SaveGameFileChooser.AUTOSAVE_TYPE autoSaveType) {
      if (HeadlessGameServer.getInstance() == null || autoSaveType == SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE2) {
        return;
      }
      final File save = new File(ClientSetting.SAVE_GAMES_FOLDER_PATH.value(), autoSaveType.getFileName());
      if (!save.exists()) {
        return;
      }
      HeadlessGameServer.getInstance().loadGameSave(save);
    }

    @Override
    public void changeToGameSave(final byte[] bytes, final String fileName) {
      // TODO: change to a string message return, so we can tell the user/requestor if it was successful or not, and why
      // if not.
      final HeadlessGameServer headless = HeadlessGameServer.getInstance();
      if (headless == null || bytes == null) {
        return;
      }
      try {
        IoUtils.consumeFromMemory(bytes, is -> {
          try (InputStream oinput = new BufferedInputStream(is)) {
            headless.loadGameSave(oinput, fileName);
          }
        });
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Failed to load save game: " + fileName, e);
      }
    }

    @Override
    public void changeToGameOptions(final byte[] bytes) {
      // TODO: change to a string message return, so we can tell the user/requestor if it was successful or not, and why
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

  private PlayerListing getPlayerListingInternal() {
    synchronized (this) {
      if (data == null) {
        return new PlayerListing(new HashMap<>(), new HashMap<>(playersEnabledListing),
            getLocalPlayerTypes(), new Version(0, 0), gameSelectorModel.getGameName(),
            gameSelectorModel.getGameRound(), new HashSet<>(playersAllowedToBeDisabled),
            new LinkedHashMap<>());
      }
      return new PlayerListing(new HashMap<>(playersToNodeListing),
          new HashMap<>(playersEnabledListing), getLocalPlayerTypes(), data.getGameVersion(),
          data.getGameName(), data.getSequence().getRound() + "",
          new HashSet<>(playersAllowedToBeDisabled), playerNamesAndAlliancesInTurnOrder);
    }
  }

  private void takePlayerInternal(final INode from, final boolean take, final String playerName) {
    // synchronize to make sure two adds arent executed at once
    synchronized (this) {
      if (!playersToNodeListing.containsKey(playerName)) {
        return;
      }
      if (take) {
        playersToNodeListing.put(playerName, from.getName());
      } else {
        playersToNodeListing.put(playerName, null);
      }
    }
    notifyChanellPlayersChanged();
    remoteModelListener.playersTakenChanged();
  }

  private void setPlayerEnabled(final String playerName, final boolean enabled) {
    takePlayerInternal(serverMessenger.getLocalNode(), true, playerName);
    // synchronize
    synchronized (this) {
      if (!playersEnabledListing.containsKey(playerName)) {
        return;
      }
      playersEnabledListing.put(playerName, enabled);
      if (headless) {
        // we do not want the host bot to actually play, so set to null if enabled, and set to weak ai if disabled
        if (enabled) {
          playersToNodeListing.put(playerName, null);
        } else {
          localPlayerTypes.put(playerName, PlayerType.WEAK_AI);
        }
      }
    }
    notifyChanellPlayersChanged();
    remoteModelListener.playersTakenChanged();
  }

  public void setAllPlayersToNullNodes() {
    if (playersToNodeListing != null) {
      for (final String p : playersToNodeListing.keySet()) {
        playersToNodeListing.put(p, null);
      }
    }
  }

  private void notifyChanellPlayersChanged() {
    Optional.ofNullable(channelMessenger)
        .ifPresent(messenger -> {
          final IClientChannel channel =
              (IClientChannel) messenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
          channel.playerListingChanged(getPlayerListingInternal());
        });
  }

  public void takePlayer(final String playerName) {
    takePlayerInternal(serverMessenger.getLocalNode(), true, playerName);
  }

  public void releasePlayer(final String playerName) {
    takePlayerInternal(serverMessenger.getLocalNode(), false, playerName);
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

  public Map<String, String> getPlayersToNodeListing() {
    synchronized (this) {
      return new HashMap<>(playersToNodeListing);
    }
  }

  public Map<String, Boolean> getPlayersEnabledListing() {
    synchronized (this) {
      return new HashMap<>(playersEnabledListing);
    }
  }

  public Collection<String> getPlayersAllowedToBeDisabled() {
    synchronized (this) {
      return new HashSet<>(playersAllowedToBeDisabled);
    }
  }

  public Map<String, Collection<String>> getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap() {
    synchronized (this) {
      return new LinkedHashMap<>(playerNamesAndAlliancesInTurnOrder);
    }
  }

  @Override
  public void messengerInvalid(final IMessenger messenger, final Exception reason) {
    if (headless) {
      if (typePanelModel != null) {
        typePanelModel.showSelectType();
      }
    } else {
      JOptionPane.showMessageDialog(ui, "Connection lost", "Error", JOptionPane.ERROR_MESSAGE);
      typePanelModel.showSelectType();
    }
  }

  @Override
  public void connectionAdded(final INode to) {}

  @Override
  public void connectionRemoved(final INode node) {
    if (removeConnectionsLatch != null) {
      try {
        removeConnectionsLatch.await(6, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
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

  public IChatPanel getChatPanel() {
    return chatPanel;
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
      return Collections.emptyMap();
    }

    final Map<String, PlayerType> localPlayerMappings = new HashMap<>();
    // local player default = humans (for bots = weak ai)
    final PlayerType defaultLocalType = headless
        ? PlayerType.WEAK_AI
        : PlayerType.HUMAN_PLAYER;
    for (final String player : playersToNodeListing.keySet()) {
      final String playedBy = playersToNodeListing.get(player);
      if (playedBy == null) {
        continue;
      }
      if (playedBy.equals(serverMessenger.getLocalNode().getName())) {
        localPlayerMappings.put(player, localPlayerTypes.getOrDefault(player, defaultLocalType));
      }
    }
    return localPlayerMappings;
  }

  public Optional<ServerLauncher> getLauncher() {
    synchronized (this) {
      disallowRemoveConnections();
      // -1 since we dont count outselves
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
      return Optional.of(new ServerLauncher(clientCount, remoteMessenger, channelMessenger,
          serverMessenger, gameSelectorModel, getPlayerListingInternal(), remotePlayers, this, headless));
    }
  }

  public void newGame() {
    serverMessenger.setAcceptNewConnections(true);
    final IClientChannel channel =
        (IClientChannel) channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
    notifyChanellPlayersChanged();
    channel.gameReset();
  }

  public void setServerLauncher(final ServerLauncher launcher) {
    serverLauncher = launcher;
  }

  @Override
  public String toString() {
    return "ServerModel GameData:" + (data == null ? "null" : data.getGameName()) + "\n"
        + "Connected:" + (serverMessenger == null ? "null" : serverMessenger.isConnected()) + "\n"
        + serverMessenger
        + "\n"
        + remoteMessenger
        + "\n"
        + channelMessenger;
  }
}
