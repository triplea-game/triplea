package games.strategy.net;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.GuardedBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.SystemId;
import org.triplea.swing.DialogBuilder;
import org.triplea.test.common.Integration;
import org.triplea.util.Version;

@Integration
class MessengerIntegrationTest {
  private IServerMessenger serverMessenger;
  private IClientMessenger client1Messenger;
  private IMessenger client2Messenger;
  private final MessageListener serverMessageListener = new MessageListener();
  private final MessageListener client1MessageListener = new MessageListener();
  private final MessageListener client2MessageListener = new MessageListener();

  @BeforeEach
  void setUp() throws Exception {
    DialogBuilder.disableUi();
    serverMessenger = new TestServerMessenger();
    serverMessenger.setAcceptNewConnections(true);
    serverMessenger.addMessageListener(serverMessageListener);
    final int serverPort = serverMessenger.getLocalNode().getSocketAddress().getPort();
    client1Messenger =
        new ClientMessenger(
            "localhost", serverPort, "client1", SystemId.of("system-id"), new Version(2, 0, 0));
    client1Messenger.addMessageListener(client1MessageListener);
    client2Messenger =
        new ClientMessenger(
            "localhost", serverPort, "client2", SystemId.of("system-id"), new Version(2, 0, 0));
    client2Messenger.addMessageListener(client2MessageListener);
    assertEquals(client1Messenger.getServerNode(), serverMessenger.getLocalNode());
    assertEquals(client2Messenger.getServerNode(), serverMessenger.getLocalNode());
    assertEquals(serverMessenger.getServerNode(), serverMessenger.getLocalNode());
    await().until(serverMessenger::getNodes, hasSize(3));
  }

  @AfterEach
  void tearDown() {
    MessengerTestUtils.shutDownQuietly(serverMessenger);
    MessengerTestUtils.shutDownQuietly(client1Messenger);
    MessengerTestUtils.shutDownQuietly(client2Messenger);
  }

  @Test
  void testServerSend() {
    final String message = "Hello";
    serverMessenger.send(message, client1Messenger.getLocalNode());
    assertEquals(message, client1MessageListener.getLastMessage());
    assertEquals(client1MessageListener.getLastSender(), serverMessenger.getLocalNode());
    assertEquals(0, client2MessageListener.getMessageCount());
  }

  @Test
  void testServerSendToClient2() {
    final String message = "Hello";
    serverMessenger.send(message, client2Messenger.getLocalNode());
    assertEquals(message, client2MessageListener.getLastMessage());
    assertEquals(client2MessageListener.getLastSender(), serverMessenger.getLocalNode());
    assertEquals(0, client1MessageListener.getMessageCount());
  }

  @Test
  void testClientSendToServer() {
    final String message = "Hello";
    client1Messenger.send(message, serverMessenger.getLocalNode());
    assertEquals(message, serverMessageListener.getLastMessage());
    assertEquals(serverMessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(0, client1MessageListener.getMessageCount());
    assertEquals(0, client2MessageListener.getMessageCount());
  }

  @Test
  void testClientSendToClient() {
    final String message = "Hello";
    client1Messenger.send(message, client2Messenger.getLocalNode());
    assertEquals(message, client2MessageListener.getLastMessage());
    assertEquals(client2MessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(0, client1MessageListener.getMessageCount());
    assertEquals(0, serverMessageListener.getMessageCount());
  }

  @Test
  void testClientSendToClientLargeMessage() {
    final int count = 1_000_000;
    final String message = "a".repeat(count);
    client1Messenger.send(message, client2Messenger.getLocalNode());
    assertEquals(client2MessageListener.getLastMessage(), message);
    assertEquals(client2MessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(0, client1MessageListener.getMessageCount());
    assertEquals(0, serverMessageListener.getMessageCount());
  }

  @Test
  void testMultipleServer() {
    for (int i = 0; i < 100; i++) {
      serverMessenger.send(i, client1Messenger.getLocalNode());
    }
    for (int i = 0; i < 100; i++) {
      client1MessageListener.clearLastMessage();
    }
  }

  @Test
  void testMultipleClientToClient() {
    for (int i = 0; i < 100; i++) {
      client1Messenger.send(i, client2Messenger.getLocalNode());
    }
    for (int i = 0; i < 100; i++) {
      client2MessageListener.clearLastMessage();
    }
  }

  @Test
  void testCorrectNodeCountInRemove() {
    // when we receive the notification that a connection has been lost, the node list should
    // reflect that change
    await().until(serverMessenger::getNodes, hasSize(3));
    final AtomicInteger serverCount = new AtomicInteger(3);
    serverMessenger.addConnectionChangeListener(
        new IConnectionChangeListener() {
          @Override
          public void connectionRemoved(final INode to) {
            serverCount.decrementAndGet();
          }

          @Override
          public void connectionAdded(final INode to) {
            fail("A connection should not be added.");
          }
        });
    client1Messenger.shutDown();
    await().until(serverMessenger::getNodes, hasSize(2));
    assertEquals(2, serverCount.get());
  }

  @Test
  void testDisconnect() {
    await().until(serverMessenger::getNodes, hasSize(3));
    client1Messenger.shutDown();
    client2Messenger.shutDown();
    await().until(serverMessenger::getNodes, hasSize(1));
  }

  @Test
  void testClose() {
    final AtomicBoolean closed = new AtomicBoolean(false);
    client1Messenger.addErrorListener(reason -> closed.set(true));
    serverMessenger.removeConnection(client1Messenger.getLocalNode());
    await().untilTrue(closed);
  }

  private static class MessageListener implements IMessageListener {
    private final List<Serializable> messages = new ArrayList<>();
    private final List<INode> senders = new ArrayList<>();
    private final Object lock = new Object();

    @Override
    public void messageReceived(final Serializable msg, final INode from) {
      synchronized (lock) {
        messages.add(msg);
        senders.add(from);
        lock.notifyAll();
      }
    }

    void clearLastMessage() {
      synchronized (lock) {
        waitForMessage();
        messages.remove(0);
        senders.remove(0);
      }
    }

    Object getLastMessage() {
      synchronized (lock) {
        waitForMessage();
        assertFalse(messages.isEmpty());
        return messages.get(0);
      }
    }

    INode getLastSender() {
      synchronized (lock) {
        waitForMessage();
        return senders.get(0);
      }
    }

    @GuardedBy("lock")
    private void waitForMessage() {
      assert Thread.holdsLock(lock);

      while (messages.isEmpty()) {
        try {
          lock.wait(1500);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          fail("unexpected exception: " + e.getMessage());
        }
      }
    }

    int getMessageCount() {
      synchronized (lock) {
        return messages.size();
      }
    }
  }
}
