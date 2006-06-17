package games.strategy.engine.chat;

import games.strategy.engine.message.*;
import games.strategy.net.*;

import java.io.IOException;
import java.util.*;

import junit.framework.*;

public class ChatTest extends TestCase
{

    private static int SERVER_PORT = 12072;

    private IServerMessenger m_server;
    private IMessenger m_client1;
    private IMessenger m_client2;

    
    UnifiedMessenger m_sum;
    RemoteMessenger m_srm;
    ChannelMessenger m_scm;
    

    UnifiedMessenger m_c1um;
    RemoteMessenger m_c1rm;
    ChannelMessenger m_c1cm;

    UnifiedMessenger m_c2um;
    RemoteMessenger m_c2rm;
    ChannelMessenger m_c2cm;
    
    TestChatListener m_serverChatListener;
    TestChatListener m_client1ChatListener;
    TestChatListener m_client2ChatListener;
 
    
    
    public void setUp() throws IOException
    {
        SERVER_PORT++;
        m_server = new ServerMessenger("Server", SERVER_PORT);
        m_server.setAcceptNewConnections(true);
        m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1");
        m_client2 = new ClientMessenger("localhost", SERVER_PORT, "client2");
 
        
        m_sum = new UnifiedMessenger(m_server);
        m_srm = new RemoteMessenger(m_sum);
        m_scm = new ChannelMessenger(m_sum);
        

        m_c1um = new UnifiedMessenger(m_client1);
        m_c1rm = new RemoteMessenger(m_c1um);
        m_c1cm = new ChannelMessenger(m_c1um);

        m_c2um = new UnifiedMessenger(m_client2);
        m_c2rm = new RemoteMessenger(m_c2um);
        m_c2cm = new ChannelMessenger(m_c2um);
        
        m_serverChatListener = new TestChatListener();
        m_client1ChatListener = new TestChatListener();
        m_client2ChatListener = new TestChatListener();
        

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

        try
        {
            if(m_client2 != null)
                m_client2.shutDown();
        } catch(Exception e)
        {
            e.printStackTrace();
        }
        
        
    }  
    
    
    
    public void testAll() throws Exception
    {
        //this is a rather big and ugly unit test
        //its just that the chat is so hard to set up
        //and we really need to test it working with sockets
        //rather than some mocked up implementation
        
        
        ChatController controller = new ChatController("c", m_server, m_srm, m_scm);
        
        flush();
        Thread.sleep(20);
        
        
        final Chat server = new Chat(m_server, "c", m_scm, m_srm);
        server.addChatListener(m_serverChatListener);
        server.init();
       
        final Chat client1 = new Chat(m_client1, "c", m_c1cm, m_c1rm);
        client1.addChatListener(m_client1ChatListener);
        client1.init();


        final Chat client2 = new Chat(m_client2, "c", m_c2cm, m_c2rm);
        client2.addChatListener(m_client2ChatListener);
        client2.init();

        flush();
        
        //we need to wait for all the messages to write
        for(int i =0; i < 10; i++)
        {
            try
            {
                assertEquals(m_client1ChatListener.m_players.size(), 3);
                assertEquals(m_client2ChatListener.m_players.size(), 3); 
                assertEquals(m_serverChatListener.m_players.size(), 3);
                break;
            } catch(AssertionFailedError afe)
            {
                Thread.sleep(25);
            }           
        }
        
        
        assertEquals(m_client1ChatListener.m_players.size(), 3);
        assertEquals(m_client2ChatListener.m_players.size(), 3); 
        assertEquals(m_serverChatListener.m_players.size(), 3);
       
        //send 50 messages, each client sending messages on a different thread.
        
        final int messageCount = 50;
        
        Runnable client2Send = new Runnable()
        {
        
            public void run()
            {
                for(int i =0; i <messageCount; i++)
                {
                    client2.sendMessage("Test", false);
                }                
            }
        
        };
        Thread clientThread = new Thread(client2Send);
        clientThread.start();
        
        Runnable serverSend = new Runnable()
        {
        
            public void run()
            {
                for(int i =0; i <messageCount; i++)
                {
                    server.sendMessage("Test", false);
                }
            }
        };
        Thread serverThread = new Thread(serverSend);
        serverThread.start();
        

        
       
        for(int i =0; i <messageCount; i++)
        {
            client1.sendMessage("Test", false);
        }
        
        serverThread.join();
        clientThread.join();
        
        
        flush();
        
        //we need to wait for all the messages to write
        for(int i =0; i < 10; i++)
        {
            try
            {
                assertEquals(m_client1ChatListener.m_messages.size(), 3 * messageCount);
                assertEquals(m_client2ChatListener.m_messages.size(), 3 * messageCount);
                assertEquals(m_serverChatListener.m_messages.size(), 3 * messageCount);
                break;
            } catch(AssertionFailedError afe)
            {
                Thread.sleep(25);
            }           
        }        
        
        assertEquals(m_client1ChatListener.m_messages.size(), 3 * messageCount);
        assertEquals(m_client2ChatListener.m_messages.size(), 3 * messageCount);
        assertEquals(m_serverChatListener.m_messages.size(), 3 * messageCount);
        
        
        client1.shutdown();
        client2.shutdown();
        
        flush();
        
        //we need to wait for all the messages to write
        for(int i =0; i < 10; i++)
        {
            try
            {
                assertEquals(m_serverChatListener.m_players.size(), 1);
                break;
            } catch(AssertionFailedError afe)
            {
                Thread.sleep(25);
            }           
        }  
        assertEquals(m_serverChatListener.m_players.size(), 1);
        
        controller.deactivate();
        
        for(int i =0; i < 10; i++)
        {
            try
            {
                assertEquals(m_serverChatListener.m_players.size(), 0);
                break;
            } catch(AssertionFailedError afe)
            {
                Thread.sleep(25);
            }           
        }  
        assertEquals(m_serverChatListener.m_players.size(), 0);

        
    
    }

    private void flush()
    {
       
        //this doesnt really flush
        //but it does something
        for(int i = 0; i < 5; i++)
        {
            m_server.flush();
            m_client1.flush();
            m_client2.flush();
    
            m_sum.waitForAllJobs();
            m_c1um.waitForAllJobs();
            m_c2um.waitForAllJobs();
            
            Thread.yield();
        }
    }
  
}



class TestChatListener implements IChatListener
{
    
    public List<String> m_players;
    public List<String> m_messages = new ArrayList<String>();
    public List<Boolean> m_thirdPerson = new ArrayList<Boolean>();
    public List<String> m_from = new ArrayList<String>();
    
    
    public void updatePlayerList(Collection<String> players)
    {
        synchronized(this)
        {
            m_players = new ArrayList<String>(players);
        }
        
    }

    public void addMessage(String message, String from, boolean thirdperson)
    {    
        synchronized(this)
        {
            m_messages.add(message);
            m_thirdPerson.add(thirdperson);
            m_from.add(from);
        }
    }
    
}



