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

/*
 * MangerTest.java
 *
 * Created on December 27, 2001, 12:24 PM
 */

package games.strategy.engine.message;

import junit.framework.*;
import java.util.*;
import java.io.*;

import games.strategy.net.*;

/**
 *
 * @author  Sean Bridges
 */
public class ManagerTest extends TestCase
{
	private static int SERVER_PORT = 3329;

	private IServerMessenger m_server;
	private IMessenger m_client1;
	private IMessenger m_client2;

	
	private IMessageManager m_client1MM;
	private IMessageManager m_client2MM;

	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(ManagerTest.class);
		return suite;
	}


	/** Creates a new instance of MangerTest */
    public ManagerTest(String name)
	{
		super(name);
    }

	public void setUp() throws IOException
	{
        SERVER_PORT++;
		m_server = new ServerMessenger("Server", SERVER_PORT);
		m_server.setAcceptNewConnections(true);

		m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1");
		m_client2 = new ClientMessenger("localhost", SERVER_PORT, "client2");

		new MessageManager(m_server);
		m_server.flush();
		m_client1MM = new MessageManager(m_client1);
		m_client1.flush();
		m_client2MM = new MessageManager(m_client2);
		m_client2.flush();

		//wait for messages to propogate
		try
		{
			synchronized(this)
			{
				wait(500);
			}
		} catch( Exception ie) {}


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


	public void testSendLocal()
	{
		TestDest td1 = new TestDest("td1");

		m_client1MM.addDestination(td1);

		TestMessage message = new TestMessage("message");
		Message response = m_client1MM.send(message, td1.getName());
		assertEquals(response.toString(), "td1message");
	}

	public void testSendRemote()
	{
		TestDest td1 = new TestDest("td1");
		m_client2MM.addDestination(td1);


		TestMessage message = new TestMessage("message");

		while(!m_client1MM.hasDestination(td1.getName()))
			Thread.yield();

		Message response = m_client1MM.send(message, td1.getName());
		assertEquals(response.toString(), "td1message");
	}
}

class TestDest implements IDestination
{
	private String m_name;

	TestDest(String name)
	{
		m_name = name;
	}

	public String getName()
	{
		return m_name;
	}

	public Message sendMessage(Message message)
	{
		return new TestMessage(m_name + message);
	}
}

class TestMessage implements Message
{
	private String m_text;

	TestMessage(String text)
	{
		m_text = text;
	}

	public String toString()
	{
		return m_text;
	}
}
