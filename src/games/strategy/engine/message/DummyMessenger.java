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

import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.ILoginValidator;
import games.strategy.net.IMessageListener;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Node;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A messenger that doesnt do anything.
 */
public class DummyMessenger implements IServerMessenger
{
	private final CopyOnWriteArrayList<IConnectionChangeListener> m_connectionChangeListeners = new CopyOnWriteArrayList<IConnectionChangeListener>();
	
	public DummyMessenger()
	{
		try
		{
			m_node = new Node("dummy", InetAddress.getLocalHost(), 0);
		} catch (final UnknownHostException e)
		{
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		}
	}
	
	/**
	 * Send a message to the given node. Returns immediately.
	 */
	public void send(final Serializable msg, final INode to)
	{
	}
	
	/**
	 * Send a message to all nodes.
	 */
	public void broadcast(final Serializable msg)
	{
	}
	
	/**
	 * Listen for messages of a certain type.
	 */
	public void addMessageListener(final IMessageListener listener)
	{
	}
	
	/**
	 * Stop listening to messages.
	 */
	public void removeMessageListener(final IMessageListener listener)
	{
	}
	
	/**
	 * Listen for messages of a certain type.
	 */
	public void addErrorListener(final IMessengerErrorListener listener)
	{
	}
	
	/**
	 * Stop listening to messages.
	 */
	public void removeErrorListener(final IMessengerErrorListener listener)
	{
	}
	
	private final INode m_node;
	
	/**
	 * Get the local node
	 */
	public INode getLocalNode()
	{
		return m_node;
	}
	
	/**
	 * Get a list of nodes.
	 */
	public Set<INode> getNodes()
	{
		return new HashSet<INode>();
	}
	
	/**
	 * test the connection.
	 */
	public boolean isConnected()
	{
		return true;
	}
	
	/**
	 * Shut the connection down.
	 */
	public void shutDown()
	{
	}
	
	/**
	 * Add a listener for change in connection status.
	 */
	public void addConnectionChangeListener(final IConnectionChangeListener listener)
	{
		m_connectionChangeListeners.add(listener);
	}
	
	/**
	 * Remove a listener for change in connection status.
	 */
	public void removeConnectionChangeListener(final IConnectionChangeListener listener)
	{
		m_connectionChangeListeners.remove(listener);
	}
	
	/**
	 * Returns when all messages have been written over the network. shutdown
	 * causes this method to return. Does not gaurantee that the messages have
	 * reached their destination.
	 */
	public void flush()
	{
	}
	
	public void setAcceptNewConnections(final boolean accept)
	{
	}
	
	public void waitForAllMessagsToBeProcessed()
	{
	}
	
	public boolean isServer()
	{
		return true;
	}
	
	public boolean isAcceptNewConnections()
	{
		return false;
	}
	
	public void removeConnection(final INode node)
	{
		for (final IConnectionChangeListener listener : m_connectionChangeListeners)
		{
			listener.connectionRemoved(node);
		}
	}
	
	public INode getServerNode()
	{
		return m_node;
	}
	
	public void setLoginValidator(final ILoginValidator loginValidator)
	{
	}
	
	public ILoginValidator getLoginValidator()
	{
		return null;
	}
	
	public InetSocketAddress getRemoteServerSocketAddress()
	{
		return m_node.getSocketAddress();
	}
	
	public void NotifyIPMiniBanningOfPlayer(final String ip)
	{
		
	}
	
	public void NotifyMacMiniBanningOfPlayer(final String mac)
	{
		
	}
	
	public void NotifyUsernameMiniBanningOfPlayer(final String username)
	{
		
	}
	
	public String GetPlayerMac(final String name)
	{
		return "DummyMacAddress";
	}
	
	public void NotifyUsernameMutingOfPlayer(final String username, final Date muteExpires)
	{
		
	}
	
	public void NotifyIPMutingOfPlayer(final String ip, final Date muteExpires)
	{
		
	}
	
	public void NotifyMacMutingOfPlayer(final String mac, final Date muteExpires)
	{
		
	}
	
	public boolean IsUsernameMiniBanned(final String username)
	{
		return false;
	}
	
	public boolean IsIpMiniBanned(final String ip)
	{
		return false;
	}
	
	public boolean IsMacMiniBanned(final String mac)
	{
		return false;
	}
	
	public boolean isShutDown()
	{
		return false;
	}
}
