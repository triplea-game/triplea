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
import junit.framework.TestCase;

/**
 * @author Sean Bridges
 */
public class RemoteMessengerTest extends TestCase
{

    private RemoteMessenger m_messenger;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        //simple set up for non networked testing
        DummyMessenger messenger = new DummyMessenger();
        MessageManager manager = new MessageManager(messenger);
        m_messenger = new RemoteMessenger(manager, messenger);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testRegisterUnregister()
    {
        TestRemote testRemote = new TestRemote();
        m_messenger.registerRemote(ITestRemote.class, testRemote, "test");
        assertTrue(m_messenger.hasRemote("test"));
        m_messenger.unregisterRemote("test");
        assertFalse(m_messenger.hasRemote("test"));
    }
    
    public void testMethodCall()
    {
        TestRemote testRemote = new TestRemote();
        m_messenger.registerRemote(ITestRemote.class, testRemote, "testMethodCall");
        ITestRemote remote = (ITestRemote) m_messenger.getRemote("testMethodCall");
        assertEquals(2, remote.increment(1));
    }
    
    public void testVoidMethodCall()
    {
        TestRemote testRemote = new TestRemote();
        m_messenger.registerRemote(ITestRemote.class, testRemote, "testVoidMethodCall");
        ITestRemote remote = (ITestRemote) m_messenger.getRemote("testVoidMethodCall");
        remote.testVoid();
    }    
    
    public void testException() throws Exception
    {
        TestRemote testRemote = new TestRemote();
        m_messenger.registerRemote(ITestRemote.class, testRemote, "testException");
        ITestRemote remote = (ITestRemote) m_messenger.getRemote("testException");
        try
        {
            remote.throwException();
        }
        catch(Exception e)
        {
            //this is what we want
            if(e.getMessage().equals(TestRemote.EXCEPTION_STRING))
                return;
            throw e;
        }
        fail("No exception thrown");
    }        
    
    
    public void testRemoteCall() throws Exception
    {
        ServerMessenger server = null;
        ClientMessenger client = null;
        try
        {
            server = new ServerMessenger("server", 12012);
            server.setAcceptNewConnections(true);
            
            client = new ClientMessenger("localhost", 12012, "client");
            
            MessageManager serverMM = new MessageManager(server);
            MessageManager clientMM = new MessageManager(client);
            
            RemoteMessenger serverRM = new RemoteMessenger(serverMM, server);
            RemoteMessenger clientRM = new RemoteMessenger(clientMM, client);
            
            //register it on the server
            serverRM.registerRemote(ITestRemote.class, new TestRemote(), "test");

            //since the registration must go over a socket
            //and through a couple threads, wait for the 
            //client to get it
            int waitCount = 0;
            while(!clientRM.hasRemote("test") && waitCount < 20)
            {
                waitCount++;
                Thread.sleep(50);
            }
            
            //call it on the client
            int rVal = ( (ITestRemote) clientRM.getRemote("test")).increment(1);
            assertEquals(2, rVal); 

        }
        finally
        {
            if(server!= null)
                server.shutDown();   
            if(client != null)
                client.shutDown();
        }
        
        
    }
}


interface ITestRemote extends IRemote
{
    public int increment(int testVal);
    public void testVoid();
    public void throwException() throws Exception;
}

class TestRemote implements ITestRemote
{
    public static final String EXCEPTION_STRING = "AND GO";
    
    public int increment(int testVal)
    {
        return testVal + 1;
    }
    
    public void testVoid()
    {
        
    }
    
    public void throwException() throws Exception
    {
        throw new Exception(EXCEPTION_STRING);
    }
}