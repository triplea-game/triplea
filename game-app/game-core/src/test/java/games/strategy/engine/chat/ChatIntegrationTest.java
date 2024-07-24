package games.strategy.engine.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.net.TestServerMessenger;
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;
import org.triplea.java.ThreadRunner;

final class ChatIntegrationTest extends AbstractClientSettingTestCase {
  private static final String CHAT_NAME = TestServerMessenger.CHAT_CHANNEL_NAME;
  private static final int MESSAGE_COUNT = 50;
  private static final int NODE_COUNT = 3;

  private ServerMessenger messenger;
  private IMessenger client1Messenger;
  private IMessenger client2Messenger;
  private RemoteMessenger remoteMessenger;
  private ChannelMessenger channelMessenger;
  private RemoteMessenger client1RemoteMessenger;
  private ChannelMessenger client1ChannelMessenger;
  private RemoteMessenger client2RemoteMessenger;
  private ChannelMessenger client2ChannelMessenger;

  private final TestChatMessageListener serverChatMessageListener = new TestChatMessageListener();
  private final TestChatPlayerListener serverChatPlayerListener = new TestChatPlayerListener();

  private final TestChatMessageListener client1ChatMessageListener = new TestChatMessageListener();
  private final TestChatPlayerListener client1ChatPlayerListener = new TestChatPlayerListener();

  private final TestChatMessageListener client2ChatMessageListener = new TestChatMessageListener();
  private final TestChatPlayerListener client2ChatPlayerListener = new TestChatPlayerListener();

  @BeforeEach
  void setUp() throws Exception {
    messenger = new TestServerMessenger();
    messenger.setAcceptNewConnections(true);
    final int serverPort = messenger.getLocalNode().getSocketAddress().getPort();
    final SystemId systemId = SystemId.of("system-id");
    client1Messenger = new ClientMessenger("localhost", serverPort, "client1", systemId);
    client2Messenger = new ClientMessenger("localhost", serverPort, "client2", systemId);
    final UnifiedMessenger serverUnifiedMessenger = new UnifiedMessenger(messenger);
    remoteMessenger = new RemoteMessenger(serverUnifiedMessenger);
    channelMessenger = new ChannelMessenger(serverUnifiedMessenger);
    final UnifiedMessenger client1UnifiedMessenger = new UnifiedMessenger(client1Messenger);
    client1RemoteMessenger = new RemoteMessenger(client1UnifiedMessenger);
    client1ChannelMessenger = new ChannelMessenger(client1UnifiedMessenger);
    final UnifiedMessenger client2UnifiedMessenger = new UnifiedMessenger(client2Messenger);
    client2RemoteMessenger = new RemoteMessenger(client2UnifiedMessenger);
    client2ChannelMessenger = new ChannelMessenger(client2UnifiedMessenger);
  }

  @AfterEach
  void tearDown() {
    if (messenger != null) {
      messenger.shutDown();
    }
    if (client1Messenger != null) {
      client1Messenger.shutDown();
    }
    if (client2Messenger != null) {
      client2Messenger.shutDown();
    }
  }

  @Test
  void shouldBeAbleToChatAcrossMultipleNodes() {
    runChatTest(
        (server, client1, client2) -> {
          sendMessagesFrom(client2);
          sendMessagesFrom(server);
          sendMessagesFrom(client1);
          waitFor(this::allMessagesToArrive);
        });
  }

  private void runChatTest(final ChatTest chatTest) {
    assertTimeoutPreemptively(
        Duration.ofSeconds(15),
        () -> {
          final ChatController controller = newChatController();
          final Chat server = newChat(new Messengers(messenger, remoteMessenger, channelMessenger));
          server.addChatListener(serverChatMessageListener);
          server.addChatListener(serverChatPlayerListener);
          final Chat client1 =
              newChat(
                  new Messengers(
                      client1Messenger, client1RemoteMessenger, client1ChannelMessenger));

          client1.addChatListener(client1ChatMessageListener);
          client1.addChatListener(client1ChatPlayerListener);
          final Chat client2 =
              newChat(
                  new Messengers(
                      client2Messenger, client2RemoteMessenger, client2ChannelMessenger));
          client2.addChatListener(client2ChatMessageListener);
          client2.addChatListener(client2ChatPlayerListener);
          waitFor(this::allNodesToConnect);

          chatTest.run(server, client1, client2);

          client1.shutdown();
          client2.shutdown();
          waitFor(this::clientNodesToDisconnect);

          controller.deactivate();
          waitFor(this::serverNodeToDisconnect);
        });
  }

  private ChatController newChatController() {
    return new ChatController(
        CHAT_NAME, new Messengers(messenger, remoteMessenger, channelMessenger), messenger);
  }

  private static Chat newChat(final Messengers messengers) {
    return new Chat(
        new MessengersChatTransmitter(CHAT_NAME, messengers, ClientNetworkBridge.NO_OP_SENDER));
  }

  private static void waitFor(final Runnable assertion) throws InterruptedException {
    final long timeoutInMilliseconds = 10_000L;
    final long startTimeInMilliseconds = System.currentTimeMillis();
    while ((System.currentTimeMillis() - startTimeInMilliseconds) < timeoutInMilliseconds) {
      try {
        assertion.run();
        return;
      } catch (final AssertionError e) {
        Thread.sleep(25L);
      }
    }

    assertion.run();
  }

  private void allNodesToConnect() {
    assertThat(serverChatPlayerListener.playerCount.get(), is(NODE_COUNT));
    assertThat(client1ChatPlayerListener.playerCount.get(), is(NODE_COUNT));
    assertThat(client2ChatPlayerListener.playerCount.get(), is(NODE_COUNT));
  }

  private void allMessagesToArrive() {
    assertThat(serverChatMessageListener.messageCount.get(), is(NODE_COUNT * MESSAGE_COUNT));
    assertThat(client1ChatMessageListener.messageCount.get(), is(NODE_COUNT * MESSAGE_COUNT));
    assertThat(client2ChatMessageListener.messageCount.get(), is(NODE_COUNT * MESSAGE_COUNT));
  }

  private void clientNodesToDisconnect() {
    assertThat(serverChatPlayerListener.playerCount.get(), is(1));
  }

  private void serverNodeToDisconnect() {
    assertThat(serverChatPlayerListener.playerCount.get(), is(0));
  }

  private static void sendMessagesFrom(final Chat node) {
    ThreadRunner.runInNewThread(
        () -> IntStream.range(0, MESSAGE_COUNT).forEach(i -> node.sendMessage("Test")));
  }

  @FunctionalInterface
  private interface ChatTest {
    void run(Chat server, Chat client1, Chat client2) throws Exception;
  }

  private static final class TestChatPlayerListener implements ChatPlayerListener {
    final AtomicInteger playerCount = new AtomicInteger();

    @Override
    public void updatePlayerList(final Collection<ChatParticipant> players) {
      playerCount.set(players.size());
    }
  }

  private static final class TestChatMessageListener implements ChatMessageListener {
    final AtomicInteger messageCount = new AtomicInteger();
    final AtomicReference<String> lastMessageReceived = new AtomicReference<>();

    @Override
    public void slapped(final UserName from) {}

    @Override
    public void eventReceived(final String event) {}

    @Override
    public void messageReceived(final UserName fromPlayer, final String chatMessage) {
      lastMessageReceived.set(chatMessage);
      messageCount.incrementAndGet();
    }

    @Override
    public void playerJoined(final String message) {}

    @Override
    public void playerLeft(final String message) {}
  }
}
