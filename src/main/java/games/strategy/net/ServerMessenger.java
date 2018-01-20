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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.IChatChannel;
import games.strategy.engine.lobby.server.db.MutedMacController;
import games.strategy.engine.lobby.server.db.MutedUsernameController;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.SpokeInvoke;
import games.strategy.net.nio.NioSocket;
import games.strategy.net.nio.NioSocketListener;
import games.strategy.net.nio.QuarantineConversation;
import games.strategy.net.nio.ServerQuarantineConversation;

/**
 * A Messenger that can have many clients connected to it.
 */
public class ServerMessenger implements IServerMessenger, NioSocketListener {
  private static final Logger logger = Logger.getLogger(ServerMessenger.class.getName());
  private final Selector acceptorSelector;
  private final ServerSocketChannel socketChannel;
  private final Node node;
  private boolean shutdown = false;
  private final NioSocket nioSocket;
  private final List<IMessageListener> listeners = new CopyOnWriteArrayList<>();
  private final List<IMessengerErrorListener> errorListeners = new CopyOnWriteArrayList<>();
  private final List<IConnectionChangeListener> connectionListeners = new CopyOnWriteArrayList<>();
  private boolean acceptNewConnection = false;
  private ILoginValidator loginValidator;
  // all our nodes
  private final Map<INode, SocketChannel> nodeToChannel = new ConcurrentHashMap<>();
  private final Map<SocketChannel, INode> channelToNode = new ConcurrentHashMap<>();

  public ServerMessenger(final String name, final int requestedPortNumber) throws IOException {
    this(name, requestedPortNumber, new DefaultObjectStreamFactory());
  }

  // A hack, till I think of something better
  public ServerMessenger(final String name, final int requestedPortNumber, final IObjectStreamFactory streamFactory)
      throws IOException {
    socketChannel = ServerSocketChannel.open();
    socketChannel.configureBlocking(false);
    socketChannel.socket().setReuseAddress(true);
    socketChannel.socket().bind(new InetSocketAddress(requestedPortNumber), 10);
    final int boundPortNumber = socketChannel.socket().getLocalPort();
    nioSocket = new NioSocket(streamFactory, this, "Server");
    acceptorSelector = Selector.open();
    if (IpFinder.findInetAddress() != null) {
      node = new Node(name, IpFinder.findInetAddress(), boundPortNumber);
    } else {
      node = new Node(name, InetAddress.getLocalHost(), boundPortNumber);
    }
    new Thread(new ConnectionHandler(), "Server Messenger Connection Handler").start();
  }

  @Override
  public void setLoginValidator(final ILoginValidator loginValidator) {
    this.loginValidator = loginValidator;
  }

  @Override
  public ILoginValidator getLoginValidator() {
    return loginValidator;
  }

  @Override
  public void addMessageListener(final IMessageListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeMessageListener(final IMessageListener listener) {
    listeners.remove(listener);
  }

  @Override
  public Set<INode> getNodes() {
    final Set<INode> nodes = new HashSet<>(nodeToChannel.keySet());
    nodes.add(node);
    return nodes;
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

  @Override
  public void send(final Serializable msg, final INode to) {
    if (shutdown) {
      return;
    }
    final MessageHeader header = new MessageHeader(to, node, msg);
    final SocketChannel socketChannel = nodeToChannel.get(to);
    // the socket was removed
    if (socketChannel == null) {
      // the socket has not been added yet
      return;
    }
    nioSocket.send(socketChannel, header);
  }

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

  private final Object cachedListLock = new Object();
  private final HashMap<String, String> cachedMacAddresses = new HashMap<>();

  @Override
  public @Nullable String getPlayerMac(final String name) {
    synchronized (cachedListLock) {
      String mac = cachedMacAddresses.get(name);
      if (mac == null) {
        mac = playersThatLeftMacsLast10.get(name);
      }
      return mac;
    }
  }

  // We need to cache whether players are muted, because otherwise the database would have to be accessed each time a
  // message was sent,
  // which can be very slow
  private final List<String> liveMutedUsernames = new ArrayList<>();

  private boolean isUsernameMuted(final String username) {
    synchronized (cachedListLock) {
      return liveMutedUsernames.contains(username);
    }
  }

  @Override
  public void notifyUsernameMutingOfPlayer(final String username, final Instant muteExpires) {
    synchronized (cachedListLock) {
      if (!liveMutedUsernames.contains(username)) {
        liveMutedUsernames.add(username);
      }
      if (muteExpires != null) {
        scheduleUsernameUnmuteAt(username, muteExpires);
      }
    }
  }

  @Override
  public void notifyIpMutingOfPlayer(final String ip, final Instant muteExpires) {
    // TODO: remove if no backwards compat issues
  }

  private final List<String> liveMutedMacAddresses = new ArrayList<>();

  private boolean isMacMuted(final String mac) {
    synchronized (cachedListLock) {
      return liveMutedMacAddresses.contains(mac);
    }
  }

  @Override
  public void notifyMacMutingOfPlayer(final String mac, final Instant muteExpires) {
    synchronized (cachedListLock) {
      if (!liveMutedMacAddresses.contains(mac)) {
        liveMutedMacAddresses.add(mac);
      }
      if (muteExpires != null) {
        scheduleMacUnmuteAt(mac, muteExpires);
      }
    }
  }

  private void scheduleUsernameUnmuteAt(final String username, final Instant checkTime) {
    final Timer unmuteUsernameTimer = new Timer("Username unmute timer");
    unmuteUsernameTimer.schedule(getUsernameUnmuteTask(username),
        checkTime.toEpochMilli() - System.currentTimeMillis());
  }

  private void scheduleMacUnmuteAt(final String mac, final Instant checkTime) {
    final Timer unmuteMacTimer = new Timer("Mac unmute timer");
    unmuteMacTimer.schedule(getMacUnmuteTask(mac), checkTime.toEpochMilli() - System.currentTimeMillis());
  }

  // TODO: remove 'ip' parameter if can confirm no backwards compat issues
  public void notifyPlayerLogin(final String uniquePlayerName, final String ip, final String mac) {
    synchronized (cachedListLock) {
      cachedMacAddresses.put(uniquePlayerName, mac);
      if (isLobby()) {
        final String realName = uniquePlayerName.split(" ")[0];
        if (!liveMutedUsernames.contains(realName)) {
          final Optional<Instant> muteTill = new MutedUsernameController().getUsernameUnmuteTime(realName);
          muteTill.ifPresent(instant -> {
            if (instant.isAfter(Instant.now())) {
              // Signal the player as muted
              liveMutedUsernames.add(realName);
              scheduleUsernameUnmuteAt(realName, instant);
            }
          });
        }
        if (!liveMutedMacAddresses.contains(mac)) {
          final Optional<Instant> muteTill = new MutedMacController().getMacUnmuteTime(mac);
          muteTill.ifPresent(instant -> {
            if (instant.isAfter(Instant.now())) {
              // Signal the player as muted
              liveMutedMacAddresses.add(mac);
              scheduleMacUnmuteAt(mac, instant);
            }
          });
        }
      }
    }
  }

  private final HashMap<String, String> playersThatLeftMacsLast10 = new HashMap<>();

  public HashMap<String, String> getPlayersThatLeftMacs_Last10() {
    return playersThatLeftMacsLast10;
  }

  private void notifyPlayerRemoval(final INode node) {
    synchronized (cachedListLock) {
      playersThatLeftMacsLast10.put(node.getName(), cachedMacAddresses.get(node.getName()));
      if (playersThatLeftMacsLast10.size() > 10) {
        playersThatLeftMacsLast10.remove(playersThatLeftMacsLast10.entrySet().iterator().next().toString());
      }
      cachedMacAddresses.remove(node.getName());
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
    final RemoteName rn;
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
  private final List<String> miniBannedUsernames = new ArrayList<>();

  @Override
  public boolean isUsernameMiniBanned(final String username) {
    synchronized (cachedListLock) {
      return miniBannedUsernames.contains(username);
    }
  }

  @Override
  public void notifyUsernameMiniBanningOfPlayer(final String username, final Instant expires) {
    synchronized (cachedListLock) {
      if (!miniBannedUsernames.contains(username)) {
        miniBannedUsernames.add(username);
      }
      if (expires != null) {
        final Timer unbanUsernameTimer = new Timer("Username unban timer");
        unbanUsernameTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            synchronized (cachedListLock) {
              miniBannedUsernames.remove(username);
            }
          }
        }, expires.toEpochMilli() - System.currentTimeMillis());
      }
    }
  }

  private final List<String> miniBannedIpAddresses = new ArrayList<>();

  @Override
  public boolean isIpMiniBanned(final String ip) {
    synchronized (cachedListLock) {
      return miniBannedIpAddresses.contains(ip);
    }
  }

  @Override
  public void notifyIpMiniBanningOfPlayer(final String ip, final Instant expires) {
    synchronized (cachedListLock) {
      if (!miniBannedIpAddresses.contains(ip)) {
        miniBannedIpAddresses.add(ip);
      }
      if (expires != null) {
        final Timer unbanIpTimer = new Timer("IP unban timer");
        unbanIpTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            synchronized (cachedListLock) {
              miniBannedIpAddresses.remove(ip);
            }
          }
        }, expires.toEpochMilli() - System.currentTimeMillis());
      }
    }
  }

  private final List<String> miniBannedMacAddresses = new ArrayList<>();

  @Override
  public boolean isMacMiniBanned(final String mac) {
    synchronized (cachedListLock) {
      return miniBannedMacAddresses.contains(mac);
    }
  }

  @Override
  public void notifyMacMiniBanningOfPlayer(final String mac, final Instant expires) {
    synchronized (cachedListLock) {
      if (!miniBannedMacAddresses.contains(mac)) {
        miniBannedMacAddresses.add(mac);
      }
      if (expires != null) {
        final Timer unbanMacTimer = new Timer("Mac unban timer");
        unbanMacTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            synchronized (cachedListLock) {
              miniBannedMacAddresses.remove(mac);
            }
          }
        }, Instant.now().until(expires, ChronoUnit.MILLIS));
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
    for (final IMessageListener listener : listeners) {
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
    for (final IConnectionChangeListener listener : connectionListeners) {
      if (added) {
        listener.connectionAdded(node);
      } else {
        listener.connectionRemoved(node);
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
        () -> (isLobby() && !new MutedUsernameController().isUsernameMuted(username)) || isGame(),
        () -> liveMutedUsernames.remove(username));
  }

  private TimerTask createUnmuteTimerTask(final Supplier<Boolean> runCondition, final Runnable action) {
    return new TimerTask() {
      @Override
      public void run() {
        if (runCondition.get()) {
          synchronized (cachedListLock) {
            action.run();
          }
        }
      }
    };
  }

  private TimerTask getMacUnmuteTask(final String mac) {
    return createUnmuteTimerTask(
        () -> (isLobby() && !new MutedMacController().isMacMuted(mac)) || isGame(),
        () -> liveMutedMacAddresses.remove(mac));
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
      logger.warning("Could not remove connection to node:" + nodeToRemove);
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
