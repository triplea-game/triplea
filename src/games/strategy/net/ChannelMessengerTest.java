/*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.net;

import games.strategy.engine.message.ChannelMessenger;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author Sean Bridges
 */
public class ChannelMessengerTest extends TestCase
{
	private IServerMessenger m_server;
	private IMessenger m_client1;
	
	private static int SERVER_PORT = 12022;
	
	private ChannelMessenger m_serverMessenger;
	private ChannelMessenger m_clientMessenger;
    
    public ChannelMessengerTest(String name)
    {
        super(name);
    }
    
	public void setUp() throws IOException
	{
        
		m_server = new ServerMessenger("Server", SERVER_PORT);
		m_server.setAcceptNewConnections(true);
		m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1");
		m_serverMessenger = new ChannelMessenger(m_server);
		m_clientMessenger = new ChannelMessenger(m_client1);
		
	}
	
	public void tearDown()
	{
		try
		{
			if(m_server != null)
				m_server.shutDown();
		} catch(Exception e)
		{
			e.printStackTrace();
		}

		try
		{
			if(m_client1 != null)
				m_client1.shutDown();
		} catch(Exception e)
		{
			e.printStackTrace();
		}

		
	}


	public void testCreateDestroy()
	{
	    m_serverMessenger.createChannel(IChannelTest.class, "channelTest");
	    m_serverMessenger.destroyChannel("channelTest");
	}
	
	public void testLocalCall()
	{
	    m_serverMessenger.createChannel(IChannelTest.class, "testLocalCall");
	    m_serverMessenger.registerChannelSubscriber(new ChannelSubscribor(), "testLocalCall");
	    IChannelTest subscribor = (IChannelTest) m_serverMessenger.getChannelBroadcastor("testLocalCall");
	    subscribor.testNoParams();
	    subscribor.testPrimitives(1,(short) 0, (long) 1, (byte) 1,true, (float) 1.0);
	    subscribor.testString("a");
	}
	
	public void testRemoteCall()
	{
	    m_serverMessenger.createChannel(IChannelTest.class, "testRemote");
	    
	    ChannelSubscribor subscribor1 = new ChannelSubscribor();
	    m_serverMessenger.registerChannelSubscriber(subscribor1, "testRemote" );
	    
	    int waitCount = 0;
	    while(waitCount < 10 &&  !m_clientMessenger.hasChannel("testRemote"))
	    {   
	        try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                //like, whatever man
                e.printStackTrace();
            }   
            waitCount++;
	    }
	    assertTrue(m_clientMessenger.hasChannel("testRemote"));
	    
	    IChannelTest channelTest = (IChannelTest) m_clientMessenger.getChannelBroadcastor("testRemote");
	    channelTest.testNoParams();
	    assertCallCountIs(subscribor1, 1);
	   
	    channelTest.testString("a");
	    assertCallCountIs(subscribor1, 2);
	    	    
	    channelTest.testPrimitives(1,(short) 0, (long) 1, (byte) 1,true, (float) 1.0);
	    assertCallCountIs(subscribor1, 3);

	    channelTest.testArray(null, null, null, null, null, null );
	    assertCallCountIs(subscribor1, 4);

	    
	}
	
	private void assertCallCountIs(ChannelSubscribor subscribor, int expected)
	{
	    //since the method call happens in a seperate thread,
	    //wait for the call to go through, but dont wait too long
	    int waitCount = 0;
	    while(waitCount < 20 && expected != subscribor.getCallCount())
	    {
	        try
            {
                Thread.sleep(50);
            } catch (InterruptedException e)
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
    
    public void testPrimitives(int a, short b, long c, byte d, boolean e,  float f)
    {
        incrementCount();   
    }
    
    public void testString(String a)
    {
        incrementCount(); 
    }
    
    public void testArray(int[] ints, short[] shorts, byte[] bytes, boolean[] bools, float[] floats, Object[] objects)
    {
        incrementCount();
    }

    
}
