package games.strategy.net.websocket.transport;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import games.strategy.net.DefaultObjectStreamFactory;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessageListener;
import games.strategy.net.INode;
import java.io.Serializable;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.game.server.GameRelayServer;

/**
 * Exercises the websocket transport pair ({@link WebsocketServerMessenger} + two {@link
 * WebsocketClientMessenger}s) over a real {@link GameRelayServer} on localhost, mirroring the
 * socket {@code MessengerIntegrationTest}.
 *
 * <p>One host+two-client trio is shared across the tests (built in {@code @BeforeAll}, listener
 * state reset per test). This is deliberate: each messenger owns a {@code GenericWebSocketClient}
 * backed by its own {@code OkHttpClient}, whose thread/connection pools cannot be released through
 * the public API, so building a fresh trio per test would leak them and eventually exhaust the test
 * JVM. Destructive cases (disconnect, ban) spin up a short-lived extra client so the shared trio
 * survives.
 */
class WebsocketMessengerIntegrationTest {
  // Random port to avoid clashing with a slow-to-release relay from a prior quick re-run.
  private static final int port = 6000 + ((int) (Math.random() * 1000));
  private static final URI RELAY_URI = URI.create("ws://localhost:" + port);
  private static final GameRelayServer relay = new GameRelayServer(port);
  private static final String GAME_ID = "integration-game";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private static WebsocketServerMessenger server;
  private static WebsocketClientMessenger client1;
  private static WebsocketClientMessenger client2;

  private static final CollectingListener serverListener = new CollectingListener();
  private static final CollectingListener client1Listener = new CollectingListener();
  private static final CollectingListener client2Listener = new CollectingListener();

  @BeforeAll
  static void startRelayAndTrio() throws Exception {
    relay.start();
    // The host must join first so the relay designates it as host.
    server =
        new WebsocketServerMessenger(RELAY_URI, GAME_ID, "host", new DefaultObjectStreamFactory());
    client1 =
        new WebsocketClientMessenger(
            RELAY_URI, GAME_ID, "client1", new DefaultObjectStreamFactory());
    client2 =
        new WebsocketClientMessenger(
            RELAY_URI, GAME_ID, "client2", new DefaultObjectStreamFactory());
    server.addMessageListener(serverListener);
    client1.addMessageListener(client1Listener);
    client2.addMessageListener(client2Listener);

    // All three agree the server node is the host.
    assertThat(client1.getServerNode(), is(server.getLocalNode()));
    assertThat(client2.getServerNode(), is(server.getLocalNode()));
    assertThat(server.getServerNode(), is(server.getLocalNode()));

    await().atMost(TIMEOUT).until(() -> server.currentNodes().size() == 3);
    await().atMost(TIMEOUT).until(() -> client1.currentNodes().size() == 3);
    await().atMost(TIMEOUT).until(() -> client2.currentNodes().size() == 3);
  }

  @AfterAll
  static void tearDown() {
    shutDownQuietly(client1);
    shutDownQuietly(client2);
    shutDownQuietly(server);
    relay.stop();
  }

  @BeforeEach
  void resetListeners() {
    serverListener.clear();
    client1Listener.clear();
    client2Listener.clear();
    // Every test starts from the steady 3-node trio.
    await().atMost(TIMEOUT).until(() -> server.currentNodes().size() == 3);
  }

  @Test
  @DisplayName("All three peers see a node count of 3")
  void allPeersSeeThreeNodes() {
    assertThat(server.getNodes(), hasSize(3));
    assertThat(client1.currentNodes(), hasSize(3));
    assertThat(client2.currentNodes(), hasSize(3));
  }

  @Test
  @DisplayName("Server to client delivers to only the addressed client")
  void serverToClient() {
    server.send("hello", client1.getLocalNode());

    client1Listener.await(1);
    assertThat(client1Listener.messages(), contains("hello"));
    assertThat(client1Listener.senders(), contains(server.getLocalNode()));
    assertThat(client2Listener.messages(), is(empty()));
  }

  @Test
  @DisplayName("Client to server delivers to only the server")
  void clientToServer() {
    client1.send("hello", server.getLocalNode());

    serverListener.await(1);
    assertThat(serverListener.messages(), contains("hello"));
    assertThat(serverListener.senders(), contains(client1.getLocalNode()));
    assertThat(client2Listener.messages(), is(empty()));
  }

  @Test
  @DisplayName("Client to client delivers to only the addressed client")
  void clientToClient() {
    client1.send("hello", client2.getLocalNode());

    client2Listener.await(1);
    assertThat(client2Listener.messages(), contains("hello"));
    assertThat(client2Listener.senders(), contains(client1.getLocalNode()));
    assertThat(serverListener.messages(), is(empty()));
  }

  @Test
  @DisplayName("A broadcast reaches every other peer but not the sender")
  void broadcastReachesOthersExcludingSender() {
    server.send("broadcast", null);

    client1Listener.await(1);
    client2Listener.await(1);
    assertThat(client1Listener.messages(), contains("broadcast"));
    assertThat(client2Listener.messages(), contains("broadcast"));
    assertThat(serverListener.messages(), is(empty()));
  }

  @Test
  @DisplayName("A Serializable payload round-trips by value, not by reference")
  void serializablePayloadRoundTripsByValue() {
    final Payload sent = new Payload("map", 42);
    server.send(sent, client1.getLocalNode());

    client1Listener.await(1);
    final Serializable received = client1Listener.messages().get(0);
    assertThat(received, is(sent));
    assertThat(received, is(not(sameInstance(sent))));
  }

  @Test
  @DisplayName("A client disconnect drops node count and fires connectionRemoved on others")
  void disconnectDropsNodeCountAndFiresConnectionChange() throws Exception {
    final AtomicInteger removed = new AtomicInteger();
    final IConnectionChangeListener listener =
        new IConnectionChangeListener() {
          @Override
          public void connectionAdded(final INode to) {}

          @Override
          public void connectionRemoved(final INode to) {
            removed.incrementAndGet();
          }
        };
    server.addConnectionChangeListener(listener);
    try {
      final WebsocketClientMessenger transient3 =
          new WebsocketClientMessenger(
              RELAY_URI, GAME_ID, "transient", new DefaultObjectStreamFactory());
      await().atMost(TIMEOUT).until(() -> server.currentNodes().size() == 4);

      transient3.shutDown();

      await().atMost(TIMEOUT).until(() -> server.currentNodes().size() == 3);
      await().atMost(TIMEOUT).until(() -> client1.currentNodes().size() == 3);
      await().atMost(TIMEOUT).until(() -> removed.get() >= 1);
    } finally {
      server.removeConnectionChangeListener(listener);
    }
  }

  @Test
  @DisplayName("A ban fires the victim's error listener and drops node count")
  void banFiresErrorListenerAndDropsNodeCount() throws Exception {
    final WebsocketClientMessenger victim =
        new WebsocketClientMessenger(
            RELAY_URI, GAME_ID, "victim", new DefaultObjectStreamFactory());
    final AtomicBoolean victimErrored = new AtomicBoolean(false);
    victim.addErrorListener(cause -> victimErrored.set(true));
    await().atMost(TIMEOUT).until(() -> server.currentNodes().size() == 4);

    server.banPlayer(victim.getLocalNode().getIpAddress(), "some-mac");

    await().atMost(TIMEOUT).untilTrue(victimErrored);
    await().atMost(TIMEOUT).until(() -> server.currentNodes().size() == 3);
  }

  private static void shutDownQuietly(final RelayMessenger messenger) {
    if (messenger != null) {
      try {
        messenger.shutDown();
      } catch (final RuntimeException ignored) {
        // best-effort teardown
      }
    }
  }

  /** A non-String Serializable to prove arbitrary payloads round-trip by value across the relay. */
  private record Payload(String name, int value) implements Serializable {}

  private static final class CollectingListener implements IMessageListener {
    private final List<Serializable> messages = new CopyOnWriteArrayList<>();
    private final List<INode> senders = new CopyOnWriteArrayList<>();

    @Override
    public void messageReceived(final Serializable msg, final INode from) {
      messages.add(msg);
      senders.add(from);
    }

    void clear() {
      messages.clear();
      senders.clear();
    }

    List<Serializable> messages() {
      return List.copyOf(messages);
    }

    List<INode> senders() {
      return List.copyOf(senders);
    }

    void await(final int count) {
      Awaitility.await().atMost(TIMEOUT).until(() -> messages.size() >= count);
    }
  }
}
