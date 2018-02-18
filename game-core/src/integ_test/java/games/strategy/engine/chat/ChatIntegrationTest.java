package games.strategy.engine.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.server.NullModeratorController;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.SoundPath;

public final class ChatIntegrationTest {
  private static final String CHAT_NAME = "c";
  private static final int MESSAGE_COUNT = 50;
  private static final int NODE_COUNT = 3;

  private IServerMessenger serverMessenger;
  private IMessenger client1Messenger;
  private IMessenger client2Messenger;
  private RemoteMessenger serverRemoteMessenger;
  private ChannelMessenger serverChannelMessenger;
  private RemoteMessenger client1RemoteMessenger;
  private ChannelMessenger client1ChannelMessenger;
  private RemoteMessenger client2RemoteMessenger;
  private ChannelMessenger client2ChannelMessenger;
  private final TestChatListener serverChatListener = new TestChatListener();
  private final TestChatListener client1ChatListener = new TestChatListener();
  private final TestChatListener client2ChatListener = new TestChatListener();
  private NullModeratorController serverModeratorController;

  @BeforeEach
  public void setUp() throws Exception {
    serverMessenger = new ServerMessenger("Server", 0);
    serverMessenger.setAcceptNewConnections(true);
    final int serverPort = serverMessenger.getLocalNode().getSocketAddress().getPort();
    final String mac = MacFinder.getHashedMacAddress();
    client1Messenger = new ClientMessenger("localhost", serverPort, "client1", mac);
    client2Messenger = new ClientMessenger("localhost", serverPort, "client2", mac);
    final UnifiedMessenger serverUnifiedMessenger = new UnifiedMessenger(serverMessenger);
    serverRemoteMessenger = new RemoteMessenger(serverUnifiedMessenger);
    serverChannelMessenger = new ChannelMessenger(serverUnifiedMessenger);
    final UnifiedMessenger client1UnifiedMessenger = new UnifiedMessenger(client1Messenger);
    client1RemoteMessenger = new RemoteMessenger(client1UnifiedMessenger);
    client1ChannelMessenger = new ChannelMessenger(client1UnifiedMessenger);
    final UnifiedMessenger client2UnifiedMessenger = new UnifiedMessenger(client2Messenger);
    client2RemoteMessenger = new RemoteMessenger(client2UnifiedMessenger);
    client2ChannelMessenger = new ChannelMessenger(client2UnifiedMessenger);
    serverModeratorController = new NullModeratorController(serverMessenger, null);
    serverModeratorController.register(serverRemoteMessenger);
  }

  @AfterEach
  public void tearDown() {
    if (serverMessenger != null) {
      serverMessenger.shutDown();
    }
    if (client1Messenger != null) {
      client1Messenger.shutDown();
    }
    if (client2Messenger != null) {
      client2Messenger.shutDown();
    }
  }

  @Test
  public void shouldBeAbleToChatAcrossMultipleNodes() {
    assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
      final ChatController controller = newChatController();
      final Chat server = newChat(serverMessenger, serverChannelMessenger, serverRemoteMessenger);
      server.addChatListener(serverChatListener);
      final Chat client1 = newChat(client1Messenger, client1ChannelMessenger, client1RemoteMessenger);
      client1.addChatListener(client1ChatListener);
      final Chat client2 = newChat(client2Messenger, client2ChannelMessenger, client2RemoteMessenger);
      client2.addChatListener(client2ChatListener);
      waitFor(this::allNodesToConnect);

      sendMessagesFrom(client2);
      sendMessagesFrom(server);
      sendMessagesFrom(client1);
      waitFor(this::allMessagesToArrive);

      client1.shutdown();
      client2.shutdown();
      waitFor(this::clientNodesToDisconnect);

      controller.deactivate();
      waitFor(this::serverNodeToDisconnect);
    });
  }

  private ChatController newChatController() {
    return new ChatController(
        CHAT_NAME,
        serverMessenger,
        serverRemoteMessenger,
        serverChannelMessenger,
        serverModeratorController);
  }

  private static Chat newChat(
      final IMessenger messenger,
      final IChannelMessenger channelMessenger,
      final IRemoteMessenger remoteMessenger) {
    return new Chat(messenger, CHAT_NAME, channelMessenger, remoteMessenger, Chat.ChatSoundProfile.NO_SOUND);
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
    assertThat(serverChatListener.playerCount.get(), is(NODE_COUNT));
    assertThat(client1ChatListener.playerCount.get(), is(NODE_COUNT));
    assertThat(client2ChatListener.playerCount.get(), is(NODE_COUNT));
  }

  private void allMessagesToArrive() {
    assertThat(serverChatListener.messageCount.get(), is(NODE_COUNT * MESSAGE_COUNT));
    assertThat(client1ChatListener.messageCount.get(), is(NODE_COUNT * MESSAGE_COUNT));
    assertThat(client2ChatListener.messageCount.get(), is(NODE_COUNT * MESSAGE_COUNT));
  }

  private void clientNodesToDisconnect() {
    assertThat(serverChatListener.playerCount.get(), is(1));
  }

  private void serverNodeToDisconnect() {
    assertThat(serverChatListener.playerCount.get(), is(0));
  }

  private static void sendMessagesFrom(final Chat node) {
    new Thread(() -> {
      IntStream.range(0, MESSAGE_COUNT).forEach(i -> node.sendMessage("Test", false));
    }).start();
  }

  private static final class TestChatListener implements IChatListener {
    final AtomicInteger playerCount = new AtomicInteger();
    final AtomicInteger messageCount = new AtomicInteger();

    @Override
    public void updatePlayerList(final Collection<INode> players) {
      playerCount.set(players.size());
    }

    @Override
    public void addMessageWithSound(final String message, final String from, final boolean thirdperson,
        final String sound) {
      messageCount.incrementAndGet();
    }

    @Override
    public void addMessage(final String message, final String from, final boolean thirdperson) {
      addMessageWithSound(message, from, thirdperson, SoundPath.CLIP_CHAT_MESSAGE);
    }

    @Override
    public void addStatusMessage(final String message) {}
  }
}
