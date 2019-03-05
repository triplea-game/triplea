package games.strategy.net;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.Serializable;
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
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

import javax.annotation.Nullable;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.IChatChannel;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.SpokeInvoke;
import games.strategy.net.nio.NioSocket;
import games.strategy.net.nio.NioSocketListener;
import games.strategy.net.nio.QuarantineConversation;
import games.strategy.net.nio.ServerQuarantineConversation;
import lombok.extern.java.Log;

/**
 * A Messenger that can have many clients connected to it.
 */
@Log
public abstract class AbstractServerMessenger implements IServerMessenger, NioSocketListener {
  private final Selector acceptorSelector;
  private final ServerSocketChannel socketChannel;
  private final Node node;
  private boolean shutdown = false;
  private final NioSocket nioSocket;
  private final List<IMessageListener> listeners = new CopyOnWriteArrayList<>();
  private final List<IConnectionChangeListener> connectionListeners = new CopyOnWriteArrayList<>();
  private boolean acceptNewConnection = false;
  private ILoginValidator loginValidator;
  // all our nodes
  private final Map<INode, SocketChannel> nodeToChannel = new ConcurrentHashMap<>();
  private final Map<SocketChannel, INode> channelToNode = new ConcurrentHashMap<>();
  private final Object cachedListLock = new Object();
  private final Map<String, String> cachedMacAddresses = new HashMap<>();
  private final List<String> miniBannedIpAddresses = new ArrayList<>();
  // We need to cache whether players are muted, because otherwise the database would have to be accessed each time a
  // message was sent, which can be very slow
  private final List<String> liveMutedUsernames = new ArrayList<>();
  private final List<String> miniBannedMacAddresses = new ArrayList<>();
  private final List<String> liveMutedMacAddresses = new ArrayList<>();
  // The following code is used in hosted lobby games by the host for player mini-banning and mini-muting
  private final List<String> miniBannedUsernames = new ArrayList<>();
  private final Map<String, String> playersThatLeftMacsLast10 = new HashMap<>();

  protected AbstractServerMessenger(final String name, final int port, final IObjectStreamFactory objectStreamFactory)
      throws IOException {
    socketChannel = ServerSocketChannel.open();
    socketChannel.configureBlocking(false);
    socketChannel.socket().setReuseAddress(true);
    socketChannel.socket().bind(new InetSocketAddress(port), 10);
    final int boundPort = socketChannel.socket().getLocalPort();
    nioSocket = new NioSocket(objectStreamFactory, this);
    acceptorSelector = Selector.open();
    node = new Node(name, IpFinder.findInetAddress(), boundPort);
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

  @Override
  public boolean isConnected() {
    return !shutdown;
  }

  @Override
  public void send(final Serializable msg, final INode to) {
    if (shutdown) {
      return;
    }
    final SocketChannel socketChannel = nodeToChannel.get(to);
    // the socket was removed
    if (socketChannel == null) {
      // the socket has not been added yet
      return;
    }
    nioSocket.send(socketChannel, new MessageHeader(to, node, msg));
  }

  @Override
  public @Nullable String getPlayerMac(final String name) {
    synchronized (cachedListLock) {
      return Optional.ofNullable(cachedMacAddresses.get(name))
          .orElseGet(() -> playersThatLeftMacsLast10.get(name));
    }
  }

  private boolean isUsernameMutedInCache(final String username) {
    synchronized (cachedListLock) {
      return liveMutedUsernames.contains(username);
    }
  }

  @Override
  public void notifyUsernameMutingOfPlayer(final String username, final @Nullable Instant muteExpires) {
    synchronized (cachedListLock) {
      if (!liveMutedUsernames.contains(username)) {
        liveMutedUsernames.add(username);
      }
      if (muteExpires != null) {
        scheduleUsernameUnmuteAt(username, muteExpires);
      }
    }
  }

  private boolean isMacMutedInCache(final String mac) {
    synchronized (cachedListLock) {
      return liveMutedMacAddresses.contains(mac);
    }
  }

  @Override
  public void notifyMacMutingOfPlayer(final String mac, final @Nullable Instant muteExpires) {
    synchronized (cachedListLock) {
      if (!liveMutedMacAddresses.contains(mac)) {
        liveMutedMacAddresses.add(mac);
      }
      if (muteExpires != null) {
        scheduleMacUnmuteAt(mac, muteExpires);
      }
    }
  }

  private void scheduleUsernameUnmuteAt(final String username, final Instant expires) {
    final Timer unmuteUsernameTimer = new Timer("Username unmute timer");
    unmuteUsernameTimer.schedule(
        newUnmuteTimerTask(() -> isUsernameMutedInBackingStore(username), () -> liveMutedUsernames.remove(username)),
        millisBetweenNowAnd(expires));
  }

  /**
   * Returns {@code true} if the user associated with the specified username is muted according to the backing store
   * (e.g. a database); otherwise {@code false}.
   *
   * <p>
   * Subclasses may override and are not required to call the superclass implementation. This implementation returns
   * {@code false} indicating the user is not currently muted.
   * </p>
   *
   * @param username The username of the user.
   */
  protected boolean isUsernameMutedInBackingStore(final String username) {
    return false;
  }

  private TimerTask newUnmuteTimerTask(final BooleanSupplier isUserMuted, final Runnable unmuteUser) {
    return new TimerTask() {
      @Override
      public void run() {
        if (!isUserMuted.getAsBoolean()) {
          synchronized (cachedListLock) {
            unmuteUser.run();
          }
        }
      }
    };
  }

  private static long millisBetweenNowAnd(final Instant end) {
    return Math.max(0, ChronoUnit.MILLIS.between(Instant.now(), end));
  }

  private void scheduleMacUnmuteAt(final String mac, final Instant expires) {
    final Timer unmuteMacTimer = new Timer("Mac unmute timer");
    unmuteMacTimer.schedule(
        newUnmuteTimerTask(() -> isMacMutedInBackingStore(mac), () -> liveMutedMacAddresses.remove(mac)),
        millisBetweenNowAnd(expires));
  }

  /**
   * Returns {@code true} if the user associated with the specified MAC is muted according to the backing store (e.g. a
   * database); otherwise {@code false}.
   *
   * <p>
   * Subclasses may override and are not required to call the superclass implementation. This implementation returns
   * {@code false} indicating the user is not currently muted.
   * </p>
   *
   * @param mac The MAC of the user.
   */
  protected boolean isMacMutedInBackingStore(final String mac) {
    return false;
  }

  /**
   * Invoked when the node with the specified unique name has successfully logged in. Note that {@code uniquePlayerName}
   * is the node name and may not be identical to the name of the player associated with the node (see
   * {@link #getUniqueName(String)}.
   */
  public void notifyPlayerLogin(final String uniquePlayerName, final String mac) {
    synchronized (cachedListLock) {
      cachedMacAddresses.put(uniquePlayerName, mac);
      final String realName = IServerMessenger.getRealName(uniquePlayerName);
      if (!liveMutedUsernames.contains(realName)) {
        final Optional<Instant> muteTill = getUsernameUnmuteTime(realName);
        muteTill.ifPresent(instant -> {
          if (instant.isAfter(Instant.now())) {
            // Signal the player as muted
            liveMutedUsernames.add(realName);
            scheduleUsernameUnmuteAt(realName, instant);
          }
        });
      }
      if (!liveMutedMacAddresses.contains(mac)) {
        final Optional<Instant> muteTill = getMacUnmuteTime(mac);
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

  /**
   * Returns the instant at which the user associated with the specified username is to be unmuted or empty if the user
   * is not currently muted.
   *
   * <p>
   * Subclasses may override and are not required to call the superclass implementation. This implementation returns an
   * empty instant indicating the user is not currently muted.
   * </p>
   *
   * @param username The username of the user.
   */
  protected Optional<Instant> getUsernameUnmuteTime(final String username) {
    return Optional.empty();
  }

  /**
   * Returns the instant at which the user associated with the specified MAC is to be unmuted or empty if the user is
   * not currently muted.
   *
   * <p>
   * Subclasses may override and are not required to call the superclass implementation. This implementation returns an
   * empty instant indicating the user is not currently muted.
   * </p>
   *
   * @param mac The MAC of the user.
   */
  protected Optional<Instant> getMacUnmuteTime(final String mac) {
    return Optional.empty();
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

  @Override
  public void messageReceived(final MessageHeader msg, final SocketChannel channel) {
    final INode expectedReceive = channelToNode.get(channel);
    if (!expectedReceive.equals(msg.getFrom())) {
      throw new IllegalStateException("Expected: " + expectedReceive + " not: " + msg.getFrom());
    }
    if (msg.getMessage() instanceof HubInvoke) { // Chat messages are always HubInvoke's
      if (((HubInvoke) msg.getMessage()).call.getRemoteName().equals(getChatControlChannelName())) {
        final String realName = IServerMessenger.getRealName(msg.getFrom().getName());
        if (isUsernameMutedInCache(realName) || isMacMutedInCache(getPlayerMac(msg.getFrom().getName()))) {
          bareBonesSendChatMessage(getAdministrativeMuteChatMessage(), msg.getFrom());
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

  private String getChatControlChannelName() {
    return ChatController.getChatChannelName(getChatChannelName());
  }

  /**
   * Returns the name of the chat channel.
   */
  protected abstract String getChatChannelName();

  /**
   * Returns the administrative chat message to send a user who has been muted.
   *
   * @see games.strategy.engine.chat.AdministrativeChatMessages
   */
  protected abstract String getAdministrativeMuteChatMessage();

  private void bareBonesSendChatMessage(final String message, final INode to) {
    final RemoteName rn = new RemoteName(getChatControlChannelName(), IChatChannel.class);
    final RemoteMethodCall call = new RemoteMethodCall(
        rn.getName(),
        "chatOccured",
        new Object[] {message},
        new Class<?>[] {String.class},
        IChatChannel.class);
    final SpokeInvoke spokeInvoke = new SpokeInvoke(null, false, call, getServerNode());
    send(spokeInvoke, to);
  }

  @Override
  public boolean isUsernameMiniBanned(final String username) {
    synchronized (cachedListLock) {
      return miniBannedUsernames.contains(username);
    }
  }

  @Override
  public void notifyUsernameMiniBanningOfPlayer(final String username, final @Nullable Instant expires) {
    synchronized (cachedListLock) {
      if (!miniBannedUsernames.contains(username)) {
        miniBannedUsernames.add(username);
      }
      if (expires != null) {
        final Timer unbanUsernameTimer = new Timer("Username unban timer");
        unbanUsernameTimer.schedule(
            newUnbanTimerTask(() -> miniBannedUsernames.remove(username)),
            millisBetweenNowAnd(expires));
      }
    }
  }

  private TimerTask newUnbanTimerTask(final Runnable unbanUser) {
    return new TimerTask() {
      @Override
      public void run() {
        synchronized (cachedListLock) {
          unbanUser.run();
        }
      }
    };
  }

  @Override
  public boolean isIpMiniBanned(final String ip) {
    synchronized (cachedListLock) {
      return miniBannedIpAddresses.contains(ip);
    }
  }

  @Override
  public void notifyIpMiniBanningOfPlayer(final String ip, final @Nullable Instant expires) {
    synchronized (cachedListLock) {
      if (!miniBannedIpAddresses.contains(ip)) {
        miniBannedIpAddresses.add(ip);
      }
      if (expires != null) {
        final Timer unbanIpTimer = new Timer("IP unban timer");
        unbanIpTimer.schedule(
            newUnbanTimerTask(() -> miniBannedIpAddresses.remove(ip)),
            millisBetweenNowAnd(expires));
      }
    }
  }

  @Override
  public boolean isMacMiniBanned(final String mac) {
    synchronized (cachedListLock) {
      return miniBannedMacAddresses.contains(mac);
    }
  }

  @Override
  public void notifyMacMiniBanningOfPlayer(final String mac, final @Nullable Instant expires) {
    synchronized (cachedListLock) {
      if (!miniBannedMacAddresses.contains(mac)) {
        miniBannedMacAddresses.add(mac);
      }
      if (expires != null) {
        final Timer unbanMacTimer = new Timer("Mac unban timer");
        unbanMacTimer.schedule(
            newUnbanTimerTask(() -> miniBannedMacAddresses.remove(mac)),
            millisBetweenNowAnd(expires));
      }
    }
  }

  private void forward(final MessageHeader msg) {
    if (shutdown) {
      return;
    }
    final SocketChannel socketChannel = nodeToChannel.get(msg.getFor());
    if (socketChannel == null) {
      throw new IllegalStateException("No channel for:" + msg.getFor());
    }
    nioSocket.send(socketChannel, msg);
  }

  private void forwardBroadcast(final MessageHeader msg) {
    if (shutdown) {
      return;
    }
    final SocketChannel fromChannel = nodeToChannel.get(msg.getFrom());
    final List<SocketChannel> nodes = new ArrayList<>(nodeToChannel.values());
    log.finest(() -> "broadcasting to" + nodes);
    for (final SocketChannel channel : nodes) {
      if (channel != fromChannel) {
        nioSocket.send(channel, msg);
      }
    }
  }

  private boolean isNameTaken(final String nodeName) {
    return getNodes().stream()
        .map(INode::getName)
        .anyMatch(nodeName::equalsIgnoreCase);
  }

  /**
   * Returns a node name, based on the specified node name, that is unique across all nodes. The node name is made
   * unique by adding a numbered suffix to the existing node name. For example, for the second node with the name "foo",
   * this method will return "foo (1)".
   */
  public String getUniqueName(final String name) {
    String currentName = name;
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
  public void addErrorListener(final IMessengerErrorListener listener) {}

  @Override
  public void removeErrorListener(final IMessengerErrorListener listener) {}

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
  public INode getLocalNode() {
    return node;
  }

  private class ConnectionHandler implements Runnable {
    @Override
    public void run() {
      try {
        socketChannel.register(acceptorSelector, SelectionKey.OP_ACCEPT);
      } catch (final ClosedChannelException e) {
        log.log(Level.SEVERE, "socket closed", e);
        shutDown();
      }
      while (!shutdown) {
        try {
          acceptorSelector.select();
        } catch (final IOException e) {
          log.log(Level.SEVERE, "Could not accept on server", e);
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
              log.log(Level.FINE, "Could not accept channel", e);
              try {
                if (socketChannel != null) {
                  socketChannel.close();
                }
              } catch (final IOException e2) {
                log.log(Level.FINE, "Could not close channel", e2);
              }
              continue;
            }
            // we are not accepting connections
            if (!acceptNewConnection) {
              try {
                socketChannel.close();
              } catch (final IOException e) {
                log.log(Level.FINE, "Could not close channel", e);
              }
              continue;
            }
            final ServerQuarantineConversation conversation = new ServerQuarantineConversation(
                loginValidator,
                socketChannel,
                nioSocket,
                AbstractServerMessenger.this);
            nioSocket.add(socketChannel, conversation);
          } else if (!key.isValid()) {
            key.cancel();
          }
        }
      }
    }
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
      log.warning("Could not remove connection to node:" + nodeToRemove);
      return;
    }
    channelToNode.remove(channel);
    nioSocket.close(channel);
    notifyConnectionsChanged(false, nodeToRemove);
    log.info("Connection removed:" + nodeToRemove);
  }

  @Override
  public INode getServerNode() {
    return node;
  }

  @Override
  public void socketError(final SocketChannel channel, final Exception error) {
    checkNotNull(channel);

    // already closed, don't report it again
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
    log.info("Connection added to:" + remote);
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
    return getClass().getSimpleName() + " LocalNode:" + node + " ClientNodes:" + nodeToChannel.keySet();
  }
}
