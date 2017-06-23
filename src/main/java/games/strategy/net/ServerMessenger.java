package games.strategy.net;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.IChatChannel;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.userDB.MutedMacController;
import games.strategy.engine.lobby.server.userDB.MutedUsernameController;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.SpokeInvoke;
import games.strategy.net.nio.NIOSocket;
import games.strategy.net.nio.NIOSocketListener;
import games.strategy.net.nio.QuarantineConversation;
import games.strategy.net.nio.ServerQuarantineConversation;

/**
 * A Messenger that can have many clients connected to it.
 */
public class ServerMessenger implements IServerMessenger, NIOSocketListener {
  private static Logger logger = Logger.getLogger(ServerMessenger.class.getName());
  private final Selector acceptorSelector;
  private final ServerSocketChannel socketChannel;
  private final Node node;
  private boolean shutdown = false;
  private final NIOSocket nioSocket;
  private final List<IMessageListener> listeners = new CopyOnWriteArrayList<>();
  private final List<IMessengerErrorListener> errorListeners =
      new CopyOnWriteArrayList<>();
  private final List<IConnectionChangeListener> connectionListeners =
      new CopyOnWriteArrayList<>();
  private boolean acceptNewConnection = false;
  private ILoginValidator loginValidator;
  // all our nodes
  private final Map<INode, SocketChannel> nodeToChannel = new ConcurrentHashMap<>();
  private final Map<SocketChannel, INode> channelToNode = new ConcurrentHashMap<>();

  // A hack, till I think of something better
  public ServerMessenger(final String name, final int portNumber, final IObjectStreamFactory streamFactory)
      throws IOException {
    socketChannel = ServerSocketChannel.open();
    socketChannel.configureBlocking(false);
    socketChannel.socket().setReuseAddress(true);
    socketChannel.socket().bind(new InetSocketAddress(portNumber), 10);
    nioSocket = new NIOSocket(streamFactory, this, "Server");
    acceptorSelector = Selector.open();
    if (IPFinder.findInetAddress() != null) {
      node = new Node(name, IPFinder.findInetAddress(), portNumber);
    } else {
      node = new Node(name, InetAddress.getLocalHost(), portNumber);
    }
    final Thread t = new Thread(new ConnectionHandler(), "Server Messenger Connection Handler");
    t.start();
  }

  @Override
  public void setLoginValidator(final ILoginValidator loginValidator) {
    this.loginValidator = loginValidator;
  }

  @Override
  public ILoginValidator getLoginValidator() {
    return loginValidator;
  }

  /** Creates new ServerMessenger. */
  public ServerMessenger(final String name, final int portNumber) throws IOException {
    this(name, portNumber, new DefaultObjectStreamFactory());
  }

  @Override
  public void addMessageListener(final IMessageListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeMessageListener(final IMessageListener listener) {
    listeners.remove(listener);
  }

  /**
   * Get a list of nodes.
   */
  @Override
  public Set<INode> getNodes() {
    final Set<INode> rVal = new HashSet<>(nodeToChannel.keySet());
    rVal.add(node);
    return rVal;
  }

  @Override
  public synchronized void shutDown() {
    if (!shutdown) {
      shutdown = true;
      nioSocket.shutDown();
      try {
        socketChannel.close();
      } catch (final Exception e) {
        // ignore
      }
      if (acceptorSelector != null) {
        acceptorSelector.wakeup();
      }
    }
  }

  public synchronized boolean isShutDown() {
    return shutdown;
  }

  @Override
  public boolean isConnected() {
    return !shutdown;
  }

  /**
   * Send a message to the given node.
   */
  @Override
  public void send(final Serializable msg, final INode to) {
    if (shutdown) {
      return;
    }
    if (logger.isLoggable(Level.FINEST)) {
      logger.log(Level.FINEST, "Sending" + msg + " to:" + to);
    }
    final MessageHeader header = new MessageHeader(to, node, msg);
    final SocketChannel socketChannel = nodeToChannel.get(to);
    // the socket was removed
    if (socketChannel == null) {
      if (logger.isLoggable(Level.FINER)) {
        logger.log(Level.FINER, "no channel for node:" + to + " dropping message:" + msg);
      }
      // the socket has not been added yet
      return;
    }
    nioSocket.send(socketChannel, header);
  }

  /**
   * Send a message to all nodes.
   */
  @Override
  public void broadcast(final Serializable msg) {
    final MessageHeader header = new MessageHeader(node, msg);
    forwardBroadcast(header);
  }

  private boolean isLobby() {
    return loginValidator instanceof LobbyLoginValidator;
  }

  private boolean isGame() {
    return !isLobby();
  }

  private final Object m_cachedListLock = new Object();
  private final HashMap<String, String> m_cachedMacAddresses = new HashMap<>();

  @Override
  public String getPlayerMac(final String name) {
    synchronized (m_cachedListLock) {
      String mac = m_cachedMacAddresses.get(name);
      if (mac == null) {
        mac = m_playersThatLeftMacs_Last10.get(name);
      }
      return mac;
    }
  }

  // We need to cache whether players are muted, because otherwise the database would have to be accessed each time a
  // message was sent,
  // which can be very slow
  private final List<String> m_liveMutedUsernames = new ArrayList<>();

  private boolean isUsernameMuted(final String username) {
    synchronized (m_cachedListLock) {
      return m_liveMutedUsernames.contains(username);
    }
  }

  @Override
  public void NotifyUsernameMutingOfPlayer(final String username, final Date muteExpires) {
    synchronized (m_cachedListLock) {
      if (!m_liveMutedUsernames.contains(username)) {
        m_liveMutedUsernames.add(username);
      }
      if (muteExpires != null) {
        scheduleUsernameUnmuteAt(username, muteExpires.getTime());
      }
    }
  }

  @Override
  public void NotifyIPMutingOfPlayer(final String ip, final Date muteExpires) {
    // TODO: remove if no backwards compat issues
  }

  private final List<String> m_liveMutedMacAddresses = new ArrayList<>();

  private boolean isMacMuted(final String mac) {
    synchronized (m_cachedListLock) {
      return m_liveMutedMacAddresses.contains(mac);
    }
  }

  @Override
  public void NotifyMacMutingOfPlayer(final String mac, final Date muteExpires) {
    synchronized (m_cachedListLock) {
      if (!m_liveMutedMacAddresses.contains(mac)) {
        m_liveMutedMacAddresses.add(mac);
      }
      if (muteExpires != null) {
        scheduleMacUnmuteAt(mac, muteExpires.getTime());
      }
    }
  }

  private void scheduleUsernameUnmuteAt(final String username, final long checkTime) {
    final Timer unmuteUsernameTimer = new Timer("Username unmute timer");
    unmuteUsernameTimer.schedule(getUsernameUnmuteTask(username), new Date(checkTime));
  }

  private void scheduleMacUnmuteAt(final String mac, final long checkTime) {
    final Timer unmuteMacTimer = new Timer("Mac unmute timer");
    unmuteMacTimer.schedule(getMacUnmuteTask(mac), new Date(checkTime));
  }

  // TODO: remove 'ip' parameter if can confirm no backwards compat issues
  public void notifyPlayerLogin(final String uniquePlayerName, final String ip, final String mac) {
    synchronized (m_cachedListLock) {
      m_cachedMacAddresses.put(uniquePlayerName, mac);
      if (isLobby()) {
        final String realName = uniquePlayerName.split(" ")[0];
        if (!m_liveMutedUsernames.contains(realName)) {
          final long muteTill = new MutedUsernameController().getUsernameUnmuteTime(realName);
          if (muteTill != -1 && muteTill <= System.currentTimeMillis()) {
            // Signal the player as muted
            m_liveMutedUsernames.add(realName);
            scheduleUsernameUnmuteAt(realName, muteTill);
          }
        }
        if (!m_liveMutedMacAddresses.contains(mac)) {
          final long muteTill = new MutedMacController().getMacUnmuteTime(mac);
          if (muteTill != -1 && muteTill <= System.currentTimeMillis()) {
            // Signal the player as muted
            m_liveMutedMacAddresses.add(mac);
            scheduleMacUnmuteAt(mac, muteTill);
          }
        }
      }
    }
  }

  private final HashMap<String, String> m_playersThatLeftMacs_Last10 = new HashMap<>();

  public HashMap<String, String> getPlayersThatLeftMacs_Last10() {
    return m_playersThatLeftMacs_Last10;
  }

  private void notifyPlayerRemoval(final INode node) {
    synchronized (m_cachedListLock) {
      m_playersThatLeftMacs_Last10.put(node.getName(), m_cachedMacAddresses.get(node.getName()));
      if (m_playersThatLeftMacs_Last10.size() > 10) {
        m_playersThatLeftMacs_Last10.remove(m_playersThatLeftMacs_Last10.entrySet().iterator().next().toString());
      }
      m_cachedMacAddresses.remove(node.getName());
    }
  }

  // Special character to stop spoofing by server
  public static final String YOU_HAVE_BEEN_MUTED_LOBBY =
      "?YOUR LOBBY CHATTING HAS BEEN TEMPORARILY 'MUTED' BY THE ADMINS, TRY AGAIN LATER";

  // Special character to stop spoofing by host
  public static final String YOU_HAVE_BEEN_MUTED_GAME = "?YOUR CHATTING IN THIS GAME HAS BEEN 'MUTED' BY THE HOST";

  @Override
  public void messageReceived(final MessageHeader msg, final SocketChannel channel) {
    final INode expectedReceive = channelToNode.get(channel);
    if (!expectedReceive.equals(msg.getFrom())) {
      throw new IllegalStateException("Expected: " + expectedReceive + " not: " + msg.getFrom());
    }
    if (msg.getMessage() instanceof HubInvoke) { // Chat messages are always HubInvoke's
      if (isLobby() && ((HubInvoke) msg.getMessage()).call.getRemoteName().equals("_ChatCtrl_LOBBY_CHAT")) {
        final String realName = msg.getFrom().getName().split(" ")[0];
        if (isUsernameMuted(realName)) {
          bareBonesSendChatMessage(YOU_HAVE_BEEN_MUTED_LOBBY, msg.getFrom());
          return;
        } else if (isMacMuted(getPlayerMac(msg.getFrom().getName()))) {
          bareBonesSendChatMessage(YOU_HAVE_BEEN_MUTED_LOBBY, msg.getFrom());
          return;
        }
      } else if (isGame() && ((HubInvoke) msg.getMessage()).call.getRemoteName()
          .equals("_ChatCtrlgames.strategy.engine.framework.ui.ServerStartup.CHAT_NAME")) {
        final String realName = msg.getFrom().getName().split(" ")[0];
        if (isUsernameMuted(realName)) {
          bareBonesSendChatMessage(YOU_HAVE_BEEN_MUTED_GAME, msg.getFrom());
          return;
        }
        if (isMacMuted(getPlayerMac(msg.getFrom().getName()))) {
          bareBonesSendChatMessage(YOU_HAVE_BEEN_MUTED_GAME, msg.getFrom());
          return;
        }
      }
    }
    if (msg.getFor() == null) {
      forwardBroadcast(msg);
      notifyListeners(msg);
    } else if (msg.getFor().equals(node)) {
      notifyListeners(msg);
    } else {
      forward(msg);
    }
  }

  private void bareBonesSendChatMessage(final String message, final INode to) {
    final List<Object> args = new ArrayList<>();
    final Class<? extends Object>[] argTypes = new Class<?>[1];
    args.add(message);
    argTypes[0] = args.get(0).getClass();
    RemoteName rn;
    if (isLobby()) {
      rn = new RemoteName(ChatController.getChatChannelName("_LOBBY_CHAT"), IChatChannel.class);
    } else {
      rn = new RemoteName(
          ChatController.getChatChannelName("games.strategy.engine.framework.ui.ServerStartup.CHAT_NAME"),
          IChatChannel.class);
    }
    final RemoteMethodCall call =
        new RemoteMethodCall(rn.getName(), "chatOccured", args.toArray(), argTypes, rn.getClazz());
    final SpokeInvoke spokeInvoke = new SpokeInvoke(null, false, call, getServerNode());
    send(spokeInvoke, to);
  }

  // The following code is used in hosted lobby games by the host for player mini-banning and mini-muting
  private final List<String> m_miniBannedUsernames = new ArrayList<>();

  @Override
  public boolean IsUsernameMiniBanned(final String username) {
    synchronized (m_cachedListLock) {
      return m_miniBannedUsernames.contains(username);
    }
  }

  @Override
  public void NotifyUsernameMiniBanningOfPlayer(final String username, final Date expires) {
    synchronized (m_cachedListLock) {
      if (!m_miniBannedUsernames.contains(username)) {
        m_miniBannedUsernames.add(username);
      }
      if (expires != null) {
        final Timer unbanUsernameTimer = new Timer("Username unban timer");
        unbanUsernameTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            synchronized (m_cachedListLock) {
              m_miniBannedUsernames.remove(username);
            }
          }
        }, new Date(expires.getTime()));
      }
    }
  }

  private final List<String> m_miniBannedIpAddresses = new ArrayList<>();

  @Override
  public boolean IsIpMiniBanned(final String ip) {
    synchronized (m_cachedListLock) {
      return m_miniBannedIpAddresses.contains(ip);
    }
  }

  @Override
  public void NotifyIPMiniBanningOfPlayer(final String ip, final Date expires) {
    synchronized (m_cachedListLock) {
      if (!m_miniBannedIpAddresses.contains(ip)) {
        m_miniBannedIpAddresses.add(ip);
      }
      if (expires != null) {
        final Timer unbanIpTimer = new Timer("IP unban timer");
        unbanIpTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            synchronized (m_cachedListLock) {
              m_miniBannedIpAddresses.remove(ip);
            }
          }
        }, new Date(expires.getTime()));
      }
    }
  }

  private final List<String> m_miniBannedMacAddresses = new ArrayList<>();

  @Override
  public boolean IsMacMiniBanned(final String mac) {
    synchronized (m_cachedListLock) {
      return m_miniBannedMacAddresses.contains(mac);
    }
  }

  @Override
  public void NotifyMacMiniBanningOfPlayer(final String mac, final Date expires) {
    synchronized (m_cachedListLock) {
      if (!m_miniBannedMacAddresses.contains(mac)) {
        m_miniBannedMacAddresses.add(mac);
      }
      if (expires != null) {
        final Timer unbanMacTimer = new Timer("Mac unban timer");
        unbanMacTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            synchronized (m_cachedListLock) {
              m_miniBannedMacAddresses.remove(mac);
            }
          }
        }, new Date(expires.getTime()));
      }
    }
  }

  private void forward(final MessageHeader msg) {
    if (shutdown) {
      return;
    }
    final SocketChannel socketChannel = nodeToChannel.get(msg.getFor());
    if (socketChannel == null) {
      throw new IllegalStateException("No channel for:" + msg.getFor() + " all channels:" + socketChannel);
    }
    nioSocket.send(socketChannel, msg);
  }

  private void forwardBroadcast(final MessageHeader msg) {
    if (shutdown) {
      return;
    }
    final SocketChannel fromChannel = nodeToChannel.get(msg.getFrom());
    final List<SocketChannel> nodes = new ArrayList<>(nodeToChannel.values());
    if (logger.isLoggable(Level.FINEST)) {
      logger.log(Level.FINEST, "broadcasting to" + nodes);
    }
    for (final SocketChannel channel : nodes) {
      if (channel != fromChannel) {
        nioSocket.send(channel, msg);
      }
    }
  }

  private boolean isNameTaken(final String nodeName) {
    for (final INode node : getNodes()) {
      if (node.getName().equalsIgnoreCase(nodeName)) {
        return true;
      }
    }
    return false;
  }

  public String getUniqueName(String currentName) {
    if (currentName.length() > 50) {
      currentName = currentName.substring(0, 50);
    }
    if (currentName.length() < 2) {
      currentName = "aa" + currentName;
    }
    synchronized (node) {
      if (isNameTaken(currentName)) {
        int i = 1;
        while (true) {
          final String newName = currentName + " (" + i + ")";
          if (!isNameTaken(newName)) {
            currentName = newName;
            break;
          }
          i++;
        }
      }
    }
    return currentName;
  }

  private void notifyListeners(final MessageHeader msg) {
    final Iterator<IMessageListener> iter = listeners.iterator();
    while (iter.hasNext()) {
      final IMessageListener listener = iter.next();
      listener.messageReceived(msg.getMessage(), msg.getFrom());
    }
  }

  @Override
  public void addErrorListener(final IMessengerErrorListener listener) {
    errorListeners.add(listener);
  }

  @Override
  public void removeErrorListener(final IMessengerErrorListener listener) {
    errorListeners.remove(listener);
  }

  @Override
  public void addConnectionChangeListener(final IConnectionChangeListener listener) {
    connectionListeners.add(listener);
  }

  @Override
  public void removeConnectionChangeListener(final IConnectionChangeListener listener) {
    connectionListeners.remove(listener);
  }

  private void notifyConnectionsChanged(final boolean added, final INode node) {
    final Iterator<IConnectionChangeListener> iter = connectionListeners.iterator();
    while (iter.hasNext()) {
      if (added) {
        iter.next().connectionAdded(node);
      } else {
        iter.next().connectionRemoved(node);
      }
    }
  }

  @Override
  public void setAcceptNewConnections(final boolean accept) {
    acceptNewConnection = accept;
  }

  @Override
  public boolean isAcceptNewConnections() {
    return acceptNewConnection;
  }

  @Override
  public INode getLocalNode() {
    return node;
  }

  private class ConnectionHandler implements Runnable {
    @Override
    public void run() {
      try {
        socketChannel.register(acceptorSelector, SelectionKey.OP_ACCEPT);
      } catch (final ClosedChannelException e) {
        logger.log(Level.SEVERE, "socket closed", e);
        shutDown();
      }
      while (!shutdown) {
        try {
          acceptorSelector.select();
        } catch (final IOException e) {
          logger.log(Level.SEVERE, "Could not accept on server", e);
          shutDown();
        }
        if (shutdown) {
          continue;
        }
        final Set<SelectionKey> keys = acceptorSelector.selectedKeys();
        final Iterator<SelectionKey> iter = keys.iterator();
        while (iter.hasNext()) {
          final SelectionKey key = iter.next();
          iter.remove();
          if (key.isAcceptable() && key.isValid()) {
            final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            // Accept the connection and make it non-blocking
            SocketChannel socketChannel = null;
            try {
              socketChannel = serverSocketChannel.accept();
              if (socketChannel == null) {
                continue;
              }
              socketChannel.configureBlocking(false);
              socketChannel.socket().setKeepAlive(true);
            } catch (final IOException e) {
              logger.log(Level.FINE, "Could not accept channel", e);
              try {
                if (socketChannel != null) {
                  socketChannel.close();
                }
              } catch (final IOException e2) {
                logger.log(Level.FINE, "Could not close channel", e2);
              }
              continue;
            }
            // we are not accepting connections
            if (!acceptNewConnection) {
              try {
                socketChannel.close();
              } catch (final IOException e) {
                logger.log(Level.FINE, "Could not close channel", e);
              }
              continue;
            }
            final ServerQuarantineConversation conversation =
                new ServerQuarantineConversation(loginValidator, socketChannel, nioSocket, ServerMessenger.this);
            nioSocket.add(socketChannel, conversation);
          } else if (!key.isValid()) {
            key.cancel();
          }
        }
      }
    }
  }

  private TimerTask getUsernameUnmuteTask(final String username) {
    return createUnmuteTimerTask(
        () -> (isLobby() && new MutedUsernameController().getUsernameUnmuteTime(username) == -1) || (isGame()),
        () -> m_liveMutedUsernames.remove(username));
  }

  private TimerTask createUnmuteTimerTask(final Supplier<Boolean> runCondition, final Runnable action) {
    return new TimerTask() {
      @Override
      public void run() {
        if (runCondition.get()) {
          synchronized (m_cachedListLock) {
            action.run();
          }
        }
      }
    };
  }

  private TimerTask getMacUnmuteTask(final String mac) {
    return createUnmuteTimerTask(
        () -> (isLobby() && new MutedMacController().getMacUnmuteTime(mac) == -1) || (isGame()),
        () -> m_liveMutedMacAddresses.remove(mac));
  }

  @Override
  public boolean isServer() {
    return true;
  }

  @Override
  public void removeConnection(final INode nodeToRemove) {
    if (nodeToRemove.equals(this.node)) {
      throw new IllegalArgumentException("Cant remove ourself!");
    }
    notifyPlayerRemoval(nodeToRemove);
    final SocketChannel channel = nodeToChannel.remove(nodeToRemove);
    if (channel == null) {
      logger.info("Could not remove connection to node:" + nodeToRemove);
      return;
    }
    channelToNode.remove(channel);
    nioSocket.close(channel);
    notifyConnectionsChanged(false, nodeToRemove);
    logger.info("Connection removed:" + nodeToRemove);
  }

  @Override
  public INode getServerNode() {
    return node;
  }

  @Override
  public void socketError(final SocketChannel channel, final Exception error) {
    if (channel == null) {
      throw new IllegalArgumentException("Null channel");
    }
    // already closed, dont report it again
    final INode node = channelToNode.get(channel);
    if (node != null) {
      removeConnection(node);
    }
  }

  @Override
  public void socketUnqaurantined(final SocketChannel channel, final QuarantineConversation conversation) {
    final ServerQuarantineConversation con = (ServerQuarantineConversation) conversation;
    final INode remote = new Node(con.getRemoteName(), (InetSocketAddress) channel.socket().getRemoteSocketAddress());
    if (logger.isLoggable(Level.FINER)) {
      logger.log(Level.FINER, "Unquarntined node:" + remote);
    }
    nodeToChannel.put(remote, channel);
    channelToNode.put(channel, remote);
    notifyConnectionsChanged(true, remote);
    logger.info("Connection added to:" + remote);
  }

  @Override
  public INode getRemoteNode(final SocketChannel channel) {
    return channelToNode.get(channel);
  }

  @Override
  public InetSocketAddress getRemoteServerSocketAddress() {
    return node.getSocketAddress();
  }

  @Override
  public String toString() {
    return "ServerMessenger LocalNode:" + node + " ClientNodes:" + nodeToChannel.keySet();
  }
}
