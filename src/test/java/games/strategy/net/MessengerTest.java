package games.strategy.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

public class MessengerTest {
  private static int SERVER_PORT = -1;
  private IServerMessenger serverMessenger;
  private IMessenger client1Messenger;
  private IMessenger client2Messenger;
  private final MessageListener serverMessageListener = new MessageListener();
  private final MessageListener client1MessageListener = new MessageListener();
  private final MessageListener client2MessageListener = new MessageListener();

  @Before
  public void setUp() throws IOException {
    SERVER_PORT = TestUtil.getUniquePort();
    serverMessenger = new ServerMessenger("Server", SERVER_PORT);
    serverMessenger.setAcceptNewConnections(true);
    serverMessenger.addMessageListener(serverMessageListener);
    final String mac = MacFinder.getHashedMacAddress();
    client1Messenger = new ClientMessenger("localhost", SERVER_PORT, "client1", mac);
    client1Messenger.addMessageListener(client1MessageListener);
    client2Messenger = new ClientMessenger("localhost", SERVER_PORT, "client2", mac);
    client2Messenger.addMessageListener(client2MessageListener);
    assertEquals(client1Messenger.getServerNode(), serverMessenger.getLocalNode());
    assertEquals(client2Messenger.getServerNode(), serverMessenger.getLocalNode());
    assertEquals(serverMessenger.getServerNode(), serverMessenger.getLocalNode());
    for (int i = 0; i < 100; i++) {
      if (serverMessenger.getNodes().size() != 3) {
        ThreadUtil.sleep(1);
      } else {
        break;
      }
    }
    assertEquals(serverMessenger.getNodes().size(), 3);
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
  public void testServerSend() {
    final String message = "Hello";
    serverMessenger.send(message, client1Messenger.getLocalNode());
    assertEquals(client1MessageListener.getLastMessage(), message);
    assertEquals(client1MessageListener.getLastSender(), serverMessenger.getLocalNode());
    assertEquals(client2MessageListener.getMessageCount(), 0);
  }

  @Test
  public void testServerSendToClient2() throws Exception {
    final String message = "Hello";
    serverMessenger.send(message, client2Messenger.getLocalNode());
    assertEquals(client2MessageListener.getLastMessage(), message);
    assertEquals(client2MessageListener.getLastSender(), serverMessenger.getLocalNode());
    assertEquals(client1MessageListener.getMessageCount(), 0);
  }

  @Test
  public void testClientSendToServer() {
    final String message = "Hello";
    client1Messenger.send(message, serverMessenger.getLocalNode());
    assertEquals(serverMessageListener.getLastMessage(), message);
    assertEquals(serverMessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(client1MessageListener.getMessageCount(), 0);
    assertEquals(client2MessageListener.getMessageCount(), 0);
  }

  @Test
  public void testClientSendToClient() {
    final String message = "Hello";
    client1Messenger.send(message, client2Messenger.getLocalNode());
    assertEquals(client2MessageListener.getLastMessage(), message);
    assertEquals(client2MessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(client1MessageListener.getMessageCount(), 0);
    assertEquals(serverMessageListener.getMessageCount(), 0);
  }

  @Test
  public void testClientSendToClientLargeMessage() {
    final int count = 1 * 1000 * 1000;
    final StringBuilder builder = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      builder.append('a');
    }
    final String message = builder.toString();
    client1Messenger.send(message, client2Messenger.getLocalNode());
    assertEquals(client2MessageListener.getLastMessage(), message);
    assertEquals(client2MessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(client1MessageListener.getMessageCount(), 0);
    assertEquals(serverMessageListener.getMessageCount(), 0);
  }

  @Test
  public void testServerBroadcast() {
    final String message = "Hello";
    serverMessenger.broadcast(message);
    assertEquals(client1MessageListener.getLastMessage(), message);
    assertEquals(client1MessageListener.getLastSender(), serverMessenger.getLocalNode());
    assertEquals(client2MessageListener.getLastMessage(), message);
    assertEquals(client2MessageListener.getLastSender(), serverMessenger.getLocalNode());
    assertEquals(serverMessageListener.getMessageCount(), 0);
  }

  @Test
  public void testClientBroadcast() {
    final String message = "Hello";
    client1Messenger.broadcast(message);
    assertEquals(client2MessageListener.getLastMessage(), message);
    assertEquals(client2MessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(serverMessageListener.getLastMessage(), message);
    assertEquals(serverMessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(client1MessageListener.getMessageCount(), 0);
  }

  @Test
  public void testMultipleServer() {
    for (int i = 0; i < 100; i++) {
      serverMessenger.send(i, client1Messenger.getLocalNode());
    }
    for (int i = 0; i < 100; i++) {
      client1MessageListener.clearLastMessage();
    }
  }

  @Test
  public void testMultipleClientToClient() {
    for (int i = 0; i < 100; i++) {
      client1Messenger.send(i, client2Messenger.getLocalNode());
    }
    for (int i = 0; i < 100; i++) {
      client2MessageListener.clearLastMessage();
    }
  }

  @Test
  public void testMultipleMessages() throws Exception {
    final Thread t1 = new Thread(new MultipleMessageSender(serverMessenger));
    final Thread t2 = new Thread(new MultipleMessageSender(client1Messenger));
    final Thread t3 = new Thread(new MultipleMessageSender(client2Messenger));
    t1.start();
    t2.start();
    t3.start();
    t1.join();
    t2.join();
    t3.join();
    for (int i = 0; i < 200; i++) {
      client1MessageListener.clearLastMessage();
    }
    for (int i = 0; i < 200; i++) {
      client2MessageListener.clearLastMessage();
    }
    for (int i = 0; i < 200; i++) {
      serverMessageListener.clearLastMessage();
    }
  }

  @Test
  public void testCorrectNodeCountInRemove() {
    // when we receive the notification that a
    // connection has been lost, the node list
    // should reflect that change
    for (int i = 0; i < 100; i++) {
      if (serverMessenger.getNodes().size() == 3) {
        break;
      }
      ThreadUtil.sleep(10);
    }
    final AtomicInteger m_serverCount = new AtomicInteger(3);
    serverMessenger.addConnectionChangeListener(new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        m_serverCount.decrementAndGet();
      }

      @Override
      public void connectionAdded(final INode to) {
        fail();
      }
    });
    client1Messenger.shutDown();
    for (int i = 0; i < 100; i++) {
      if (serverMessenger.getNodes().size() == 2) {
        ThreadUtil.sleep(10);
        break;
      }
      ThreadUtil.sleep(10);
    }
    assertEquals(2, m_serverCount.get());
  }

  @Test
  public void testDisconnect() {
    for (int i = 0; i < 100; i++) {
      if (serverMessenger.getNodes().size() == 3) {
        break;
      }
      ThreadUtil.sleep(10);
    }
    assertEquals(3, serverMessenger.getNodes().size());
    client1Messenger.shutDown();
    client2Messenger.shutDown();
    for (int i = 0; i < 100; i++) {
      if (serverMessenger.getNodes().size() == 1) {
        ThreadUtil.sleep(10);
        break;
      }
      ThreadUtil.sleep(1);
    }
    assertEquals(serverMessenger.getNodes().size(), 1);
  }

  @Test
  public void testClose() {
    final AtomicBoolean closed = new AtomicBoolean(false);
    client1Messenger.addErrorListener(new IMessengerErrorListener() {
      @Override
      public void messengerInvalid(final IMessenger messenger, final Exception reason) {
        closed.set(true);
      }
    });
    serverMessenger.removeConnection(client1Messenger.getLocalNode());
    int waitCount = 0;
    while (!closed.get() && waitCount < 10) {
      ThreadUtil.sleep(40);
      waitCount++;
    }
    assert (closed.get());
  }

  @Test
  public void testManyClients() throws UnknownHostException, CouldNotLogInException, IOException {
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

    serverMessenger.broadcast("TEST");
    for (final MessageListener listener : listeners) {
      assertEquals("TEST", listener.getLastMessage());
    }
    for (int i = 0; i < count; i++) {
      clients.get(i).shutDown();
    }
  }

  private static class MessageListener implements IMessageListener {
    private final List<Serializable> messages = new ArrayList<>();
    private final ArrayList<INode> senders = new ArrayList<>();
    private final Object lock = new Object();

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
        assertFalse(messages.isEmpty());
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

  private static class MultipleMessageSender implements Runnable {
    IMessenger messenger;

    public MultipleMessageSender(final IMessenger messenger) {
      this.messenger = messenger;
    }

    @Override
    public void run() {
      Thread.yield();
      for (int i = 0; i < 100; i++) {
        messenger.broadcast(i);
      }
    }
  }
}
