package games.strategy.engine.chat;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.lobby.server.NullModeratorController;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.SoundPath;
import games.strategy.test.TestUtil;
import games.strategy.util.ThreadUtil;

public class ChatTest {
  private static int SERVER_PORT = -1;
  private IServerMessenger serverMessenger;
  private IMessenger client1Messenger;
  private IMessenger client2Messenger;
  UnifiedMessenger serverUnifiedMessenger;
  RemoteMessenger serverRemoteMessenger;
  ChannelMessenger serverChannelMessenger;
  UnifiedMessenger client1UnifiedMessenger;
  RemoteMessenger client1RemoteMessenger;
  ChannelMessenger client1ChannelMessenger;
  UnifiedMessenger client2UnifiedMessenger;
  RemoteMessenger client2RemoteMessenger;
  ChannelMessenger client2ChannelMessenger;
  TestChatListener serverChatListener;
  TestChatListener client1ChatListener;
  TestChatListener client2ChatListener;
  NullModeratorController serverModeratorController;

  @Before
  public void setUp() throws IOException {
    SERVER_PORT = TestUtil.getUniquePort();
    serverMessenger = new ServerMessenger("Server", SERVER_PORT);
    serverMessenger.setAcceptNewConnections(true);
    final String mac = MacFinder.getHashedMacAddress();
    client1Messenger = new ClientMessenger("localhost", SERVER_PORT, "client1", mac);
    client2Messenger = new ClientMessenger("localhost", SERVER_PORT, "client2", mac);
    serverUnifiedMessenger = new UnifiedMessenger(serverMessenger);
    serverRemoteMessenger = new RemoteMessenger(serverUnifiedMessenger);
    serverChannelMessenger = new ChannelMessenger(serverUnifiedMessenger);
    client1UnifiedMessenger = new UnifiedMessenger(client1Messenger);
    client1RemoteMessenger = new RemoteMessenger(client1UnifiedMessenger);
    client1ChannelMessenger = new ChannelMessenger(client1UnifiedMessenger);
    client2UnifiedMessenger = new UnifiedMessenger(client2Messenger);
    client2RemoteMessenger = new RemoteMessenger(client2UnifiedMessenger);
    client2ChannelMessenger = new ChannelMessenger(client2UnifiedMessenger);
    serverModeratorController = new NullModeratorController(serverMessenger, null);
    serverModeratorController.register(serverRemoteMessenger);
    serverChatListener = new TestChatListener();
    client1ChatListener = new TestChatListener();
    client2ChatListener = new TestChatListener();
  }

  @After
  public void tearDown() {
    try {
      if (serverMessenger != null) {
        serverMessenger.shutDown();
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
    try {
      if (client1Messenger != null) {
        client1Messenger.shutDown();
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
    try {
      if (client2Messenger != null) {
        client2Messenger.shutDown();
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
  }

  @Test
  public void testAll() throws Exception {
    // this is a rather big and ugly unit test
    // its just that the chat is so hard to set up
    // and we really need to test it working with sockets
    // rather than some mocked up implementation
    final ChatController controller = new ChatController("c", serverMessenger, serverRemoteMessenger,
        serverChannelMessenger, serverModeratorController);
    final Chat server =
        new Chat(serverMessenger, "c", serverChannelMessenger, serverRemoteMessenger, Chat.CHAT_SOUND_PROFILE.NO_SOUND);
    server.addChatListener(serverChatListener);
    final Chat client1 = new Chat(client1Messenger, "c", client1ChannelMessenger, client1RemoteMessenger,
        Chat.CHAT_SOUND_PROFILE.NO_SOUND);
    client1.addChatListener(client1ChatListener);
    final Chat client2 = new Chat(client2Messenger, "c", client2ChannelMessenger, client2RemoteMessenger,
        Chat.CHAT_SOUND_PROFILE.NO_SOUND);
    client2.addChatListener(client2ChatListener);
    // we need to wait for all the messages to write
    for (int i = 0; i < 10; i++) {
      try {
        assertEquals(client1ChatListener.players.size(), 3);
        assertEquals(client2ChatListener.players.size(), 3);
        assertEquals(serverChatListener.players.size(), 3);
        break;
      } catch (final AssertionError e) {
        ThreadUtil.sleep(25);
      }
    }
    assertEquals(client1ChatListener.players.size(), 3);
    assertEquals(client2ChatListener.players.size(), 3);
    assertEquals(serverChatListener.players.size(), 3);
    // send 50 messages, each client sending messages on a different thread.
    final int messageCount = 50;
    final Runnable client2Send = new Runnable() {
      @Override
      public void run() {
        for (int i = 0; i < messageCount; i++) {
          client2.sendMessage("Test", false);
        }
      }
    };
    final Thread clientThread = new Thread(client2Send);
    clientThread.start();
    final Runnable serverSend = new Runnable() {
      @Override
      public void run() {
        for (int i = 0; i < messageCount; i++) {
          server.sendMessage("Test", false);
        }
      }
    };
    final Thread serverThread = new Thread(serverSend);
    serverThread.start();
    for (int i = 0; i < messageCount; i++) {
      client1.sendMessage("Test", false);
    }
    serverThread.join();
    clientThread.join();
    // we need to wait for all the messages to write
    for (int i = 0; i < 10; i++) {
      try {
        assertEquals(client1ChatListener.messages.size(), 3 * messageCount);
        assertEquals(client2ChatListener.messages.size(), 3 * messageCount);
        assertEquals(serverChatListener.messages.size(), 3 * messageCount);
        break;
      } catch (final AssertionError afe) {
        ThreadUtil.sleep(25);
      }
    }
    assertEquals(client1ChatListener.messages.size(), 3 * messageCount);
    assertEquals(client2ChatListener.messages.size(), 3 * messageCount);
    assertEquals(serverChatListener.messages.size(), 3 * messageCount);
    client1.shutdown();
    client2.shutdown();
    // we need to wait for all the messages to write
    for (int i = 0; i < 10; i++) {
      try {
        assertEquals(serverChatListener.players.size(), 1);
        break;
      } catch (final AssertionError e) {
        ThreadUtil.sleep(25);
      }
    }
    assertEquals(serverChatListener.players.size(), 1);
    controller.deactivate();
    for (int i = 0; i < 10; i++) {
      try {
        assertEquals(serverChatListener.players.size(), 0);
        break;
      } catch (final AssertionError afe) {
        ThreadUtil.sleep(25);
      }
    }
    assertEquals(serverChatListener.players.size(), 0);
  }

  private static class TestChatListener implements IChatListener {
    public List<INode> players;
    public List<String> messages = new ArrayList<>();

    @Override
    public void updatePlayerList(final Collection<INode> players) {
      synchronized (this) {
        this.players = new ArrayList<>(players);
      }
    }

    @Override
    public void addMessageWithSound(final String message, final String from, final boolean thirdperson,
        final String sound) {
      synchronized (this) {
        messages.add(message);
      }
    }

    @Override
    public void addMessage(final String message, final String from, final boolean thirdperson) {
      addMessageWithSound(message, from, thirdperson, SoundPath.CLIP_CHAT_MESSAGE);
    }

    @Override
    public void addStatusMessage(final String message) {}
  }
}
