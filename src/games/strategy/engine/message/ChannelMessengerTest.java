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

package games.strategy.engine.message;

import games.strategy.net.*;

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
        SERVER_PORT++;
		m_server = new ServerMessenger("Server", SERVER_PORT);
		m_server.setAcceptNewConnections(true);
		m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1");
		m_serverMessenger = new ChannelMessenger( new UnifiedMessenger(m_server));
		m_clientMessenger = new ChannelMessenger(new UnifiedMessenger(m_client1));
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
	    
	    
	    assertHasChannel("testRemote", m_clientMessenger);
	    assertTrue(m_clientMessenger.hasSubscribors("testRemote"));
	    
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

	/**
     * @param channelName
     */
    private void assertHasSubscribor(String channelName, ChannelMessenger channel)
    {
        int waitCount = 0;
	    while(waitCount < 10 &&  !channel.hasSubscribors(channelName))
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
	    assertTrue(channel.hasSubscribors(channelName));
	    
    }
	
	/**
     * @param channelName
     */
    private void assertHasChannel(String channelName, ChannelMessenger channel)
    {
        int waitCount = 0;
	    while(waitCount < 10 &&  !channel.hasChannel(channelName))
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
	    assertTrue(channel.hasChannel(channelName));
	    
    }
    
    public void testMultipleClients() throws Exception
    {
        
        //set up the client and server
        //so that the client has 1 subscribor, and the server knows about it
        m_serverMessenger.createChannel(IChannelTest.class, "test");
        
        ChannelSubscribor client1Subscriboror = new ChannelSubscribor();
       
        assertHasChannel("test", m_clientMessenger);
        m_clientMessenger.registerChannelSubscriber(client1Subscriboror, "test");
        
        assertHasSubscribor("test", m_serverMessenger);
        
        //add a new client
        ClientMessenger clientMessenger2 = new ClientMessenger("localhost", SERVER_PORT, "client2");
        
        ChannelMessenger client2 = new ChannelMessenger(new UnifiedMessenger(clientMessenger2));
        assertHasChannel("test", client2);
        
        assertHasSubscribor("test", client2);
        
        
        ((IChannelTest) client2.getChannelBroadcastor("test")).testString("a");
         
         assertCallCountIs(client1Subscriboror, 1);
    }

    public void testMultipleChannels()
	{
	    m_serverMessenger.createChannel(IChannelTest.class, "testRemote");
	    m_serverMessenger.createChannel(IChannelTest.class, "testRemote2");
	    m_serverMessenger.createChannel(IChannelTest.class, "testRemote3");
	    
	    assertHasChannel("testRemote", m_clientMessenger);
	    assertHasChannel("testRemote2", m_clientMessenger);
	    assertHasChannel("testRemote3", m_clientMessenger);
	   

	    ChannelSubscribor subscribor2 = new ChannelSubscribor();
	    m_clientMessenger.registerChannelSubscriber(subscribor2, "testRemote2");

	    ChannelSubscribor subscribor3 = new ChannelSubscribor();
	    m_clientMessenger.registerChannelSubscriber(subscribor3, "testRemote3");
	    
	    assertHasSubscribor("testRemote2",m_serverMessenger);
	    assertHasSubscribor("testRemote3",m_serverMessenger);
	    
	    IChannelTest channelTest2 = (IChannelTest) m_serverMessenger.getChannelBroadcastor("testRemote2");
	    channelTest2.testNoParams();
	    assertCallCountIs(subscribor2, 1);

	    IChannelTest channelTest3 = (IChannelTest) m_serverMessenger.getChannelBroadcastor("testRemote3");
	    channelTest3.testNoParams();
	    assertCallCountIs(subscribor3, 1);
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
 
	   public void testInit() throws Exception
	    {
	        m_serverMessenger.createChannel(IChannelTest.class, "testInit");
	        
	       
	        ClientMessenger client2 = new ClientMessenger("localhost", SERVER_PORT, "client2");
	        try
	        {
	            ChannelMessenger client2Messenger = new ChannelMessenger(new UnifiedMessenger(client2));	            
	            assertTrue (client2Messenger.hasChannel("testInit"));

	        }
	        finally
	        {
	            client2.shutDown();
	        }
	        
	        
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
