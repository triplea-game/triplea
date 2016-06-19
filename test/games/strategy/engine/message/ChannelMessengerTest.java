package games.strategy.engine.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import games.strategy.debug.ClientLogger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.ServerMessenger;
import games.strategy.test.TestUtil;
import games.strategy.util.ThreadUtil;

public class ChannelMessengerTest {
  private IServerMessenger m_server;
  private IMessenger m_client1;
  private static int SERVER_PORT = -1;
  private ChannelMessenger m_serverMessenger;
  private ChannelMessenger m_clientMessenger;
  private UnifiedMessengerHub m_hub;

  @Before
  public void setUp() throws IOException {
    SERVER_PORT = TestUtil.getUniquePort();
    m_server = new ServerMessenger("Server", SERVER_PORT);
    m_server.setAcceptNewConnections(true);
    final String mac = MacFinder.getHashedMacAddress();
    m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1", mac);
    final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(m_server);
    m_hub = unifiedMessenger.getHub();
    m_serverMessenger = new ChannelMessenger(unifiedMessenger);
    m_clientMessenger = new ChannelMessenger(new UnifiedMessenger(m_client1));
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
  }

  @Test
  public void testLocalCall() {
    final RemoteName descriptor = new RemoteName(IChannelBase.class, "testLocalCall");
    m_serverMessenger.registerChannelSubscriber(new ChannelSubscribor(), descriptor);
    final IChannelBase subscribor = (IChannelBase) m_serverMessenger.getChannelBroadcastor(descriptor);
    subscribor.testNoParams();
    subscribor.testPrimitives(1, (short) 0, 1, (byte) 1, true, (float) 1.0);
    subscribor.testString("a");
  }

  @Test
  public void testRemoteCall() {
    final RemoteName testRemote = new RemoteName(IChannelBase.class, "testRemote");
    final ChannelSubscribor subscribor1 = new ChannelSubscribor();
    m_serverMessenger.registerChannelSubscriber(subscribor1, testRemote);
    assertHasChannel(testRemote, m_hub);
    final IChannelBase channelTest = (IChannelBase) m_clientMessenger.getChannelBroadcastor(testRemote);
    channelTest.testNoParams();
    assertCallCountIs(subscribor1, 1);
    channelTest.testString("a");
    assertCallCountIs(subscribor1, 2);
    channelTest.testPrimitives(1, (short) 0, 1, (byte) 1, true, (float) 1.0);
    assertCallCountIs(subscribor1, 3);
    channelTest.testArray(null, null, null, null, null, null);
    assertCallCountIs(subscribor1, 4);
  }

  @Test
  public void testMultipleClients() throws Exception {
    // set up the client and server
    // so that the client has 1 subscribor, and the server knows about it
    final RemoteName test = new RemoteName(IChannelBase.class, "test");
    final ChannelSubscribor client1Subscribor = new ChannelSubscribor();
    m_clientMessenger.registerChannelSubscriber(client1Subscribor, test);
    assertHasChannel(test, m_hub);
    assertEquals(1, m_clientMessenger.getUnifiedMessenger().getLocalEndPointCount(test));
    // add a new client
    final String mac = MacFinder.getHashedMacAddress();
    final ClientMessenger clientMessenger2 = new ClientMessenger("localhost", SERVER_PORT, "client2", mac);
    final ChannelMessenger client2 = new ChannelMessenger(new UnifiedMessenger(clientMessenger2));
    ((IChannelBase) client2.getChannelBroadcastor(test)).testString("a");
    assertCallCountIs(client1Subscribor, 1);
  }

  @Test
  public void testMultipleChannels() {
    final RemoteName testRemote2 = new RemoteName(IChannelBase.class, "testRemote2");
    final RemoteName testRemote3 = new RemoteName(IChannelBase.class, "testRemote3");
    final ChannelSubscribor subscribor2 = new ChannelSubscribor();
    m_clientMessenger.registerChannelSubscriber(subscribor2, testRemote2);
    final ChannelSubscribor subscribor3 = new ChannelSubscribor();
    m_clientMessenger.registerChannelSubscriber(subscribor3, testRemote3);
    assertHasChannel(testRemote2, m_hub);
    assertHasChannel(testRemote3, m_hub);
    final IChannelBase channelTest2 = (IChannelBase) m_serverMessenger.getChannelBroadcastor(testRemote2);
    channelTest2.testNoParams();
    assertCallCountIs(subscribor2, 1);
    final IChannelBase channelTest3 = (IChannelBase) m_serverMessenger.getChannelBroadcastor(testRemote3);
    channelTest3.testNoParams();
    assertCallCountIs(subscribor3, 1);
  }

  private void assertHasChannel(final RemoteName descriptor, final UnifiedMessengerHub hub) {
    int waitCount = 0;
    while (waitCount < 10 && !hub.hasImplementors(descriptor.getName())) {
      ThreadUtil.sleep(100);
      waitCount++;
    }
    assertTrue(hub.hasImplementors(descriptor.getName()));
  }

  private static void assertCallCountIs(final ChannelSubscribor subscribor, final int expected) {
    // since the method call happens in a seperate thread,
    // wait for the call to go through, but dont wait too long
    int waitCount = 0;
    while (waitCount < 20 && expected != subscribor.getCallCount()) {
      ThreadUtil.sleep(50);
      waitCount++;
    }
    assertEquals(expected, subscribor.getCallCount());
  }
}


interface IChannelBase extends IChannelSubscribor {
  void testNoParams();

  void testPrimitives(int a, short b, long c, byte d, boolean e, float f);

  void testString(String a);

  void testArray(int[] ints, short[] shorts, byte[] bytes, boolean[] bools, float[] floats, Object[] objects);
}


class ChannelSubscribor implements IChannelBase {
  private int m_callCount = 0;

  private synchronized void incrementCount() {
    m_callCount++;
  }

  public synchronized int getCallCount() {
    return m_callCount;
  }

  @Override
  public void testNoParams() {
    incrementCount();
  }

  @Override
  public void testPrimitives(final int a, final short b, final long c, final byte d, final boolean e, final float f) {
    incrementCount();
  }

  @Override
  public void testString(final String a) {
    incrementCount();
  }

  @Override
  public void testArray(final int[] ints, final short[] shorts, final byte[] bytes, final boolean[] bools,
      final float[] floats, final Object[] objects) {
    incrementCount();
  }
}
