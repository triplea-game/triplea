package games.strategy.engine.message.unifiedmessenger;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteMethodCallResults;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.ClientMessenger;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.TestServerMessenger;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.SystemId;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Proves the typed-dispatch path preserves the correlation-engine threading contract: a
 * request/response reply is counted down on a different thread from the blocked caller (no
 * self-deadlock), the handler runs off the caller thread, and a single-threaded channel applies
 * typed messages in send order even when a handler is slow.
 */
class TypedMessageDispatchTest {
  private static final RemoteName ECHO = new RemoteName("echo", EchoRemote.class);
  private static final RemoteName ORDERED = new RemoteName("ordered", OrderedChannel.class);

  @Test
  void typedRequestResponseRunsOffCallerThreadAndDoesNotDeadlock() throws Exception {
    IServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new TestServerMessenger();
      server.setAcceptNewConnections(true);
      final int serverPort = server.getLocalNode().getSocketAddress().getPort();
      client = new ClientMessenger("localhost", serverPort, "client", SystemId.of("system-id"));

      final UnifiedMessenger serverUnified = new UnifiedMessenger(server);
      final RemoteMessenger serverRemote = new RemoteMessenger(serverUnified);
      final AtomicReference<String> handlerThread = new AtomicReference<>();
      serverRemote.registerRemote((EchoRemote) () -> {}, ECHO);
      serverUnified
          .getTypedMessageRegistry()
          .register(
              EchoRequest.TYPE,
              (request, implementor) -> {
                handlerThread.set(Thread.currentThread().getName());
                return new EchoResponse(request.value + "-ack");
              });

      final UnifiedMessenger clientUnified = new UnifiedMessenger(client);
      final AtomicReference<String> callerThread = new AtomicReference<>();

      final EchoResponse response =
          assertTimeoutPreemptively(
              Duration.ofSeconds(10),
              () -> {
                callerThread.set(Thread.currentThread().getName());
                final RemoteMethodCallResults results =
                    clientUnified.invokeAndWait(
                        ECHO.getName(),
                        RemoteMethodCall.typed(ECHO.getName(), new EchoRequest("hello")));
                return (EchoResponse) results.getRVal();
              });

      assertThat(response.value, is("hello-ack"));
      assertThat(
          "handler must run off the caller thread that blocks on the latch",
          handlerThread.get(),
          is(not(callerThread.get())));
    } finally {
      shutdown(server, client);
    }
  }

  @Test
  void typedChannelBroadcastPreservesSendOrderUnderNumberGate() throws Exception {
    final int messageCount = 8;
    IServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new TestServerMessenger();
      server.setAcceptNewConnections(true);
      final int serverPort = server.getLocalNode().getSocketAddress().getPort();
      client = new ClientMessenger("localhost", serverPort, "client", SystemId.of("system-id"));

      final UnifiedMessenger serverUnified = new UnifiedMessenger(server);
      final UnifiedMessenger clientUnified = new UnifiedMessenger(client);
      final ChannelMessenger clientChannel = new ChannelMessenger(clientUnified);

      final List<Integer> applied = new CopyOnWriteArrayList<>();
      final AtomicReference<String> handlerThread = new AtomicReference<>();
      clientChannel.registerChannelSubscriber((OrderedChannel) () -> {}, ORDERED);
      clientUnified
          .getTypedMessageRegistry()
          .register(
              OrderMessage.TYPE,
              (message, implementor) -> {
                handlerThread.set(Thread.currentThread().getName());
                // A slow first handler would let the thread pool reorder later messages if the
                // single-threaded number gate were not enforcing send order.
                if (message.sequence == 0) {
                  sleepQuietly();
                }
                applied.add(message.sequence);
                return null;
              });

      await().until(() -> serverUnified.getHub().hasImplementors(ORDERED.getName()), is(true));

      IntStream.range(0, messageCount)
          .forEach(
              sequence ->
                  serverUnified.invoke(
                      ORDERED.getName(),
                      RemoteMethodCall.typed(ORDERED.getName(), new OrderMessage(sequence))));

      await().until(applied::size, is(messageCount));

      assertThat(
          applied, contains(IntStream.range(0, messageCount).boxed().toArray(Integer[]::new)));
      assertThat(handlerThread.get(), is(not(Thread.currentThread().getName())));
    } finally {
      shutdown(server, client);
    }
  }

  @Test
  void guardedRegistrationIsIdempotentAcrossGamesAndStillDispatches() throws Exception {
    IServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new TestServerMessenger();
      server.setAcceptNewConnections(true);
      final int serverPort = server.getLocalNode().getSocketAddress().getPort();
      client = new ClientMessenger("localhost", serverPort, "client", SystemId.of("system-id"));

      final Messengers serverMessengers = new Messengers(server);
      final Messengers clientMessengers = new Messengers(client);
      serverMessengers.registerRemote((EchoRemote) () -> {}, ECHO);

      // Two sequential games on one session share this registry, so the guarded call-site path must
      // register the first time and no-op the second, never tripping the duplicate check.
      registerEchoHandlerGuarded(serverMessengers);
      assertDoesNotThrow(() -> registerEchoHandlerGuarded(serverMessengers));

      final EchoResponse response =
          assertTimeoutPreemptively(
              Duration.ofSeconds(10),
              () ->
                  clientMessengers.invokeRemoteMessage(
                      ECHO, new EchoRequest("hello"), EchoResponse.TYPE));
      assertThat(response.value, is("hello-ack"));

      // Backstop intact: an unguarded double registration of the same type still throws, so a real
      // fan-out collision surfaces during development.
      serverMessengers.registerMessageHandler(OrderMessage.TYPE, (message, implementor) -> null);
      assertThrows(
          IllegalStateException.class,
          () ->
              serverMessengers.registerMessageHandler(
                  OrderMessage.TYPE, (message, implementor) -> null));
    } finally {
      shutdown(server, client);
    }
  }

  private static void registerEchoHandlerGuarded(final Messengers messengers) {
    if (!messengers.hasTypedMessageHandler(EchoRequest.TYPE)) {
      messengers.registerMessageHandler(
          EchoRequest.TYPE, (request, implementor) -> new EchoResponse(request.value + "-ack"));
    }
  }

  private static void sleepQuietly() {
    try {
      Thread.sleep(200);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void shutdown(final IServerMessenger server, final ClientMessenger client) {
    if (server != null) {
      server.shutDown();
    }
    if (client != null) {
      client.shutDown();
    }
  }

  private interface EchoRemote extends IRemote {
    void ignored();
  }

  private interface OrderedChannel extends IChannelSubscriber {
    void ignored();
  }

  private static final class EchoRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    static final MessageType<EchoRequest> TYPE = MessageType.of(EchoRequest.class);
    private final String value;

    private EchoRequest(final String value) {
      this.value = value;
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  private static final class EchoResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    static final MessageType<EchoResponse> TYPE = MessageType.of(EchoResponse.class);
    private final String value;

    private EchoResponse(final String value) {
      this.value = value;
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  private static final class OrderMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    static final MessageType<OrderMessage> TYPE = MessageType.of(OrderMessage.class);
    private final int sequence;

    private OrderMessage(final int sequence) {
      this.sequence = sequence;
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }
}
