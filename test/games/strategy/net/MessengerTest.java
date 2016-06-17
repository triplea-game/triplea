package games.strategy.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import games.strategy.debug.ClientLogger;
import games.strategy.test.TestUtil;
import games.strategy.util.ThreadUtil;
import junit.framework.TestCase;

public class MessengerTest {
  private static int SERVER_PORT = -1;
  private IServerMessenger m_server;
  private IMessenger m_client1;
  private IMessenger m_client2;
  private final MessageListener m_serverListener = new MessageListener();
  private final MessageListener m_client1Listener = new MessageListener();
  private final MessageListener m_client2Listener = new MessageListener();

  @Before
  public void setUp() throws IOException {
    SERVER_PORT = TestUtil.getUniquePort();
    m_server = new ServerMessenger("Server", SERVER_PORT);
    m_server.setAcceptNewConnections(true);
    m_server.addMessageListener(m_serverListener);
    final String mac = MacFinder.getHashedMacAddress();
    m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1", mac);
    m_client1.addMessageListener(m_client1Listener);
    m_client2 = new ClientMessenger("localhost", SERVER_PORT, "client2", mac);
    m_client2.addMessageListener(m_client2Listener);
    assertEquals(m_client1.getServerNode(), m_server.getLocalNode());
    assertEquals(m_client2.getServerNode(), m_server.getLocalNode());
    assertEquals(m_server.getServerNode(), m_server.getLocalNode());
    for (int i = 0; i < 100; i++) {
      if (m_server.getNodes().size() != 3) {
        ThreadUtil.sleep(1);
      } else {
        break;
      }
    }
    assertEquals(m_server.getNodes().size(), 3);
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
  public void testServerSend() {
    final String message = "Hello";
    m_server.send(message, m_client1.getLocalNode());
    assertEquals(m_client1Listener.getLastMessage(), message);
    assertEquals(m_client1Listener.getLastSender(), m_server.getLocalNode());
    assertEquals(m_client2Listener.getMessageCount(), 0);
  }

  @Test
  public void testServerSendToClient2() throws Exception {
    final String message = "Hello";
    m_server.send(message, m_client2.getLocalNode());
    assertEquals(m_client2Listener.getLastMessage(), message);
    assertEquals(m_client2Listener.getLastSender(), m_server.getLocalNode());
    assertEquals(m_client1Listener.getMessageCount(), 0);
  }

  @Test
  public void testClientSendToServer() {
    final String message = "Hello";
    m_client1.send(message, m_server.getLocalNode());
    assertEquals(m_serverListener.getLastMessage(), message);
    assertEquals(m_serverListener.getLastSender(), m_client1.getLocalNode());
    assertEquals(m_client1Listener.getMessageCount(), 0);
    assertEquals(m_client2Listener.getMessageCount(), 0);
  }

  @Test
  public void testClientSendToClient() throws InterruptedException {
    final String message = "Hello";
    m_client1.send(message, m_client2.getLocalNode());
    assertEquals(m_client2Listener.getLastMessage(), message);
    assertEquals(m_client2Listener.getLastSender(), m_client1.getLocalNode());
    assertEquals(m_client1Listener.getMessageCount(), 0);
    assertEquals(m_serverListener.getMessageCount(), 0);
  }

  @Test
  public void testClientSendToClientLargeMessage() throws InterruptedException {
    final int count = 1 * 1000 * 1000;
    final StringBuilder builder = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      builder.append('a');
    }
    final String message = builder.toString();
    m_client1.send(message, m_client2.getLocalNode());
    assertEquals(m_client2Listener.getLastMessage(), message);
    assertEquals(m_client2Listener.getLastSender(), m_client1.getLocalNode());
    assertEquals(m_client1Listener.getMessageCount(), 0);
    assertEquals(m_serverListener.getMessageCount(), 0);
  }

  @Test
  public void testServerBroadcast() {
    final String message = "Hello";
    m_server.broadcast(message);
    assertEquals(m_client1Listener.getLastMessage(), message);
    assertEquals(m_client1Listener.getLastSender(), m_server.getLocalNode());
    assertEquals(m_client2Listener.getLastMessage(), message);
    assertEquals(m_client2Listener.getLastSender(), m_server.getLocalNode());
    assertEquals(m_serverListener.getMessageCount(), 0);
  }

  @Test
  public void testClientBroadcast() {
    final String message = "Hello";
    m_client1.broadcast(message);
    assertEquals(m_client2Listener.getLastMessage(), message);
    assertEquals(m_client2Listener.getLastSender(), m_client1.getLocalNode());
    assertEquals(m_serverListener.getLastMessage(), message);
    assertEquals(m_serverListener.getLastSender(), m_client1.getLocalNode());
    assertEquals(m_client1Listener.getMessageCount(), 0);
  }

  @Test
  public void testMultipleServer() {
    for (int i = 0; i < 100; i++) {
      m_server.send(i, m_client1.getLocalNode());
    }
    for (int i = 0; i < 100; i++) {
      m_client1Listener.clearLastMessage();
    }
  }

  @Test
  public void testMultipleClientToClient() {
    for (int i = 0; i < 100; i++) {
      m_client1.send(i, m_client2.getLocalNode());
    }
    for (int i = 0; i < 100; i++) {
      m_client2Listener.clearLastMessage();
    }
  }

  @Test
  public void testMultipleMessages() throws Exception {
    final Thread t1 = new Thread(new MultipleMessageSender(m_server));
    final Thread t2 = new Thread(new MultipleMessageSender(m_client1));
    final Thread t3 = new Thread(new MultipleMessageSender(m_client2));
    t1.start();
    t2.start();
    t3.start();
    t1.join();
    t2.join();
    t3.join();
    for (int i = 0; i < 200; i++) {
      m_client1Listener.clearLastMessage();
    }
    for (int i = 0; i < 200; i++) {
      m_client2Listener.clearLastMessage();
    }
    for (int i = 0; i < 200; i++) {
      m_serverListener.clearLastMessage();
    }
  }

  @Test
  public void testCorrectNodeCountInRemove() throws InterruptedException {
    // when we receive the notification that a
    // connection has been lost, the node list
    // should reflect that change
    for (int i = 0; i < 100; i++) {
      if (m_server.getNodes().size() == 3) {
        break;
      }
      ThreadUtil.sleep(10);
    }
    final AtomicInteger m_serverCount = new AtomicInteger(3);
    m_server.addConnectionChangeListener(new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        m_serverCount.decrementAndGet();
      }

      @Override
      public void connectionAdded(final INode to) {
        fail();
      }
    });
    m_client1.shutDown();
    for (int i = 0; i < 100; i++) {
      if (m_server.getNodes().size() == 2) {
        ThreadUtil.sleep(10);
        break;
      }
      ThreadUtil.sleep(10);
    }
    assertEquals(2, m_serverCount.get());
  }

  @Test
  public void testDisconnect() throws InterruptedException {
    for (int i = 0; i < 100; i++) {
      if (m_server.getNodes().size() == 3) {
        break;
      }
      ThreadUtil.sleep(10);
    }
    assertEquals(3, m_server.getNodes().size());
    m_client1.shutDown();
    m_client2.shutDown();
    for (int i = 0; i < 100; i++) {
      if (m_server.getNodes().size() == 1) {
        ThreadUtil.sleep(10);
        break;
      }
      ThreadUtil.sleep(1);
    }
    assertEquals(m_server.getNodes().size(), 1);
  }

  @Test
  public void testClose() throws InterruptedException {
    final AtomicBoolean closed = new AtomicBoolean(false);
    m_client1.addErrorListener(new IMessengerErrorListener() {
      @Override
      public void messengerInvalid(final IMessenger messenger, final Exception reason) {
        closed.set(true);
      }
    });
    m_server.removeConnection(m_client1.getLocalNode());
    int waitCount = 0;
    while (!closed.get() && waitCount < 10) {
      ThreadUtil.sleep(40);
      waitCount++;
    }
    assert (closed.get());
  }

  @Test
  public void testManyClients() throws UnknownHostException, CouldNotLogInException, IOException, InterruptedException {
    final int count = 5;
    final List<ClientMessenger> clients = new ArrayList<>();
    final List<MessageListener> listeners = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      final String name = "newClient" + i;
      final String mac = MacFinder.getHashedMacAddress();
      final ClientMessenger messenger = new ClientMessenger("localhost", SERVER_PORT, name, mac);
      final MessageListener listener = new MessageListener();
      messenger.addMessageListener(listener);
      clients.add(messenger);
      listeners.add(listener);
    }

    m_server.broadcast("TEST");
    for (final MessageListener listener : listeners) {
      assertEquals("TEST", listener.getLastMessage());
    }
    for (int i = 0; i < count; i++) {
      clients.get(i).shutDown();
    }
  }
}


class MessageListener implements IMessageListener {
  private final List<Serializable> messages = new ArrayList<>();
  private final ArrayList<INode> senders = new ArrayList<>();
  private final Object lock = new Object();

  public MessageListener() {}

  @Override
  public void messageReceived(final Serializable msg, final INode from) {
    synchronized (lock) {
      messages.add(msg);
      senders.add(from);
      lock.notifyAll();
    }
  }

  public void clearLastMessage() {
    synchronized (lock) {
      if (messages.isEmpty()) {
        waitForMessage();
      }
      messages.remove(0);
      senders.remove(0);
    }
  }

  public Object getLastMessage() {
    synchronized (lock) {
      if (messages.isEmpty()) {
        waitForMessage();
      }
      TestCase.assertFalse(messages.isEmpty());
      return messages.get(0);
    }
  }

  public INode getLastSender() {
    synchronized (lock) {
      if (messages.isEmpty()) {
        waitForMessage();
      }
      return senders.get(0);
    }
  }

  private void waitForMessage() {
    try {
      lock.wait(1500);
    } catch (final InterruptedException e) {
      fail("unexpected exception: " + e.getMessage());
    }
  }

  public int getMessageCount() {
    synchronized (lock) {
      return messages.size();
    }
  }
}


class MultipleMessageSender implements Runnable {
  IMessenger m_messenger;

  public MultipleMessageSender(final IMessenger messenger) {
    m_messenger = messenger;
  }

  @Override
  public void run() {
    Thread.yield();
    for (int i = 0; i < 100; i++) {
      m_messenger.broadcast(new Integer(i));
    }
  }
}
