package games.strategy.engine.message.unifiedmessenger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.UnifiedMessengerHub;
import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.MessengerTestUtils;
import games.strategy.net.TestServerMessenger;
import games.strategy.util.Interruptibles;

public class ChannelMessengerTest {
  private IServerMessenger serverMessenger;
  private IMessenger clientMessenger;
  private int serverPort = -1;
  private ChannelMessenger serverChannelMessenger;
  private ChannelMessenger clientChannelMessenger;
  private UnifiedMessengerHub unifiedMessengerHub;

  @BeforeEach
  public void setUp() throws IOException {
    serverMessenger = new TestServerMessenger("Server", 0);
    serverMessenger.setAcceptNewConnections(true);
    serverPort = serverMessenger.getLocalNode().getSocketAddress().getPort();
    final String mac = MacFinder.getHashedMacAddress();
    clientMessenger = new ClientMessenger("localhost", serverPort, "client1", mac);
    final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(serverMessenger);
    unifiedMessengerHub = unifiedMessenger.getHub();
    serverChannelMessenger = new ChannelMessenger(unifiedMessenger);
    clientChannelMessenger = new ChannelMessenger(new UnifiedMessenger(clientMessenger));
  }

  @AfterEach
  public void tearDown() {
    MessengerTestUtils.shutDownQuietly(serverMessenger);
    MessengerTestUtils.shutDownQuietly(clientMessenger);
  }

  @Test
  public void testLocalCall() {
    final RemoteName descriptor = new RemoteName("testLocalCall", IChannelBase.class);
    serverChannelMessenger.registerChannelSubscriber(new ChannelSubscriber(), descriptor);
    final IChannelBase subscriber = (IChannelBase) serverChannelMessenger.getChannelBroadcastor(descriptor);
    subscriber.testNoParams();
    subscriber.testPrimitives(1, (short) 0, 1, (byte) 1, true, (float) 1.0);
    subscriber.testString("a");
  }

  @Test
  public void testRemoteCall() {
    final RemoteName testRemote = new RemoteName("testRemote", IChannelBase.class);
    final ChannelSubscriber subscriber1 = new ChannelSubscriber();
    serverChannelMessenger.registerChannelSubscriber(subscriber1, testRemote);
    assertHasChannel(testRemote, unifiedMessengerHub);
    final IChannelBase channelTest = (IChannelBase) clientChannelMessenger.getChannelBroadcastor(testRemote);
    channelTest.testNoParams();
    assertCallCountIs(subscriber1, 1);
    channelTest.testString("a");
    assertCallCountIs(subscriber1, 2);
    channelTest.testPrimitives(1, (short) 0, 1, (byte) 1, true, (float) 1.0);
    assertCallCountIs(subscriber1, 3);
    channelTest.testArray(null, null, null, null, null, null);
    assertCallCountIs(subscriber1, 4);
  }

  @Test
  public void testMultipleClients() throws Exception {
    // set up the client and server so that the client has 1 subscriber, and the server knows about it
    final RemoteName test = new RemoteName("test", IChannelBase.class);
    final ChannelSubscriber client1Subscriber = new ChannelSubscriber();
    clientChannelMessenger.registerChannelSubscriber(client1Subscriber, test);
    assertHasChannel(test, unifiedMessengerHub);
    assertEquals(1, clientChannelMessenger.getUnifiedMessenger().getLocalEndPointCount(test));
    // add a new client
    final String mac = MacFinder.getHashedMacAddress();
    final ClientMessenger clientMessenger2 = new ClientMessenger("localhost", serverPort, "client2", mac);
    final ChannelMessenger client2 = new ChannelMessenger(new UnifiedMessenger(clientMessenger2));
    ((IChannelBase) client2.getChannelBroadcastor(test)).testString("a");
    assertCallCountIs(client1Subscriber, 1);
  }

  @Test
  public void testMultipleChannels() {
    final RemoteName testRemote2 = new RemoteName("testRemote2", IChannelBase.class);
    final RemoteName testRemote3 = new RemoteName("testRemote3", IChannelBase.class);
    final ChannelSubscriber subscriber2 = new ChannelSubscriber();
    clientChannelMessenger.registerChannelSubscriber(subscriber2, testRemote2);
    final ChannelSubscriber subscriber3 = new ChannelSubscriber();
    clientChannelMessenger.registerChannelSubscriber(subscriber3, testRemote3);
    assertHasChannel(testRemote2, unifiedMessengerHub);
    assertHasChannel(testRemote3, unifiedMessengerHub);
    final IChannelBase channelTest2 = (IChannelBase) serverChannelMessenger.getChannelBroadcastor(testRemote2);
    channelTest2.testNoParams();
    assertCallCountIs(subscriber2, 1);
    final IChannelBase channelTest3 = (IChannelBase) serverChannelMessenger.getChannelBroadcastor(testRemote3);
    channelTest3.testNoParams();
    assertCallCountIs(subscriber3, 1);
  }

  private static void assertHasChannel(final RemoteName descriptor, final UnifiedMessengerHub hub) {
    int waitCount = 0;
    while (waitCount < 10 && !hub.hasImplementors(descriptor.getName())) {
      Interruptibles.sleep(100);
      waitCount++;
    }
    assertTrue(hub.hasImplementors(descriptor.getName()));
  }

  private static void assertCallCountIs(final ChannelSubscriber subscriber, final int expected) {
    // since the method call happens in a separate thread, wait for the call to go through, but don't wait too long
    int waitCount = 0;
    while (waitCount < 20 && expected != subscriber.getCallCount()) {
      Interruptibles.sleep(50);
      waitCount++;
    }
    assertEquals(expected, subscriber.getCallCount());
  }

  private interface IChannelBase extends IChannelSubscriber {
    void testNoParams();

    void testPrimitives(int a, short b, long c, byte d, boolean e, float f);

    void testString(String a);

    void testArray(int[] ints, short[] shorts, byte[] bytes, boolean[] bools, float[] floats, Object[] objects);
  }

  private static class ChannelSubscriber implements IChannelBase {
    private int callCount = 0;

    private synchronized void incrementCount() {
      callCount++;
    }

    public synchronized int getCallCount() {
      return callCount;
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
}
