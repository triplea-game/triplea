/*
 * MangerTest.java
 *
 * Created on December 27, 2001, 12:24 PM
 */

package games.strategy.net;

import junit.framework.*;
import java.util.*;
import java.io.*;



/**
 *
 * @author  Sean Bridges
 */
public class MessengerTest extends TestCase
{
	private static int SERVER_PORT = 12021;
	
	private IServerMessenger m_server;
	private IMessenger m_client1;
	private IMessenger m_client2;
	
	private MessageListener m_serverListener = new MessageListener("server");
	private MessageListener m_client1Listener = new MessageListener("client1");
	private MessageListener m_client2Listener = new MessageListener("client2");
	
	
	public static Test suite()
	{
		TestSuite suite = new junit.extensions.ActiveTestSuite();
		suite.addTestSuite(MessengerTest.class);
		return suite;
	}

	
	/** Creates a new instance of MangerTest */
    public MessengerTest(String name) 
	{
		super(name);
    }
	
	public void setUp() throws IOException
	{
	
		m_server = new ServerMessenger("Server", SERVER_PORT);
		m_server.setAcceptNewConnections(true);
		m_server.addMessageListener(m_serverListener);
		
		m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1");
		m_client1.addMessageListener(m_client1Listener);
		
		m_client2 = new ClientMessenger("localhost", SERVER_PORT, "client2");
		m_client2.addMessageListener(m_client2Listener);
		
		
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
	
	public void testServerSend()
	{
		String message = "Hello";
		
		m_server.send(message, m_client1.getLocalNode());
		
		assertEquals(m_client1Listener.getLastMessage(), message);
		assertEquals(m_client1Listener.getLastSender(), m_server.getLocalNode());
		
		assertEquals(m_client2Listener.getMessageCount(), 0);
	}
	
	public void testServerSendToClient2()
	{
		String message = "Hello";
		
		m_server.send(message, m_client2.getLocalNode());
		
		assertEquals(m_client2Listener.getLastMessage(), message);
		assertEquals(m_client2Listener.getLastSender(), m_server.getLocalNode());
		
		assertEquals(m_client1Listener.getMessageCount(), 0);
	}

	
	public void testClientSendToServer()
	{
		String message = "Hello";
		
		m_client1.send(message, m_server.getLocalNode());
		
		assertEquals(m_serverListener.getLastMessage(), message);
		assertEquals(m_serverListener.getLastSender(), m_client1.getLocalNode());
		
		assertEquals(m_client1Listener.getMessageCount(), 0);
		assertEquals(m_client2Listener.getMessageCount(), 0);
	}

	public void testClientSendToClient()
	{
		String message = "Hello";
		
		m_client1.send(message, m_client2.getLocalNode());
		
		assertEquals(m_client2Listener.getLastMessage(), message);
		assertEquals(m_client2Listener.getLastSender(), m_client1.getLocalNode());
		
		assertEquals(m_client1Listener.getMessageCount(), 0);
		assertEquals(m_serverListener.getMessageCount(), 0);
	}

	
	public void testServerBroadcast()
	{
		String message = "Hello";
		
		m_server.broadcast(message);
		
		assertEquals(m_client1Listener.getLastMessage(), message);
		assertEquals(m_client1Listener.getLastSender(), m_server.getLocalNode());
		
		assertEquals(m_client2Listener.getLastMessage(), message);
		assertEquals(m_client2Listener.getLastSender(), m_server.getLocalNode());
		
		assertEquals(m_serverListener.getMessageCount(), 0);
	}
	
	public void testClientBroadcast()
	{
		String message = "Hello";
		
		m_client1.broadcast(message);
		
		assertEquals(m_client2Listener.getLastMessage(), message);
		assertEquals(m_client2Listener.getLastSender(), m_client1.getLocalNode());
		
		assertEquals(m_serverListener.getLastMessage(), message);
		assertEquals(m_serverListener.getLastSender(), m_client1.getLocalNode());
		
		assertEquals(m_client1Listener.getMessageCount(), 0);
	}

	public void testMultipleServer()
	{
		for(int i = 0; i < 100; i++)
		{
			m_server.send(new Integer(i), m_client1.getLocalNode());
		}
		
		for(int i = 0; i < 100; i++)
		{
			m_client1Listener.clearLastMessage();
		}
	}
	
	public void testMultipleClientToClient()
	{
		for(int i = 0; i < 100; i++)
		{
			m_client1.send(new Integer(i), m_client2.getLocalNode());
		}
		
		for(int i = 0; i < 100; i++)
		{
			m_client2Listener.clearLastMessage();
		}
	}

	
	
	public void testMultipleMessages() throws Exception
	{
		Thread t1 = new Thread( new MultipleMessageSender(m_server));
		Thread t2 = new Thread( new MultipleMessageSender(m_client1));
		Thread t3 = new Thread( new MultipleMessageSender(m_client2));
		
		t1.start();
		t2.start();
		t3.start();
		
		t1.join();
		t2.join();
		t3.join();

		m_client1.flush();
		m_client2.flush();
		m_server.flush();
		
		
		for(int i = 0; i < 200; i++)
		{
			m_client1Listener.clearLastMessage();
		}
		for(int i = 0; i < 200; i++)
		{
			m_client2Listener.clearLastMessage();
		}
		for(int i = 0; i < 200; i++)
		{
			m_serverListener.clearLastMessage();
		}
	} 
}


class MessageListener implements IMessageListener
{
	private ArrayList messages = new ArrayList();
	private ArrayList senders = new ArrayList();
	private Object lock = new Object();
	private String name;
	
	public MessageListener(String name)
	{
		this.name = name;
	}
	
	
	public void messageReceived(Serializable msg, INode from)
	{
		synchronized(lock)
		{
			messages.add(msg);
			senders.add(from);

			lock.notifyAll();
		}
	}
	
	public void clearLastMessage()
	{
		synchronized(lock)
		{
			if(messages.isEmpty())
				waitForMessage();
			messages.remove(0);
			senders.remove(0);
		}
	}
	
	public Object getLastMessage()
	{
		synchronized(lock)
		{
			if(messages.isEmpty())
				waitForMessage();
			return messages.get(0);
		}
	}

	public INode getLastSender()
	{
		synchronized(lock)
		{
			if(messages.isEmpty())
				waitForMessage();

			return (INode) senders.get(0);
		}
	}
	
	private void waitForMessage()
	{
		try
		{
			lock.wait(1500);
		}
		catch(InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}
	
	public int getMessageCount()
	{
		synchronized(lock)
		{
			return messages.size();
		}
	}
}

class MultipleMessageSender implements Runnable
{
	IMessenger m_messenger;
	
	public MultipleMessageSender(IMessenger messenger)
	{
		m_messenger = messenger;
	}
	
	public void run()
	{
		Thread.currentThread().yield();
		for(int i = 0; i < 100; i++)
		{
			m_messenger.broadcast( new Integer(i));
		}
	}
}