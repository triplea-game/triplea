package games.strategy.net;

import java.io.*;
import java.util.*;
import java.net.*;

import games.strategy.util.ListenerList;

public class ClientMessenger implements IMessenger 
{
	private INode m_node;
	private Set m_allNodes;
	private ListenerList m_listeners = new ListenerList();
	private Connection m_connection;
	private ListenerList m_errorListeners = new ListenerList();
	private ListenerList m_connectionListeners = new ListenerList();

	public ClientMessenger(String host, int port, String name) throws IOException, UnknownHostException
	{
		this(host, port, name, new DefaultObjectStreamFactory());
	}
	
	public ClientMessenger(String host, int port, String name, IObjectStreamFactory streamFact) throws IOException, UnknownHostException
	{
		Socket socket = new Socket(host, port);
		m_node = new Node(name, socket.getLocalAddress(), socket.getLocalPort());				
		
		m_connection = new Connection(socket, m_node, m_connectionListener, streamFact);
		
		//wait for the init message
		while(m_allNodes == null && m_connection.isConnected())
		{
			try
			{
				synchronized(this)
				{
					wait(100);	
				}
			} catch(InterruptedException ie)
			{}
		}
		if(!m_connection.isConnected())
			throw new IOException("Connection lost");
	}
		
	private void serverMessageReceived(ServerMessage msg)
	{
		if(msg instanceof NodeChangeServerMessage)
		{
			nodeChangeMessageReceived( (NodeChangeServerMessage) msg);
		}
		else if(msg instanceof ClientInitServerMessage)
		{
			initMessageReceived( (ClientInitServerMessage) msg);
		}
		else 
			throw new IllegalArgumentException("Unknown server messgae:" + msg);
	}
	
	private synchronized void nodeChangeMessageReceived(NodeChangeServerMessage msg)
	{
		INode node = msg.getNode();
		if(msg.getAdd())
		{
			m_allNodes.add(node);
		}
		else
		{
			m_allNodes.remove(node);
		}
		notifyConnectionsChanged();
	}
	
	private synchronized void initMessageReceived(ClientInitServerMessage msg)
	{
		m_allNodes = msg.getAllNodes();
	}

	/*
	 * @see IMessenger#send(Serializable, INode)
	 */
	public synchronized void send(Serializable msg, INode to)
	{
		MessageHeader header = new MessageHeader(to, m_node, msg);
		m_connection.send(header);
	}

	/*
	 * @see IMessenger#broadcast(Serializable)
	 */
	public synchronized void broadcast(Serializable msg) 
	{
		MessageHeader header = new MessageHeader(m_node, msg);
		m_connection.send(header);
	}

	/*
	 * @see IMessenger#addMessageListener(Class, IMessageListener)
	 */
	public void addMessageListener(IMessageListener listener) 
	{
		m_listeners.add(listener);
	}

	/*
	 * @see IMessenger#removeMessageListener(Class, IMessageListener)
	 */
	public void removeMessageListener(IMessageListener listener) 
	{
		m_listeners.remove(listener);
	}

	public void addErrorListener(IMessengerErrorListener  listener) 
	{
		m_errorListeners.add(listener);
	}

	public void removeErrorListener(IMessengerErrorListener  listener) 
	{
		m_errorListeners.remove(listener);
	}
	
	/*
	 * @see IMessenger#getNodes()
	 */
	public synchronized Set getNodes() 
	{
		//if the init message hasnt reached us yet, stall
		return Collections.unmodifiableSet(m_allNodes);
	}

	/*
	 * @see IMessenger#isConnected()
	 */
	public boolean isConnected() 
	{
		return m_connection.isConnected();
	}

	public void shutDown()
	{
		m_connection.shutDown();
		m_allNodes = Collections.EMPTY_SET;
		
	}
	
	private IConnectionListener m_connectionListener = new IConnectionListener()
	{
		public void messageReceived(Serializable message, Connection connection)
		{
			ClientMessenger.this.messageReceived( (MessageHeader) message);
		}
		
		public void fatalError(Exception error, Connection connection, List unsent)
		{
			Iterator iter = m_errorListeners.iterator();
			while(iter.hasNext())
			{
				IMessengerErrorListener errorListener = (IMessengerErrorListener) iter.next();
				errorListener.messengerInvalid(ClientMessenger.this, error, unsent);
			}
		}
	};
	
	private void messageReceived(MessageHeader msg)
	{
		if(msg.getMessage() instanceof ServerMessage)
			serverMessageReceived( (ServerMessage) msg.getMessage());
		else
		{	
			Iterator iter = m_listeners.iterator();
			while(iter.hasNext())
			{
				IMessageListener listener = (IMessageListener) iter.next();
				listener.messageReceived(msg.getMessage(), msg.getFrom());
			}
		}
	}

	public void flush()
	{
		m_connection.flush();
	}
	
	public void addConnectionChangeListener(IConnectionChangeListener  listener) 
	{
		m_connectionListeners.add(listener);
	}

	public void removeConnectionChangeListener(IConnectionChangeListener listener) 
	{
		m_connectionListeners.remove(listener);
	}

	private void notifyConnectionsChanged()
	{
		Iterator iter =  m_connectionListeners.iterator();
		while(iter.hasNext())
		{
			((IConnectionChangeListener) iter.next()).connectionsChanged();
		}
	}
	
	/**
	 * Get the local node
	 */
	public INode getLocalNode()
	{
		return m_node;
	}
	
	public INode getServerNode()
	{
		return m_connection.getRemoteNode();
	}
}