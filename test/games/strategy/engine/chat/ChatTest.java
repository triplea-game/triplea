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
import games.strategy.engine.message.UnifiedMessenger;
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
  private IServerMessenger m_server;
  private IMessenger m_client1;
  private IMessenger m_client2;
  UnifiedMessenger m_sum;
  RemoteMessenger m_srm;
  ChannelMessenger m_scm;
  UnifiedMessenger m_c1um;
  RemoteMessenger m_c1rm;
  ChannelMessenger m_c1cm;
  UnifiedMessenger m_c2um;
  RemoteMessenger m_c2rm;
  ChannelMessenger m_c2cm;
  TestChatListener m_serverChatListener;
  TestChatListener m_client1ChatListener;
  TestChatListener m_client2ChatListener;
  NullModeratorController m_smc;

  @Before
  public void setUp() throws IOException {
    SERVER_PORT = TestUtil.getUniquePort();
    m_server = new ServerMessenger("Server", SERVER_PORT);
    m_server.setAcceptNewConnections(true);
    final String mac = MacFinder.getHashedMacAddress();
    m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1", mac);
    m_client2 = new ClientMessenger("localhost", SERVER_PORT, "client2", mac);
    m_sum = new UnifiedMessenger(m_server);
    m_srm = new RemoteMessenger(m_sum);
    m_scm = new ChannelMessenger(m_sum);
    m_c1um = new UnifiedMessenger(m_client1);
    m_c1rm = new RemoteMessenger(m_c1um);
    m_c1cm = new ChannelMessenger(m_c1um);
    m_c2um = new UnifiedMessenger(m_client2);
    m_c2rm = new RemoteMessenger(m_c2um);
    m_c2cm = new ChannelMessenger(m_c2um);
    m_smc = new NullModeratorController(m_server, null);
    m_smc.register(m_srm);
    m_serverChatListener = new TestChatListener();
    m_client1ChatListener = new TestChatListener();
    m_client2ChatListener = new TestChatListener();
  }

  @After
  public void tearDown() {
    try {
      if (m_server != null) {
        m_server.shutDown();
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
    try {
      if (m_client1 != null) {
        m_client1.shutDown();
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
    try {
      if (m_client2 != null) {
        m_client2.shutDown();
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
    final ChatController controller = new ChatController("c", m_server, m_srm, m_scm, m_smc);
    final Chat server = new Chat(m_server, "c", m_scm, m_srm, Chat.CHAT_SOUND_PROFILE.NO_SOUND);
    server.addChatListener(m_serverChatListener);
    final Chat client1 = new Chat(m_client1, "c", m_c1cm, m_c1rm, Chat.CHAT_SOUND_PROFILE.NO_SOUND);
    client1.addChatListener(m_client1ChatListener);
    final Chat client2 = new Chat(m_client2, "c", m_c2cm, m_c2rm, Chat.CHAT_SOUND_PROFILE.NO_SOUND);
    client2.addChatListener(m_client2ChatListener);
    // we need to wait for all the messages to write
    for (int i = 0; i < 10; i++) {
      try {
        assertEquals(m_client1ChatListener.m_players.size(), 3);
        assertEquals(m_client2ChatListener.m_players.size(), 3);
        assertEquals(m_serverChatListener.m_players.size(), 3);
        break;
      } catch (final AssertionError e) {
        ThreadUtil.sleep(25);
      }
    }
    assertEquals(m_client1ChatListener.m_players.size(), 3);
    assertEquals(m_client2ChatListener.m_players.size(), 3);
    assertEquals(m_serverChatListener.m_players.size(), 3);
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
        assertEquals(m_client1ChatListener.m_messages.size(), 3 * messageCount);
        assertEquals(m_client2ChatListener.m_messages.size(), 3 * messageCount);
        assertEquals(m_serverChatListener.m_messages.size(), 3 * messageCount);
        break;
      } catch (final AssertionError afe) {
        ThreadUtil.sleep(25);
      }
    }
    assertEquals(m_client1ChatListener.m_messages.size(), 3 * messageCount);
    assertEquals(m_client2ChatListener.m_messages.size(), 3 * messageCount);
    assertEquals(m_serverChatListener.m_messages.size(), 3 * messageCount);
    client1.shutdown();
    client2.shutdown();
    // we need to wait for all the messages to write
    for (int i = 0; i < 10; i++) {
      try {
        assertEquals(m_serverChatListener.m_players.size(), 1);
        break;
      } catch (final AssertionError e) {
        ThreadUtil.sleep(25);
      }
    }
    assertEquals(m_serverChatListener.m_players.size(), 1);
    controller.deactivate();
    for (int i = 0; i < 10; i++) {
      try {
        assertEquals(m_serverChatListener.m_players.size(), 0);
        break;
      } catch (final AssertionError afe) {
        ThreadUtil.sleep(25);
      }
    }
    assertEquals(m_serverChatListener.m_players.size(), 0);
  }
}


class TestChatListener implements IChatListener {
  public List<INode> m_players;
  public List<String> m_messages = new ArrayList<>();
  public List<Boolean> m_thirdPerson = new ArrayList<>();
  public List<String> m_from = new ArrayList<>();

  @Override
  public void updatePlayerList(final Collection<INode> players) {
    synchronized (this) {
      m_players = new ArrayList<>(players);
    }
  }

  @Override
  public void addMessageWithSound(final String message, final String from, final boolean thirdperson,
      final String sound) {
    synchronized (this) {
      m_messages.add(message);
      m_thirdPerson.add(thirdperson);
      m_from.add(from);
    }
  }

  @Override
  public void addMessage(final String message, final String from, final boolean thirdperson) {
    addMessageWithSound(message, from, thirdperson, SoundPath.CLIP_CHAT_MESSAGE);
  }

  @Override
  public void addStatusMessage(final String message) {}
}
