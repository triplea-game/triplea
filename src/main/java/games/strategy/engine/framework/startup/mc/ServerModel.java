package games.strategy.engine.framework.startup.mc;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
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
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.lobby.server.NullModeratorController;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.ServerMessenger;
import games.strategy.util.Version;

public class ServerModel extends Observable implements IMessengerErrorListener, IConnectionChangeListener {
  public static final RemoteName SERVER_REMOTE_NAME =
      new RemoteName("games.strategy.engine.framework.ui.ServerStartup.SERVER_REMOTE", IServerStartupRemote.class);

  public enum InteractionMode {
    HEADLESS, SWING_CLIENT_UI
  }

  static final String CHAT_NAME = "games.strategy.engine.framework.ui.ServerStartup.CHAT_NAME";
  static final String PLAYERNAME = "PlayerName";

  static RemoteName getObserverWaitingToStartName(final INode node) {
    return new RemoteName("games.strategy.engine.framework.startup.mc.ServerModel.OBSERVER" + node.getName(),
        IObserverWaitingToJoin.class);
  }

  private static Logger logger = Logger.getLogger(ServerModel.class.getName());
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
  private final Map<String, String> localPlayerTypes = new HashMap<>();
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

  public void shutDown() {
    gameSelectorModel.deleteObserver(gameSelectorObserver);
    if (serverMessenger != null) {
      chatController.deactivate();
      serverMessenger.shutDown();
      serverMessenger.removeErrorListener(this);
      chatPanel.shutDown();
    }
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

  public void setRemoteModelListener(IRemoteModelListener listener) {
    if (listener == null) {
      listener = IRemoteModelListener.NULL_LISTENER;
    }
    remoteModelListener = listener;
  }

  public void setLocalPlayerType(final String player, final String type) {
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
              // the 2nd in the list should be Weak AI
              final int indexPosition =
                  Math.max(0, Math.min(data.getGameLoader().getServerPlayerTypes().length - 1, 1));
              localPlayerTypes.put(name, data.getGameLoader().getServerPlayerTypes()[indexPosition]);
            } else {
              // we generally do not want a headless host bot to be doing any AI turns, since that
              // is taxing on the system
              playersToNodeListing.put(name, null);
            }
          } else {
            playersToNodeListing.put(name, serverMessenger.getLocalNode().getName());
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

  private ServerProps getServerProps(final Component ui) {
    if (System.getProperties().getProperty(GameRunner.TRIPLEA_SERVER_PROPERTY, "false").equals("true")
        && System.getProperties().getProperty(GameRunner.TRIPLEA_STARTED, "").equals("")) {
      final ServerProps props = new ServerProps();
      props.setName(System.getProperty(GameRunner.TRIPLEA_NAME_PROPERTY));
      props.setPort(Integer.parseInt(System.getProperty(GameRunner.TRIPLEA_PORT_PROPERTY)));
      if (System.getProperty(GameRunner.TRIPLEA_SERVER_PASSWORD_PROPERTY) != null) {
        props.setPassword(System.getProperty(GameRunner.TRIPLEA_SERVER_PASSWORD_PROPERTY));
      }
      System.setProperty(GameRunner.TRIPLEA_STARTED, "true");
      return props;
    }
    final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    final String playername = prefs.get(PLAYERNAME, System.getProperty("user.name"));
    final ServerOptions options = new ServerOptions(ui, playername, GameRunner.PORT, false);
    options.setLocationRelativeTo(ui);
    options.setVisible(true);
    options.dispose();
    if (!options.getOKPressed()) {
      return null;
    }
    final String name = options.getName();
    logger.log(Level.FINE, "Server playing as:" + name);
    // save the name! -- lnxduk
    prefs.put(PLAYERNAME, name);
    final int port = options.getPort();
    if (port >= 65536 || port == 0) {
      if (headless) {
        System.out.println("Invalid Port: " + port);
      } else {
        JOptionPane.showMessageDialog(ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
      }
      return null;
    }
    final ServerProps props = new ServerProps();
    props.setName(options.getName());
    props.setPort(options.getPort());
    props.setPassword(options.getPassword());
    return props;
  }

  /**
   * UI can be null. We use it as the parent for message dialogs we show.
   * If you have a component displayed, use it.
   */
  public boolean createServerMessenger(Component ui) {
    ui = ui == null ? null : JOptionPane.getFrameForComponent(ui);
    this.ui = ui;
    final ServerProps props = getServerProps(ui);
    if (props == null) {
      return false;
    }
    try {
      serverMessenger = new ServerMessenger(props.getName(), props.getPort(), objectStreamFactory);
      final ClientLoginValidator clientLoginValidator = new ClientLoginValidator(serverMessenger);
      clientLoginValidator.setGamePassword(props.getPassword());
      serverMessenger.setLoginValidator(clientLoginValidator);
      serverMessenger.addErrorListener(this);
      serverMessenger.addConnectionChangeListener(this);
      final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(serverMessenger);
      remoteMessenger = new RemoteMessenger(unifiedMessenger);
      remoteMessenger.registerRemote(m_serverStartupRemote, SERVER_REMOTE_NAME);
      channelMessenger = new ChannelMessenger(unifiedMessenger);
      final NullModeratorController moderatorController = new NullModeratorController(serverMessenger, null);
      moderatorController.register(remoteMessenger);
      chatController =
          new ChatController(CHAT_NAME, serverMessenger, remoteMessenger, channelMessenger, moderatorController);

      if (ui == null && headless) {
        chatPanel = new HeadlessChat(serverMessenger, channelMessenger, remoteMessenger, CHAT_NAME,
            Chat.CHAT_SOUND_PROFILE.GAME_CHATROOM);
      } else {
        chatPanel = new ChatPanel(serverMessenger, channelMessenger, remoteMessenger, CHAT_NAME,
            Chat.CHAT_SOUND_PROFILE.GAME_CHATROOM);
      }

      serverMessenger.setAcceptNewConnections(true);
      gameDataChanged();
      return true;
    } catch (final IOException ioe) {
      ioe.printStackTrace(System.out);
      if (headless) {
        System.out.println("Unable to create server socket:" + ioe.getMessage());
      } else {
        JOptionPane.showMessageDialog(ui, "Unable to create server socket:" + ioe.getMessage(), "Error",
            JOptionPane.ERROR_MESSAGE);
      }
      return false;
    }
  }

  private final IServerStartupRemote m_serverStartupRemote = new IServerStartupRemote() {
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
      } else {
        return false;
      }
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
      System.out.println("Sending save game");

      byte[] bytes = null;
      try (final ByteArrayOutputStream sink = new ByteArrayOutputStream(5000)) {
        new GameDataManager().saveGame(sink, data);
        bytes = sink.toByteArray();
      } catch (final IOException e) {
        ClientLogger.logQuietly(e);
        throw new IllegalStateException(e);
      }
      return bytes;
    }

    @Override
    public byte[] getGameOptions() {
      byte[] bytes = null;
      if (data == null || data.getProperties() == null || data.getProperties().getEditableProperties() == null
          || data.getProperties().getEditableProperties().isEmpty()) {
        return bytes;
      }
      final List<IEditableProperty> currentEditableProperties = data.getProperties().getEditableProperties();

      try (final ByteArrayOutputStream sink = new ByteArrayOutputStream(1000)) {
        GameProperties.toOutputStream(sink, currentEditableProperties);
        bytes = sink.toByteArray();
      } catch (final IOException e) {
        ClientLogger.logQuietly(e);
      }
      return bytes;
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
      System.out.println("Changing to game map: " + gameName);
      headless.setGameMapTo(gameName);
    }

    @Override
    public void changeToLatestAutosave(final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave) {
      final HeadlessGameServer headless = HeadlessGameServer.getInstance();
      if (headless == null) {
        return;
      }
      final File save;
      if (SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE.equals(typeOfAutosave)) {
        save = new File(ClientContext.folderSettings().getSaveGamePath(), SaveGameFileChooser.getAutoSaveFileName());
      } else if (SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_ODD.equals(typeOfAutosave)) {
        save = new File(ClientContext.folderSettings().getSaveGamePath(), SaveGameFileChooser.getAutoSaveOddFileName());
      } else if (SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_EVEN.equals(typeOfAutosave)) {
        save =
            new File(ClientContext.folderSettings().getSaveGamePath(), SaveGameFileChooser.getAutoSaveEvenFileName());
      } else {
        return;
      }
      if (save == null || !save.exists()) {
        return;
      }
      System.out.println("Changing to autosave of type: " + typeOfAutosave.toString());
      headless.loadGameSave(save);
    }

    @Override
    public void changeToGameSave(final byte[] bytes, final String fileName) {
      // TODO: change to a string message return, so we can tell the user/requestor if it was successful or not, and why
      // if not.
      final HeadlessGameServer headless = HeadlessGameServer.getInstance();
      if (headless == null || bytes == null) {
        return;
      }
      System.out.println("Changing to user savegame: " + fileName);
      try (ByteArrayInputStream input = new ByteArrayInputStream(bytes);
          InputStream oinput = new BufferedInputStream(input);) {
        headless.loadGameSave(oinput, fileName);
      } catch (final Exception e) {
        ClientLogger.logQuietly(e);
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
      System.out.println("Changing to user game options.");
      try {
        headless.loadGameOptions(bytes);
      } catch (final Exception e) {
        ClientLogger.logQuietly(e);
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
      } else {
        return new PlayerListing(new HashMap<>(playersToNodeListing),
            new HashMap<>(playersEnabledListing), getLocalPlayerTypes(), data.getGameVersion(),
            data.getGameName(), data.getSequence().getRound() + "",
            new HashSet<>(playersAllowedToBeDisabled), playerNamesAndAlliancesInTurnOrder);
      }
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
          localPlayerTypes.put(playerName,
              data.getGameLoader().getServerPlayerTypes()[Math.max(0,
                  // the 2nd in the list should be Weak AI
                  Math.min(data.getGameLoader().getServerPlayerTypes().length - 1, 1))]);
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
    final IClientChannel channel =
        (IClientChannel) channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
    channel.playerListingChanged(getPlayerListingInternal());
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
      System.out.println("Connection Lost");
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
      } catch (final InterruptedException e) { // no worries
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

  public void disallowRemoveConnections() {
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

  public Map<String, String> getLocalPlayerTypes() {
    final Map<String, String> localPlayerMappings = new HashMap<>();
    if (data == null) {
      return localPlayerMappings;
    }
    // local player default = humans (for bots = weak ai)
    final String defaultLocalType = headless
        ? data.getGameLoader().getServerPlayerTypes()[Math.max(0,
            Math.min(data.getGameLoader().getServerPlayerTypes().length - 1, 1))]
        : data.getGameLoader().getServerPlayerTypes()[0];
    for (final String player : playersToNodeListing.keySet()) {
      final String playedBy = playersToNodeListing.get(player);
      if (playedBy == null) {
        continue;
      }
      if (playedBy.equals(serverMessenger.getLocalNode().getName())) {
        String type = defaultLocalType;
        if (localPlayerTypes.containsKey(player)) {
          type = localPlayerTypes.get(player);
        }
        localPlayerMappings.put(player, type);
      }
    }
    return localPlayerMappings;
  }

  public ILauncher getLauncher() {
    synchronized (this) {
      disallowRemoveConnections();
      // -1 since we dont count outselves
      final int clientCount = serverMessenger.getNodes().size() - 1;
      final Map<String, INode> remotePlayers = new HashMap<>();
      for (final String player : playersToNodeListing.keySet()) {
        final String playedBy = playersToNodeListing.get(player);
        if (playedBy == null) {
          return null;
        }
        if (!playedBy.equals(serverMessenger.getLocalNode().getName())) {
          final Set<INode> nodes = serverMessenger.getNodes();
          for (final INode node : nodes) {
            if (node.getName().equals(playedBy)) {
              remotePlayers.put(player, node);
              break;
            }
          }
        }
      }
      final ServerLauncher launcher = new ServerLauncher(clientCount, remoteMessenger, channelMessenger,
          serverMessenger, gameSelectorModel, getPlayerListingInternal(), remotePlayers, this, headless);
      return launcher;
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
    final StringBuilder sb = new StringBuilder();
    sb.append("ServerModel GameData:").append(data == null ? "null" : data.getGameName()).append("\n");
    sb.append("Connected:").append(serverMessenger == null ? "null" : serverMessenger.isConnected()).append("\n");
    sb.append(serverMessenger);
    sb.append("\n");
    sb.append(remoteMessenger);
    sb.append("\n");
    sb.append(channelMessenger);
    return sb.toString();
  }
}


class ServerProps {
  private String name;
  private int port;
  private String password;

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
    this.port = port;
  }
}
