package games.strategy.net.websocket.transport;

import com.google.gson.Gson;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessageListener;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.triplea.game.server.relay.GameRelayEnvelope;
import org.triplea.game.server.relay.RelayControl.JoinPayload;
import org.triplea.game.server.relay.RelayControl.MemberInfo;
import org.triplea.game.server.relay.RelayControl.MemberJoinedPayload;
import org.triplea.game.server.relay.RelayControl.MemberLeftPayload;
import org.triplea.game.server.relay.RelayControl.WelcomePayload;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.WebSocket;

/**
 * Common relay-client behaviour shared by the websocket {@link games.strategy.net.IClientMessenger}
 * and {@link games.strategy.net.IServerMessenger}. Both dial OUT to a {@code GameRelayServer} with
 * a {@link GenericWebSocketClient} and speak the Phase-A relay protocol; the only differences are
 * {@link #isServer()} and the server-only admin API.
 *
 * <p>Layering: this is the L0 transport under the unchanged L1 {@code UnifiedMessenger}. It carries
 * {@link MessageHeader}s exactly like the socket {@code ClientMessenger}/{@code ServerMessenger}.
 *
 * <h2>Payload serialization</h2>
 *
 * Game traffic is serialized with the supplied {@link IObjectStreamFactory} (in production {@code
 * GameObjectStreamFactory}), NOT plain Java serialization, so {@code Unit} UUID reconciliation and
 * name-marker collapsing behave identically to the socket path. The serialized {@link
 * MessageHeader} is base64-encoded into the opaque {@link GameRelayEnvelope#payload()}; the relay
 * never decodes it.
 *
 * <h2>Threading</h2>
 *
 * Inbound frames arrive on OkHttp's single reader thread ({@code
 * GenericWebSocketClient.messageReceived} is {@code @Synchronized}), which preserves per-connection
 * in-order delivery. {@link IMessageListener#messageReceived} is dispatched on that thread and it
 * is never blocked here. The handshake blocks the CONSTRUCTOR thread on a latch that WELCOME
 * (arriving on the reader thread) counts down, so there is no deadlock.
 */
@Slf4j
abstract class RelayMessenger implements IMessenger {
  private static final Gson GSON = new Gson();
  private static final long HANDSHAKE_TIMEOUT_SECONDS = 20;

  private final GenericWebSocketClient client;
  private final String gameId;
  private final IObjectStreamFactory objectStreamFactory;

  private final CountDownLatch initLatch = new CountDownLatch(1);
  private final AtomicReference<Exception> handshakeError = new AtomicReference<>();

  private final CopyOnWriteArrayList<IMessageListener> messageListeners =
      new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<IConnectionChangeListener> connectionChangeListeners =
      new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<IMessengerErrorListener> errorListeners =
      new CopyOnWriteArrayList<>();

  // Registry keyed on the relay-assigned nodeId, populated from WELCOME + MEMBER_JOINED/LEFT.
  private final Map<String, INode> nodeIdToNode = new ConcurrentHashMap<>();
  private final Map<INode, String> nodeToNodeId = new ConcurrentHashMap<>();

  private volatile String localNodeId;
  private volatile INode localNode;
  private volatile INode serverNode;
  private volatile boolean shutDown = false;

  RelayMessenger(
      final URI relayUri,
      final String gameId,
      final String playerName,
      final IObjectStreamFactory objectStreamFactory)
      throws IOException {
    this.gameId = gameId;
    this.objectStreamFactory = objectStreamFactory;
    client =
        GenericWebSocketClient.builder()
            .websocketUri(relayUri)
            .errorHandler(this::onConnectionError)
            .headers(Map.of())
            .build();
    client.addListener(GameRelayEnvelope.TYPE, this::onEnvelope);
    // A client-initiated close is a clean shutdown; a ban is surfaced via connectionTerminated.
    client.addConnectionClosedListener(() -> shutDown = true);
    client.addConnectionTerminatedListener(
        reason -> fireError(new IOException("Connection terminated by relay: " + reason)));
    // An unexpected drop of an established connection would otherwise be silently auto-reconnected
    // by the underlying client (firing onReconnecting on each attempt). For round 1 we fail fast
    // instead: the first reconnect attempt is our signal that the relay died, so surface it as an
    // error (see onUnexpectedDisconnect) rather than reconnect silently.
    client.addReconnectionListener(
        new WebSocket.ReconnectionHandler() {
          @Override
          public void onReconnecting(final int currentAttempt) {
            onUnexpectedDisconnect();
          }

          @Override
          public void onReconnected() {}
        });
    client.connect();

    // Queued until the socket opens, then flushed in order (see WebSocketConnection).
    client.sendMessage(
        new GameRelayEnvelope(
            gameId,
            null,
            null,
            null,
            GameRelayEnvelope.TYPE_JOIN,
            GSON.toJson(new JoinPayload(playerName))));

    awaitHandshake();
  }

  private void awaitHandshake() throws IOException {
    try {
      if (!initLatch.await(HANDSHAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        shutDown();
        throw new IOException("Timed out waiting for relay WELCOME");
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      shutDown();
      throw new IOException("Interrupted waiting for relay WELCOME", e);
    }
    final Exception error = handshakeError.get();
    if (error != null) {
      shutDown();
      throw new IOException("Failed to connect to relay: " + error.getMessage(), error);
    }
  }

  private void onConnectionError(final String message) {
    // Before the handshake completes, a connect error must unblock (and fail) the constructor.
    // After it completes, surface it to error listeners so L1 can fail pending latches.
    if (initLatch.getCount() > 0) {
      handshakeError.compareAndSet(null, new IOException(message));
      initLatch.countDown();
    } else {
      fireError(new IOException(message));
    }
  }

  /**
   * Fires when an established relay connection drops unexpectedly (the underlying client would
   * otherwise silently auto-reconnect). Round 1 fails fast: surface it to error listeners so L1's
   * pending {@code invokeAndWait} latches fail rather than hang forever, mirroring the socket
   * {@code ClientMessenger.socketError} contract, then shut down (which also stops the background
   * reconnect). Runs on the reconnect thread; must not block on relay traffic.
   */
  private void onUnexpectedDisconnect() {
    if (shutDown) {
      return;
    }
    if (initLatch.getCount() > 0) {
      // Dropped mid-handshake: unblock and fail the blocked constructor, matching
      // onConnectionError.
      handshakeError.compareAndSet(null, new IOException("Relay connection lost during handshake"));
      initLatch.countDown();
      return;
    }
    fireError(new IOException("Relay connection lost"));
    shutDown();
  }

  /** Runs on the OkHttp reader thread. Must not block. */
  private void onEnvelope(final GameRelayEnvelope envelope) {
    try {
      switch (envelope.type()) {
        case GameRelayEnvelope.TYPE_WELCOME -> onWelcome(envelope);
        case GameRelayEnvelope.TYPE_MEMBER_JOINED -> onMemberJoined(envelope);
        case GameRelayEnvelope.TYPE_MEMBER_LEFT -> onMemberLeft(envelope);
        case GameRelayEnvelope.TYPE_GAME -> onGame(envelope);
        default -> log.warn("Ignoring unexpected relay envelope type: {}", envelope.type());
      }
    } catch (final RuntimeException e) {
      log.error("Error handling relay envelope of type {}", envelope.type(), e);
    }
  }

  private void onWelcome(final GameRelayEnvelope envelope) {
    final WelcomePayload welcome = GSON.fromJson(envelope.payload(), WelcomePayload.class);
    localNodeId = welcome.nodeId();
    for (final MemberInfo member : welcome.members()) {
      register(member.nodeId(), member.name());
    }
    localNode = nodeIdToNode.get(localNodeId);
    // The relay reports members in join order (LinkedHashMap); the first is the host = server node.
    final String serverNodeId =
        welcome.members().isEmpty() ? localNodeId : welcome.members().get(0).nodeId();
    serverNode = nodeIdToNode.get(serverNodeId);
    initLatch.countDown();
  }

  private void onMemberJoined(final GameRelayEnvelope envelope) {
    final MemberJoinedPayload joined = GSON.fromJson(envelope.payload(), MemberJoinedPayload.class);
    final INode node = register(joined.nodeId(), joined.name());
    fireConnectionChanged(true, node);
  }

  private void onMemberLeft(final GameRelayEnvelope envelope) {
    final MemberLeftPayload left = GSON.fromJson(envelope.payload(), MemberLeftPayload.class);
    final INode node = nodeIdToNode.remove(left.nodeId());
    if (node != null) {
      nodeToNodeId.remove(node);
      fireConnectionChanged(false, node);
    }
  }

  private void onGame(final GameRelayEnvelope envelope) {
    final MessageHeader header = deserialize(envelope.payload());
    if (header == null) {
      return;
    }
    for (final IMessageListener listener : messageListeners) {
      listener.messageReceived(header.getMessage(), header.getFrom());
    }
  }

  private INode register(final String nodeId, final String name) {
    final INode node = RelayNodeIdentity.toNode(nodeId, name);
    nodeIdToNode.put(nodeId, node);
    nodeToNodeId.put(node, nodeId);
    return node;
  }

  @Override
  public void send(final Serializable msg, @Nullable final INode to) {
    if (shutDown) {
      return;
    }
    final String toNodeId = to == null ? null : nodeToNodeId.get(to);
    // Fail silently if the target is unknown, matching the socket messengers' contract.
    if (to != null && toNodeId == null) {
      return;
    }
    final MessageHeader header = new MessageHeader(to, localNode, msg);
    final String payload = serialize(header);
    if (payload == null) {
      return;
    }
    client.sendMessage(
        new GameRelayEnvelope(
            gameId, toNodeId, localNodeId, null, GameRelayEnvelope.TYPE_GAME, payload));
  }

  private @Nullable String serialize(final MessageHeader header) {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = objectStreamFactory.create(bytes)) {
      out.writeObject(header);
    } catch (final IOException e) {
      log.error("Failed to serialize outbound message", e);
      return null;
    }
    return Base64.getEncoder().encodeToString(bytes.toByteArray());
  }

  private @Nullable MessageHeader deserialize(final @Nullable String payload) {
    if (payload == null) {
      return null;
    }
    final byte[] bytes = Base64.getDecoder().decode(payload);
    try (ObjectInputStream in = objectStreamFactory.create(new ByteArrayInputStream(bytes))) {
      return (MessageHeader) in.readObject();
    } catch (final IOException | ClassNotFoundException e) {
      log.error("Failed to deserialize inbound message", e);
      return null;
    }
  }

  /** Host-only relay boot; a no-op / rejected by the relay for a non-host. */
  void sendBoot(final INode target, final boolean ban) {
    final String targetNodeId = nodeToNodeId.get(target);
    if (targetNodeId == null) {
      return;
    }
    client.sendMessage(
        new GameRelayEnvelope(
            gameId,
            null,
            localNodeId,
            null,
            GameRelayEnvelope.TYPE_BOOT,
            GSON.toJson(
                new org.triplea.game.server.relay.RelayControl.BootPayload(targetNodeId, ban))));
  }

  Set<INode> currentNodes() {
    return Set.copyOf(nodeIdToNode.values());
  }

  @Nullable
  String nodeIdOf(final INode node) {
    return nodeToNodeId.get(node);
  }

  @Override
  public void addMessageListener(final IMessageListener listener) {
    messageListeners.add(listener);
  }

  @Override
  public void addConnectionChangeListener(final IConnectionChangeListener listener) {
    connectionChangeListeners.add(listener);
  }

  @Override
  public void removeConnectionChangeListener(final IConnectionChangeListener listener) {
    connectionChangeListeners.remove(listener);
  }

  void addErrorListener(final IMessengerErrorListener listener) {
    errorListeners.add(listener);
  }

  void removeErrorListener(final IMessengerErrorListener listener) {
    errorListeners.remove(listener);
  }

  private void fireConnectionChanged(final boolean added, final INode node) {
    for (final IConnectionChangeListener listener : connectionChangeListeners) {
      if (added) {
        listener.connectionAdded(node);
      } else {
        listener.connectionRemoved(node);
      }
    }
  }

  private void fireError(final Throwable cause) {
    if (shutDown) {
      return;
    }
    for (final IMessengerErrorListener listener : errorListeners) {
      listener.messengerInvalid(cause);
    }
  }

  @Override
  public INode getLocalNode() {
    return localNode;
  }

  @Override
  public INode getServerNode() {
    return serverNode;
  }

  @Override
  public boolean isConnected() {
    return !shutDown && client.isOpen();
  }

  @Override
  public void shutDown() {
    // Set first so the reconnection handler treats this as INTENTIONAL and never fires an error.
    shutDown = true;
    // shutdown() (not close()) also releases the OkHttp thread/connection pools, so repeated
    // join/leave cycles do not leak resources.
    client.shutdown();
  }
}
