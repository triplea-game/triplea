package games.strategy.net;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.mc.messages.ModeratorPromoted;
import games.strategy.net.nio.NioSocket;
import games.strategy.net.nio.NioSocketListener;
import games.strategy.net.nio.QuarantineConversation;
import games.strategy.net.nio.ServerQuarantineConversation;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.client.connections.GameToLobbyConnection;
import org.triplea.java.IpAddressParser;
import org.triplea.java.ThreadRunner;

/** A Messenger that can have many clients connected to it. */
@Slf4j
public class ServerMessenger implements IServerMessenger, NioSocketListener {
  public final Object newNodeLock = new Object();

  private final Selector acceptorSelector;
  private final ServerSocketChannel socketChannel;
  private final Node node;
  private boolean shutdown = false;
  private final NioSocket nioSocket;
  private final List<IMessageListener> listeners = new CopyOnWriteArrayList<>();
  private final List<IConnectionChangeListener> connectionListeners = new CopyOnWriteArrayList<>();
  private boolean acceptNewConnection = false;

  @Getter(onMethod_ = {@Override})
  @Setter(onMethod_ = {@Override})
  private ILoginValidator loginValidator;

  // all our nodes
  private final Map<INode, SocketChannel> nodeToChannel = new ConcurrentHashMap<>();
  private final Map<SocketChannel, INode> channelToNode = new ConcurrentHashMap<>();
  // list that keeps track of the order in which nodes have joined. Nodes are removed as they leave
  private final List<INode> nodeJoinOrder = new LinkedList<>();

  private final Map<UserName, String> cachedMacAddresses = new ConcurrentHashMap<>();
  private final Set<String> miniBannedIpAddresses = new ConcurrentSkipListSet<>();
  private final Set<String> miniBannedMacAddresses = new ConcurrentSkipListSet<>();

  @Setter @Nullable private GameToLobbyConnection gameToLobbyConnection;

  public ServerMessenger(
      final String name, final int port, final IObjectStreamFactory objectStreamFactory)
      throws IOException {
    socketChannel = ServerSocketChannel.open();
    socketChannel.configureBlocking(false);
    socketChannel.socket().setReuseAddress(true);
    socketChannel.socket().bind(new InetSocketAddress(port), 10);
    final int boundPort = socketChannel.socket().getLocalPort();
    nioSocket = new NioSocket(objectStreamFactory, this);
    acceptorSelector = Selector.open();
    node = new Node(name, IpFinder.findInetAddress(), boundPort);
    nodeJoinOrder.add(node);
    ThreadRunner.runInNewThread(new ConnectionHandler());
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
  public @Nullable String getPlayerMac(final UserName name) {
    return cachedMacAddresses.get(name);
  }

  /**
   * Invoked when the node with the specified unique name has successfully logged in. Note that
   * {@code uniquePlayerName} is the node name and may not be identical to the name of the player
   * associated with the node
   */
  public void notifyPlayerLogin(final UserName uniqueUserName, final String mac) {
    cachedMacAddresses.put(uniqueUserName, mac);
  }

  private void notifyPlayerRemoval(final INode node) {
    cachedMacAddresses.remove(node.getPlayerName());
  }

  @Override
  public void messageReceived(final MessageHeader msg, final SocketChannel channel) {
    final INode expectedReceive = channelToNode.get(channel);
    if (!expectedReceive.equals(msg.getFrom())) {
      throw new IllegalStateException("Expected: " + expectedReceive + " not: " + msg.getFrom());
    }

    if (msg.isBroadcast()) {
      forwardBroadcast(msg);
      notifyListeners(msg);
    } else if (msg.isAddressedTo(node)) {
      notifyListeners(msg);
    } else {
      forward(msg);
    }
  }

  @Override
  public boolean isPlayerBanned(final String ip, final String mac) {
    if (IpAddressParser.fromString(ip).isAnyLocalAddress()) {
      return false;
    }

    return miniBannedIpAddresses.contains(ip)
        || miniBannedMacAddresses.contains(mac)
        || (gameToLobbyConnection != null && gameToLobbyConnection.isPlayerBanned(ip));
  }

  @Override
  public void banPlayer(final String ip, final String mac) {
    miniBannedIpAddresses.add(ip);
    miniBannedMacAddresses.add(mac);
  }

  private void forward(final MessageHeader msg) {
    if (shutdown) {
      return;
    }
    final SocketChannel socketChannel = nodeToChannel.get(msg.getTo());
    if (socketChannel == null) {
      throw new IllegalStateException("No channel for:" + msg.getTo());
    }
    nioSocket.send(socketChannel, msg);
  }

  private void forwardBroadcast(final MessageHeader msg) {
    if (shutdown) {
      return;
    }
    final SocketChannel fromChannel = nodeToChannel.get(msg.getFrom());
    final List<SocketChannel> nodes = new ArrayList<>(nodeToChannel.values());
    for (final SocketChannel channel : nodes) {
      if (channel != fromChannel) {
        nioSocket.send(channel, msg);
      }
    }
  }

  private void notifyListeners(final MessageHeader msg) {
    for (final IMessageListener listener : listeners) {
      listener.messageReceived(msg.getMessage(), msg.getFrom());
    }
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
  public INode getLocalNode() {
    return node;
  }

  private class ConnectionHandler implements Runnable {
    @Override
    public void run() {
      try {
        socketChannel.register(acceptorSelector, SelectionKey.OP_ACCEPT);
      } catch (final ClosedChannelException e) {
        log.error("socket closed", e);
        shutDown();
      }
      while (!shutdown) {
        try {
          acceptorSelector.select();
        } catch (final IOException e) {
          log.error("Could not accept on server", e);
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
              log.error("Could not accept channel", e);
              try {
                if (socketChannel != null) {
                  socketChannel.close();
                }
              } catch (final IOException e2) {
                log.error("Could not close channel", e2);
              }
              continue;
            }
            // we are not accepting connections
            if (!acceptNewConnection) {
              try {
                socketChannel.close();
              } catch (final IOException e) {
                log.error("Could not close channel", e);
              }
              continue;
            }
            final ServerQuarantineConversation conversation =
                new ServerQuarantineConversation(
                    loginValidator, socketChannel, nioSocket, ServerMessenger.this);
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

  /** Bans & disconnects a player. */
  public void banPlayer(String playerName) {
    getNodes().stream()
        .filter(n -> n.getName().equals(playerName))
        .findAny()
        .ifPresent(
            nodeToBan -> {
              miniBannedIpAddresses.add(nodeToBan.getIpAddress());
              miniBannedMacAddresses.add(getPlayerMac(node.getPlayerName()));
              removeConnection(nodeToBan);
            });
  }

  public boolean isModerator(INode node) {
    return getModerators().stream()
        .anyMatch(
            moderator -> moderator.equals(node) && moderator.getName().equals(node.getName()));
  }

  /**
   * Moderator is the first person to join, which is the host player. In 'headless' case, it is the
   * host player plus the next 'oldest' player.
   */
  private List<INode> getModerators() {
    Preconditions.checkState(!nodeJoinOrder.isEmpty());
    if (nodeJoinOrder.size() == 1) {
      return List.of(nodeJoinOrder.get(0));
    } else {
      return List.of(nodeJoinOrder.get(0), nodeJoinOrder.get(1));
    }
  }

  /** Disconnects a player by player name. */
  public void removeConnection(String playerName) {
    getNodes().stream()
        .filter(n -> n.getName().equals(playerName))
        .findAny()
        .ifPresent(this::removeConnection);
  }

  @Override
  public void removeConnection(final INode nodeToRemove) {
    if (nodeToRemove.equals(this.node)) {
      throw new IllegalArgumentException("Can't remove yourself!");
    }
    notifyPlayerRemoval(nodeToRemove);
    final SocketChannel channel = nodeToChannel.remove(nodeToRemove);
    if (channel == null) {
      log.warn("Could not find node to remove: " + nodeToRemove);
      return;
    }
    channelToNode.remove(channel);
    nioSocket.close(channel);

    boolean removingModerator = getModerators().contains(nodeToRemove);
    nodeJoinOrder.remove(nodeToRemove);
    if (removingModerator) {
      INode newModerator = getModerators().get(getModerators().size() - 1);

      nodeToChannel
          .keySet()
          .forEach(node -> send(new ModeratorPromoted(newModerator.getName()), node));
    }

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
  public void socketUnquarantined(
      final SocketChannel channel, final QuarantineConversation conversation) {
    final ServerQuarantineConversation con = (ServerQuarantineConversation) conversation;
    final INode remote =
        new Node(
            con.getRemoteName(), (InetSocketAddress) channel.socket().getRemoteSocketAddress());
    synchronized (newNodeLock) {
      nodeToChannel.put(remote, channel);
    }
    channelToNode.put(channel, remote);
    // only keep track of join order if the game is headless.
    // If game is not headless, then it will end when the host leaves.
    if (GameRunner.headless()) {
      nodeJoinOrder.add(remote);
    }
    notifyConnectionsChanged(true, remote);

    if (getModerators().contains(remote)) {
      nodeToChannel.keySet().forEach(node -> send(new ModeratorPromoted(remote.getName()), node));
    }

    log.info("Connection added to:" + remote);
  }
}
