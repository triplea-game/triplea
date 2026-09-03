package games.strategy.net.websocket.transport;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.DefaultObjectStreamFactory;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.game.server.GameRelayServer;

/**
 * Proves the REAL production RPC stack (L1 {@code UnifiedMessenger} correlation + L2 RMI proxies /
 * reflective dispatch, both unmodified) runs end-to-end over the websocket L0 transport against a
 * real {@link GameRelayServer} on localhost.
 *
 * <p>Each messenger ({@link WebsocketServerMessenger} host + two {@link WebsocketClientMessenger}
 * clients) is wrapped in {@code new Messengers(...)} exactly as production does at the {@code
 * ClientModel}/{@code ServerModel} construction seam. Nothing above the {@code IMessenger}
 * interface is touched, so if this passes the transport is a drop-in for the socket transport.
 *
 * <p>The key proof is the SYNCHRONOUS request/response path: {@link IRemoteReturning#increment} et
 * al. are ordinary blocking calls on the test thread. Under the hood the caller blocks in {@code
 * UnifiedMessenger.invokeAndWaitRemote} on a latch that is released when the reply arrives on the
 * transport's reader thread — a different thread — so a correct threading design returns a value
 * while a wrong one deadlocks.
 *
 * <p>The trio is shared across tests (see {@link WebsocketMessengerIntegrationTest} for why: OkHttp
 * pools cannot be released through the public API, so a fresh trio per test would leak them).
 */
class WebsocketRpcIntegrationTest {
  private static final int port = 6000 + ((int) (Math.random() * 1000));
  private static final URI RELAY_URI = URI.create("ws://localhost:" + port);
  private static final GameRelayServer relay = new GameRelayServer(port);
  private static final String GAME_ID = "rpc-game";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private static WebsocketServerMessenger serverMessenger;
  private static WebsocketClientMessenger client1Messenger;
  private static WebsocketClientMessenger client2Messenger;

  private static Messengers server;
  private static Messengers client1;
  private static Messengers client2;

  @BeforeAll
  static void startRelayAndTrio() throws Exception {
    relay.start();
    // The host must join first so the relay designates it as host / hub owner.
    serverMessenger =
        new WebsocketServerMessenger(RELAY_URI, GAME_ID, "host", new DefaultObjectStreamFactory());
    client1Messenger =
        new WebsocketClientMessenger(
            RELAY_URI, GAME_ID, "client1", new DefaultObjectStreamFactory());
    client2Messenger =
        new WebsocketClientMessenger(
            RELAY_URI, GAME_ID, "client2", new DefaultObjectStreamFactory());

    // Build the full production RPC stack (UnifiedMessenger + Channel + Remote) over each
    // transport.
    server = new Messengers(serverMessenger);
    client1 = new Messengers(client1Messenger);
    client2 = new Messengers(client2Messenger);

    await().atMost(TIMEOUT).until(() -> serverMessenger.currentNodes().size() == 3);
    await().atMost(TIMEOUT).until(() -> client1Messenger.currentNodes().size() == 3);
    await().atMost(TIMEOUT).until(() -> client2Messenger.currentNodes().size() == 3);
  }

  @AfterAll
  static void tearDown() {
    shutDownQuietly(client1);
    shutDownQuietly(client2);
    shutDownQuietly(server);
    relay.stop();
  }

  private static void shutDownQuietly(final Messengers messengers) {
    if (messengers != null) {
      try {
        messengers.shutDown();
      } catch (final RuntimeException ignored) {
        // best-effort teardown
      }
    }
  }

  @Test
  @DisplayName("A remote invoked from a client returns its value synchronously over the relay")
  void remoteReturnValueRoundTripsSynchronously() {
    final RemoteName name = new RemoteName("return-remote", IRemoteReturning.class);
    final RemoteReturning implementor = new RemoteReturning();
    server.registerRemote(implementor, name);

    final IRemoteReturning proxy = (IRemoteReturning) client1.getRemote(name);

    // The hub lives on the host; a host-registered remote is known to it synchronously, so the
    // client invoke below reaches an implementor immediately. This is a blocking invokeAndWait over
    // the relay: the value must come back on the SAME call, on the caller's thread.
    assertEquals(2, proxy.increment(1));
    assertEquals(101, proxy.increment(100));
    // Arguments and non-int return types cross the relay by value.
    assertEquals("hello world", proxy.concat("hello", "world"));
    // The invocation is attributed to the calling client's node.
    assertThat(implementor.getLastSender(), is(client1Messenger.getLocalNode()));

    server.unregisterRemote(name);
  }

  @Test
  @DisplayName("A checked exception thrown by the remote propagates back to the blocked caller")
  void remoteExceptionPropagatesToCaller() {
    final RemoteName name = new RemoteName("throwing-remote", IRemoteReturning.class);
    server.registerRemote(new RemoteReturning(), name);

    final IRemoteReturning proxy = (IRemoteReturning) client1.getRemote(name);

    final Exception e = assertThrows(Exception.class, proxy::throwException);
    assertEquals(RemoteReturning.EXCEPTION_MESSAGE, e.getCause().getMessage());

    server.unregisterRemote(name);
  }

  @Test
  @DisplayName("A remote hosted on a client is invokable from the server (reverse direction)")
  void remoteHostedOnClientInvokableFromServer() {
    final RemoteName name = new RemoteName("client-hosted-remote", IRemoteReturning.class);
    final RemoteReturning implementor = new RemoteReturning();
    client1.registerRemote(implementor, name);

    final IRemoteReturning proxy = (IRemoteReturning) server.getRemote(name);

    // A client-registered remote must first propagate to the hub over the relay; until it lands the
    // invoke throws RemoteNotFoundException. Retry until it resolves, then it returns
    // synchronously.
    await().ignoreExceptions().atMost(TIMEOUT).until(() -> proxy.increment(42) == 43);
    assertThat(implementor.getLastSender(), is(serverMessenger.getLocalNode()));

    client1.unregisterRemote(name);
  }

  @Test
  @DisplayName("A channel broadcast from the host reaches subscribers on every client")
  void channelBroadcastFromHostReachesAllClients() {
    final RemoteName name = new RemoteName("host-broadcast-channel", ITestChannel.class);
    final CountingChannel sub1 = new CountingChannel();
    final CountingChannel sub2 = new CountingChannel();
    client1.registerChannelSubscriber(sub1, name);
    client2.registerChannelSubscriber(sub2, name);

    final ITestChannel broadcaster = (ITestChannel) server.getChannelBroadcaster(name);
    // Subscriber registration propagates to the hub over the relay; re-broadcast until it has
    // landed
    // on both clients (a broadcast sent before registration arrives is simply not delivered).
    await()
        .atMost(TIMEOUT)
        .until(
            () -> {
              broadcaster.notifyEvent("go");
              return sub1.count() >= 1 && sub2.count() >= 1;
            });
    assertThat(sub1.lastValue(), is("go"));
    assertThat(sub2.lastValue(), is("go"));

    client1.unregisterChannelSubscriber(sub1, name);
    client2.unregisterChannelSubscriber(sub2, name);
  }

  @Test
  @DisplayName("A channel broadcast from a client reaches a subscriber on the host (reverse)")
  void channelBroadcastFromClientReachesHost() {
    final RemoteName name = new RemoteName("client-broadcast-channel", ITestChannel.class);
    final CountingChannel hostSub = new CountingChannel();
    server.registerChannelSubscriber(hostSub, name);

    final ITestChannel broadcaster = (ITestChannel) client1.getChannelBroadcaster(name);
    // A host-registered subscriber is known to the hub synchronously, but the client's broadcaster
    // routes through the hub over the relay; a single broadcast suffices, awaited for delivery.
    broadcaster.notifyEvent("back");

    await().atMost(TIMEOUT).until(() -> hostSub.count() >= 1);
    assertThat(hostSub.lastValue(), is("back"));

    server.unregisterChannelSubscriber(hostSub, name);
  }

  public interface IRemoteReturning extends IRemote {
    @RemoteActionCode(0)
    int increment(int value);

    @RemoteActionCode(1)
    String concat(String a, String b);

    @RemoteActionCode(2)
    void throwException() throws Exception;
  }

  private static final class RemoteReturning implements IRemoteReturning {
    static final String EXCEPTION_MESSAGE = "boom over the relay";
    private volatile INode lastSender;

    @Override
    public int increment(final int value) {
      lastSender = MessageContext.getSender();
      return value + 1;
    }

    @Override
    public String concat(final String a, final String b) {
      lastSender = MessageContext.getSender();
      return a + " " + b;
    }

    @Override
    public void throwException() throws Exception {
      throw new Exception(EXCEPTION_MESSAGE);
    }

    INode getLastSender() {
      return lastSender;
    }
  }

  public interface ITestChannel extends IChannelSubscriber {
    @RemoteActionCode(0)
    void notifyEvent(String value);
  }

  private static final class CountingChannel implements ITestChannel {
    private final AtomicInteger count = new AtomicInteger();
    private volatile String lastValue;

    @Override
    public void notifyEvent(final String value) {
      lastValue = value;
      count.incrementAndGet();
    }

    int count() {
      return count.get();
    }

    String lastValue() {
      return lastValue;
    }
  }
}
