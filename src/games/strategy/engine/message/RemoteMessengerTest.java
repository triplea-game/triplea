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

import java.util.concurrent.atomic.*;

import games.strategy.net.*;
import junit.framework.TestCase;

/**
 * @author Sean Bridges
 */
public class RemoteMessengerTest extends TestCase
{

    private IMessenger m_messenger;
    private RemoteMessenger m_remoteMessenger;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        //simple set up for non networked testing
        m_messenger = new DummyMessenger();
        m_remoteMessenger = new RemoteMessenger(new UnifiedMessenger(m_messenger));
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        m_messenger = null;
        m_remoteMessenger = null;
    }

    public void testRegisterUnregister()
    {
        TestRemote testRemote = new TestRemote();
        m_remoteMessenger.registerRemote(ITestRemote.class, testRemote, "test");
        assertTrue(m_remoteMessenger.hasRemote("test"));
        m_remoteMessenger.unregisterRemote("test");
        assertFalse(m_remoteMessenger.hasRemote("test"));
    }
    
    public void testMethodCall()
    {
        TestRemote testRemote = new TestRemote();
        m_remoteMessenger.registerRemote(ITestRemote.class, testRemote, "testMethodCall");
        ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote("testMethodCall");
        assertEquals(2, remote.increment(1));
        assertEquals(testRemote.getLastSenderNode(), m_messenger.getLocalNode());
    }
    
    public void testExceptionThrownWhenUnregisteredRemote()
    {
        TestRemote testRemote = new TestRemote();
        m_remoteMessenger.registerRemote(ITestRemote.class, testRemote, "testMethodCall");
        ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote("testMethodCall");
        
        m_remoteMessenger.unregisterRemote("testMethodCall");
        
        try
        {
            remote.increment(1);
            fail("No exception thrown");
        }
        catch(RemoteNotFoundException rme)
        {
            //this is what we expect
        }
        
    }    
    
    public void testNoRemote()
    {
        try
        {
            m_remoteMessenger.getRemote("testMethodCall");
            fail("No exception thrown");
        }
        catch(RemoteNotFoundException rme)
        {
            //this is what we expect
        }

    }
    
    public void testVoidMethodCall()
    {
        TestRemote testRemote = new TestRemote();
        m_remoteMessenger.registerRemote(ITestRemote.class, testRemote, "testVoidMethodCall");
        ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote("testVoidMethodCall");
        remote.testVoid();
    }    
    
    public void testException() throws Exception
    {
        TestRemote testRemote = new TestRemote();
        m_remoteMessenger.registerRemote(ITestRemote.class, testRemote, "testException");
        ITestRemote remote = (ITestRemote) m_remoteMessenger.getRemote("testException");
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
            server = new ServerMessenger("server", 12024);
            server.setAcceptNewConnections(true);
            
            client = new ClientMessenger("localhost", 12024, "client");
            
            
            RemoteMessenger serverRM = new RemoteMessenger(new UnifiedMessenger( server));
            RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger( client));
            
            //register it on the server
            TestRemote testRemote = new TestRemote();
            serverRM.registerRemote(ITestRemote.class, testRemote, "test");

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
            assertEquals(testRemote.getLastSenderNode(), client.getLocalNode());

        }
        finally
        {
            if(server!= null)
                server.shutDown();   
            if(client != null)
                client.shutDown();
        }
        
        
    }
    
    public void testRemoteCall2() throws Exception
    {
        ServerMessenger server = null;
        ClientMessenger client = null;
        try
        {
            server = new ServerMessenger("server", 12025);
            server.setAcceptNewConnections(true);
            
            client = new ClientMessenger("localhost", 12025, "client");
           
            RemoteMessenger serverRM = new RemoteMessenger(new UnifiedMessenger( server));
            TestRemote testRemote = new TestRemote();
            serverRM.registerRemote(ITestRemote.class, testRemote, "test");
            
            RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger( client));
            
            //call it on the client
            //should be no need to wait since the constructor should not
            //reutrn until the initial state of the messenger is good
            int rVal = ( (ITestRemote) clientRM.getRemote("test")).increment(1);
            assertEquals(2, rVal); 
            assertEquals(testRemote.getLastSenderNode(), client.getLocalNode());

        }
        finally
        {
            if(server!= null)
                server.shutDown();   
            if(client != null)
                client.shutDown();
        }
        
        
    }
    
    
    public void testShutDownServer() throws Exception
    {
       
        //when the server shutdown, remotes created
        //on the server should not be visible on clients
        
        ServerMessenger server = null;
        ClientMessenger client = null;
        int port = 12026;
        try
        {
            server = new ServerMessenger("server", port);
            server.setAcceptNewConnections(true);
            
            client = new ClientMessenger("localhost", port, "client");
           
            RemoteMessenger serverRM = new RemoteMessenger(new UnifiedMessenger( server));
            serverRM.registerRemote(ITestRemote.class, new TestRemote(), "test");
            
            RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger( client));

            clientRM.waitForRemote("test", 200);
            assertTrue(clientRM.hasRemote("test"));
            
            server.shutDown();
            sleep(100);            
            
            assertFalse(clientRM.hasRemote("test"));
            

        }
        finally
        {
            if(server!= null)
                server.shutDown();   
            if(client != null)
                client.shutDown();
        }
    }
    
    private void sleep(int ms)
    {
        try
        {
            Thread.sleep(ms);
        } catch (InterruptedException e)
        {
        }
    }
    
    public void testShutDownClient() throws Exception
    {
       
        //when the client shutdown, remotes created
        //on the client should not be visible on server

        
        ServerMessenger server = null;
        ClientMessenger client = null;
        int port = 12026;
        try
        {
            server = new ServerMessenger("server", port);
            server.setAcceptNewConnections(true);
            
            client = new ClientMessenger("localhost", port, "client");
           
            RemoteMessenger serverRM = new RemoteMessenger(new UnifiedMessenger( server));
            
            
            RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger( client));
            clientRM.registerRemote(ITestRemote.class, new TestRemote(), "test");
            
            serverRM.waitForRemote("test", 200);
            assertTrue(serverRM.hasRemote("test"));
                       
            client.shutDown();
            sleep(100);
            
            assertFalse(serverRM.hasRemote("test"));
            

        }
        finally
        {
            if(server!= null)
                server.shutDown();   
            if(client != null)
                client.shutDown();
        }
    }    
    
    
    public void testMethodReturnsOnWait() throws Exception
    {
       
        //when the client shutdown, remotes created
        //on the client should not be visible on server

        ServerMessenger server = null;
        ClientMessenger client = null;
        int port = 12029;
        try
        {
            server = new ServerMessenger("server", port);
            server.setAcceptNewConnections(true);
            
            client = new ClientMessenger("localhost", port, "client");
           
            final RemoteMessenger serverRM = new RemoteMessenger(new UnifiedMessenger( server));            
            RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger( client));
            
            final Object lock = new Object();
            
            IFoo foo = new IFoo()
            {
                public void foo() 
                {
                    synchronized(lock)
                    {
                        try
                        {
                            lock.wait();
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            };
            
            clientRM.registerRemote(IFoo.class, foo, "test");
            
            serverRM.waitForRemote("test", 200);
            assertTrue(serverRM.hasRemote("test"));
                     
            final AtomicReference<ConnectionLostException> rme = new AtomicReference<ConnectionLostException>(null);
            final AtomicBoolean started = new AtomicBoolean(false);   
            
            Runnable r = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        IFoo remoteFoo = (IFoo) serverRM.getRemote("test");
                        started.set(true);
                        remoteFoo.foo();
                    }
                    catch(ConnectionLostException e)
                    {
                        rme.set(e);
                    }
            
                }
            };

            Thread t = new Thread(r);
            t.start();
            
            //wait for the thread to start
            while(started.get() == false)
                sleep(1);
            
            sleep(20);
           
            
            client.shutDown();
            
            
            //when the client shutdowns, this should wake up.
            //and an error should be thrown
            //give the thread a chance to execute
            t.join(200);
            
            synchronized(lock)
            {
                lock.notifyAll();
            }
            
            assertNotNull(rme.get());
        }
        finally
        {
            if(server!= null)
                server.shutDown();   
            if(client != null)
                client.shutDown();
        }
    }    
    
    
    public void testShutDownWithTwoClients() throws Exception
    {
       
        //three nodes, one server and 2 clients
        //make sure that when client1 shutdown, remotes
        //created on client1 are not visible anymore

        
        ServerMessenger server = null;
        ClientMessenger client = null;
        ClientMessenger client2 = null;
        int port = 12027;
        try
        {
            server = new ServerMessenger("server", port);
            server.setAcceptNewConnections(true);
            
            client = new ClientMessenger("localhost", port, "client1");
            client2 = new ClientMessenger("localhost", port, "client2");
           
            RemoteMessenger serverRM = new RemoteMessenger(new UnifiedMessenger( server));
            RemoteMessenger clientRM = new RemoteMessenger(new UnifiedMessenger( client));
            RemoteMessenger client2RM = new RemoteMessenger(new UnifiedMessenger( client2));
            
            
            clientRM.registerRemote(ITestRemote.class, new TestRemote(), "test");
            
            serverRM.waitForRemote("test", 200);
            client2RM.waitForRemote("test", 200);
            assertTrue(serverRM.hasRemote("test"));
            assertTrue(client2RM.hasRemote("test"));
                       
            
            client.shutDown();
            sleep(100);
            
            assertFalse(serverRM.hasRemote("test"));
            assertFalse(client2RM.hasRemote("test"));
            

        }
        finally
        {
            if(server!= null)
                server.shutDown();   
            if(client != null)
                client.shutDown();
            if(client2 != null)
                client.shutDown();            
        }
    }        
    
    
}


interface IFoo extends IRemote
{
    public void foo();
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
    
    private INode m_senderNode;
    
    public int increment(int testVal)
    {
        m_senderNode = MessageContext.getSender();
        return testVal + 1;
    }
    
    public void testVoid()
    {
        m_senderNode = MessageContext.getSender();
    }
    
    public void throwException() throws Exception
    {
        throw new Exception(EXCEPTION_STRING);
    }
    
    public INode getLastSenderNode()
    {
        return m_senderNode;
    }
}