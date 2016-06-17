package games.strategy.engine.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.MacFinder;
import games.strategy.net.ServerMessenger;
import games.strategy.test.TestUtil;
import games.strategy.util.ThreadUtil;

public class RemoteMessengerTest {
  private int SERVER_PORT = -1;
  private IMessenger m_messenger;
  private RemoteMessenger m_remoteMessenger;
  private UnifiedMessengerHub m_hub;

  @Before
  public void setUp() throws Exception {
    // simple set up for non networked testing
    m_messenger = new DummyMessenger();
    m_remoteMessenger = new RemoteMessenger(new UnifiedMessenger(m_messenger));
    SERVER_PORT = TestUtil.getUniquePort();
  }

  @After
  public void tearDown() throws Exception {
    m_messenger = null;
    m_remoteMessenger = null;
  }

  @Test
  public void testRegisterUnregister() {
    final TestRemote testRemote = new TestRemote();
    final RemoteName test = new RemoteName(ITestRemote.class, "test");
    m_remoteMessenger.registerRemote(testRemote, test);
    assertTrue(m_remoteMessenger.hasLocalImplementor(test));
    m_remoteMessenger.unregisterRemote(test);
    assertFalse(m_remoteMessenger.hasLocalImplementor(test));
  }

  @Test
  public void testMethodCall() {
    final TestRemote testRemote = new TestRemote();
    final RemoteName test = new RemoteName(ITestRemote.class, "test");
    m_remoteMessenger.registerRemote(testRemote, test);
    final ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote(test);
    assertEquals(2, remote.increment(1));
    assertEquals(testRemote.getLastSenderNode(), m_messenger.getLocalNode());
  }

  @Test
  public void testExceptionThrownWhenUnregisteredRemote() {
    final TestRemote testRemote = new TestRemote();
    final RemoteName test = new RemoteName(ITestRemote.class, "test");
    m_remoteMessenger.registerRemote(testRemote, test);
    final ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote(test);
    m_remoteMessenger.unregisterRemote("test");
    try {
      remote.increment(1);
      fail("No exception thrown");
    } catch (final RemoteNotFoundException rme) {
      // this is what we expect
    }
  }

  @Test
  public void testNoRemote() {
    final RemoteName test = new RemoteName(ITestRemote.class, "test");
    try {
      m_remoteMessenger.getRemote(test);
      final ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote(test);
      remote.testVoid();
      fail("No exception thrown");
    } catch (final RemoteNotFoundException rme) {
      // this is what we expect
    }
  }

  @Test
  public void testVoidMethodCall() {
    final TestRemote testRemote = new TestRemote();
    final RemoteName test = new RemoteName(ITestRemote.class, "test");
    m_remoteMessenger.registerRemote(testRemote, test);
    final ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote(test);
    remote.testVoid();
  }

  @Test
  public void testException() throws Exception {
    final TestRemote testRemote = new TestRemote();
    final RemoteName test = new RemoteName(ITestRemote.class, "test");
    m_remoteMessenger.registerRemote(testRemote, test);
    final ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote(test);
    try {
      remote.throwException();
    } catch (final Exception e) {
      // this is what we want
      if (e.getMessage().equals(TestRemote.EXCEPTION_STRING)) {
        return;
      }
      throw e;
    }
    fail("No exception thrown");
  }

  @Test
  public void testRemoteCall() throws Exception {
    final RemoteName test = new RemoteName(ITestRemote.class, "test");
    ServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new ServerMessenger("server", SERVER_PORT);
      server.setAcceptNewConnections(true);
      final String mac = MacFinder.getHashedMacAddress();
      client = new ClientMessenger("localhost", SERVER_PORT, "client", mac);
      final UnifiedMessenger serverUM = new UnifiedMessenger(server);
      m_hub = serverUM.getHub();
      final RemoteMessenger serverRM = new RemoteMessenger(serverUM);
      final RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger(client));
      // register it on the server
      final TestRemote testRemote = new TestRemote();
      serverRM.registerRemote(testRemote, test);
      // since the registration must go over a socket
      // and through a couple threads, wait for the
      // client to get it
      int waitCount = 0;
      while (!m_hub.hasImplementors(test.getName()) && waitCount < 20) {
        waitCount++;
        ThreadUtil.sleep(50);
      }
      // call it on the client
      final int rVal = ((ITestRemote) clientRM.getRemote(test)).increment(1);
      assertEquals(2, rVal);
      assertEquals(testRemote.getLastSenderNode(), client.getLocalNode());
    } finally {
      shutdownServerAndClient(server, client);
    }
  }

  private static void shutdownServerAndClient(ServerMessenger server, ClientMessenger client) {
    if (server != null) {
      server.shutDown();
    }
    if (client != null) {
      client.shutDown();
    }
  }

  @Test
  public void testRemoteCall2() throws Exception {
    final RemoteName test = new RemoteName(ITestRemote.class, "test");
    ServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new ServerMessenger("server", SERVER_PORT);
      server.setAcceptNewConnections(true);
      final String mac = MacFinder.getHashedMacAddress();
      client = new ClientMessenger("localhost", SERVER_PORT, "client", mac);
      final RemoteMessenger serverRM = new RemoteMessenger(new UnifiedMessenger(server));
      final TestRemote testRemote = new TestRemote();
      serverRM.registerRemote(testRemote, test);
      final RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger(client));
      // call it on the client
      // should be no need to wait since the constructor should not
      // reutrn until the initial state of the messenger is good
      final int rVal = ((ITestRemote) clientRM.getRemote(test)).increment(1);
      assertEquals(2, rVal);
      assertEquals(testRemote.getLastSenderNode(), client.getLocalNode());
    } finally {
      shutdownServerAndClient(server, client);
    }
  }

  @Test
  public void testShutDownClient() throws Exception {
    // when the client shutdown, remotes created
    // on the client should not be visible on server
    final RemoteName test = new RemoteName(ITestRemote.class, "test");
    ServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new ServerMessenger("server", SERVER_PORT);
      server.setAcceptNewConnections(true);
      final String mac = MacFinder.getHashedMacAddress();
      client = new ClientMessenger("localhost", SERVER_PORT, "client", mac);
      final UnifiedMessenger serverUM = new UnifiedMessenger(server);
      final RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger(client));
      clientRM.registerRemote(new TestRemote(), test);
      serverUM.getHub().waitForNodesToImplement(test.getName(), 200);
      assertTrue(serverUM.getHub().hasImplementors(test.getName()));
      client.shutDown();
      ThreadUtil.sleep(200);
      assertTrue(!serverUM.getHub().hasImplementors(test.getName()));
    } finally {
      shutdownServerAndClient(server, client);
    }
  }

  @Test
  public void testMethodReturnsOnWait() throws Exception {
    // when the client shutdown, remotes created
    // on the client should not be visible on server
    final RemoteName test = new RemoteName(IFoo.class, "test");
    ServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new ServerMessenger("server", SERVER_PORT);
      server.setAcceptNewConnections(true);
      final String mac = MacFinder.getHashedMacAddress();
      client = new ClientMessenger("localhost", SERVER_PORT, "client", mac);
      final UnifiedMessenger serverUM = new UnifiedMessenger(server);
      final RemoteMessenger serverRM = new RemoteMessenger(serverUM);
      final RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger(client));
      final Object lock = new Object();
      final AtomicBoolean started = new AtomicBoolean(false);
      final IFoo foo = new IFoo() {
        @Override
        public void foo() {
          synchronized (lock) {
            try {
              started.set(true);
              lock.wait();
            } catch (final InterruptedException e) {
              // ignore interrupted exception
            }
          }
        }
      };
      clientRM.registerRemote(foo, test);
      serverUM.getHub().waitForNodesToImplement(test.getName(), 200);
      assertTrue(serverUM.getHub().hasImplementors(test.getName()));
      final AtomicReference<ConnectionLostException> rme = new AtomicReference<>(null);
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          try {
            final IFoo remoteFoo = (IFoo) serverRM.getRemote(test);
            remoteFoo.foo();
          } catch (final ConnectionLostException e) {
            rme.set(e);
          }
        }
      };
      final Thread t = new Thread(r);
      t.start();
      // wait for the thread to start
      while (started.get() == false) {
        ThreadUtil.sleep(1);
      }
      ThreadUtil.sleep(20);
      // TODO: we are getting a RemoteNotFoundException because the client is disconnecting before the invoke goes out
      // completely
      // Perhaps this situation should be changed to a ConnectionLostException or something else?
      client.shutDown();
      // when the client shutdowns, this should wake up.
      // and an error should be thrown
      // give the thread a chance to execute
      t.join(200);
      synchronized (lock) {
        lock.notifyAll();
      }
      assertNotNull(rme.get());
    } finally {
      shutdownServerAndClient(server, client);
    }
  }
}


interface IFoo extends IRemote {
  void foo();
}


interface ITestRemote extends IRemote {
  int increment(int testVal);

  void testVoid();

  void throwException() throws Exception;
}


class TestRemote implements ITestRemote {
  public static final String EXCEPTION_STRING = "AND GO";
  private INode m_senderNode;

  @Override
  public int increment(final int testVal) {
    m_senderNode = MessageContext.getSender();
    return testVal + 1;
  }

  @Override
  public void testVoid() {
    m_senderNode = MessageContext.getSender();
  }

  @Override
  public void throwException() throws Exception {
    throw new Exception(EXCEPTION_STRING);
  }

  public INode getLastSenderNode() {
    return m_senderNode;
  }
}
