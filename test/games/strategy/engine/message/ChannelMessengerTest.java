/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.message;

import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.ServerMessenger;
import games.strategy.test.TestUtil;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author Sean Bridges
 */
public class ChannelMessengerTest extends TestCase
{
	private IServerMessenger m_server;
	private IMessenger m_client1;
	private static int SERVER_PORT = -1;
	private ChannelMessenger m_serverMessenger;
	private ChannelMessenger m_clientMessenger;
	private UnifiedMessengerHub m_hub;
	
	public ChannelMessengerTest(final String name)
	{
		super(name);
	}
	
	@Override
	public void setUp() throws IOException
	{
		SERVER_PORT = TestUtil.getUniquePort();
		m_server = new ServerMessenger("Server", SERVER_PORT);
		m_server.setAcceptNewConnections(true);
		final String mac = MacFinder.GetHashedMacAddress();
		m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1", mac);
		final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(m_server);
		m_hub = unifiedMessenger.getHub();
		m_serverMessenger = new ChannelMessenger(unifiedMessenger);
		m_clientMessenger = new ChannelMessenger(new UnifiedMessenger(m_client1));
	}
	
	@Override
	public void tearDown()
	{
		try
		{
			if (m_server != null)
				m_server.shutDown();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			if (m_client1 != null)
				m_client1.shutDown();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void testLocalCall()
	{
		final RemoteName descriptor = new RemoteName(IChannelTest.class, "testLocalCall");
		m_serverMessenger.registerChannelSubscriber(new ChannelSubscribor(), descriptor);
		final IChannelTest subscribor = (IChannelTest) m_serverMessenger.getChannelBroadcastor(descriptor);
		subscribor.testNoParams();
		subscribor.testPrimitives(1, (short) 0, 1, (byte) 1, true, (float) 1.0);
		subscribor.testString("a");
	}
	
	public void testRemoteCall()
	{
		final RemoteName testRemote = new RemoteName(IChannelTest.class, "testRemote");
		final ChannelSubscribor subscribor1 = new ChannelSubscribor();
		m_serverMessenger.registerChannelSubscriber(subscribor1, testRemote);
		assertHasChannel(testRemote, m_hub);
		final IChannelTest channelTest = (IChannelTest) m_clientMessenger.getChannelBroadcastor(testRemote);
		channelTest.testNoParams();
		assertCallCountIs(subscribor1, 1);
		channelTest.testString("a");
		assertCallCountIs(subscribor1, 2);
		channelTest.testPrimitives(1, (short) 0, 1, (byte) 1, true, (float) 1.0);
		assertCallCountIs(subscribor1, 3);
		channelTest.testArray(null, null, null, null, null, null);
		assertCallCountIs(subscribor1, 4);
	}
	
	/**
	 * @param channelName
	 */
	private void assertHasChannel(final RemoteName descriptor, final UnifiedMessengerHub hub)
	{
		int waitCount = 0;
		while (waitCount < 10 && !hub.hasImplementors(descriptor.getName()))
		{
			try
			{
				Thread.sleep(100);
			} catch (final InterruptedException e)
			{
				// like, whatever man
				e.printStackTrace();
			}
			waitCount++;
		}
		assertTrue(hub.hasImplementors(descriptor.getName()));
	}
	
	public void testMultipleClients() throws Exception
	{
		// set up the client and server
		// so that the client has 1 subscribor, and the server knows about it
		final RemoteName test = new RemoteName(IChannelTest.class, "test");
		final ChannelSubscribor client1Subscribor = new ChannelSubscribor();
		m_clientMessenger.registerChannelSubscriber(client1Subscribor, test);
		assertHasChannel(test, m_hub);
		assertEquals(1, m_clientMessenger.getUnifiedMessenger().getLocalEndPointCount(test));
		// add a new client
		final String mac = MacFinder.GetHashedMacAddress();
		final ClientMessenger clientMessenger2 = new ClientMessenger("localhost", SERVER_PORT, "client2", mac);
		final ChannelMessenger client2 = new ChannelMessenger(new UnifiedMessenger(clientMessenger2));
		((IChannelTest) client2.getChannelBroadcastor(test)).testString("a");
		assertCallCountIs(client1Subscribor, 1);
	}
	
	public void testMultipleChannels()
	{
		final RemoteName testRemote2 = new RemoteName(IChannelTest.class, "testRemote2");
		final RemoteName testRemote3 = new RemoteName(IChannelTest.class, "testRemote3");
		final ChannelSubscribor subscribor2 = new ChannelSubscribor();
		m_clientMessenger.registerChannelSubscriber(subscribor2, testRemote2);
		final ChannelSubscribor subscribor3 = new ChannelSubscribor();
		m_clientMessenger.registerChannelSubscriber(subscribor3, testRemote3);
		assertHasChannel(testRemote2, m_hub);
		assertHasChannel(testRemote3, m_hub);
		final IChannelTest channelTest2 = (IChannelTest) m_serverMessenger.getChannelBroadcastor(testRemote2);
		channelTest2.testNoParams();
		assertCallCountIs(subscribor2, 1);
		final IChannelTest channelTest3 = (IChannelTest) m_serverMessenger.getChannelBroadcastor(testRemote3);
		channelTest3.testNoParams();
		assertCallCountIs(subscribor3, 1);
	}
	
	private void assertCallCountIs(final ChannelSubscribor subscribor, final int expected)
	{
		// since the method call happens in a seperate thread,
		// wait for the call to go through, but dont wait too long
		int waitCount = 0;
		while (waitCount < 20 && expected != subscribor.getCallCount())
		{
			try
			{
				Thread.sleep(50);
			} catch (final InterruptedException e)
			{
				e.printStackTrace();
			}
			waitCount++;
		}
		assertEquals(expected, subscribor.getCallCount());
	}
}


interface IChannelTest extends IChannelSubscribor
{
	public void testNoParams();
	
	public void testPrimitives(int a, short b, long c, byte d, boolean e, float f);
	
	public void testString(String a);
	
	public void testArray(int[] ints, short[] shorts, byte[] bytes, boolean[] bools, float[] floats, Object[] objects);
}


class ChannelSubscribor implements IChannelTest
{
	private int m_callCount = 0;
	
	private synchronized void incrementCount()
	{
		m_callCount++;
	}
	
	public synchronized int getCallCount()
	{
		return m_callCount;
	}
	
	public void testNoParams()
	{
		incrementCount();
	}
	
	public void testPrimitives(final int a, final short b, final long c, final byte d, final boolean e, final float f)
	{
		incrementCount();
	}
	
	public void testString(final String a)
	{
		incrementCount();
	}
	
	public void testArray(final int[] ints, final short[] shorts, final byte[] bytes, final boolean[] bools, final float[] floats, final Object[] objects)
	{
		incrementCount();
	}
}
