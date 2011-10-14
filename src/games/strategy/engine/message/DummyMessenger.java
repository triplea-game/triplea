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

import java.io.*;
import java.net.*;
import java.util.*;
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
        } catch (UnknownHostException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
    }
    
    /**
     * Send a message to the given node. Returns immediately.
     */
    @Override
	public void send(Serializable msg, INode to)
    {
    }

    /**
     * Send a message to all nodes.
     */
    @Override
	public void broadcast(Serializable msg)
    {
    }

    /**
     * Listen for messages of a certain type.
     */
    @Override
	public void addMessageListener(IMessageListener listener)
    {
    }

    /**
     * Stop listening to messages.
     */
    @Override
	public void removeMessageListener(IMessageListener listener)
    {
    }

    /**
     * Listen for messages of a certain type.
     */
    @Override
	public void addErrorListener(IMessengerErrorListener listener)
    {
    }

    /**
     * Stop listening to messages.
     */
    @Override
	public void removeErrorListener(IMessengerErrorListener listener)
    {
    }

    private final INode m_node;

    /**
     * Get the local node
     */
    @Override
	public INode getLocalNode()
    {
        return m_node;
    }

    /**
     * Get a list of nodes.
     */
    @Override
	public Set<INode> getNodes()
    {
        return new HashSet<INode>();
    }

    /**
     * test the connection.
     */
    @Override
	public boolean isConnected()
    {
        return true;
    }

    /**
     * Shut the connection down.
     */
    @Override
	public void shutDown()
    {
    }

    /**
     * Add a listener for change in connection status.
     */
    @Override
	public void addConnectionChangeListener(IConnectionChangeListener listener)
    {
        m_connectionChangeListeners.add(listener);
    }

    /**
     * Remove a listener for change in connection status.
     */
    @Override
	public void removeConnectionChangeListener(IConnectionChangeListener listener)
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

    @Override
	public void setAcceptNewConnections(boolean accept)
    {
    }


    public void waitForAllMessagsToBeProcessed()
    {

    }

    @Override
	public boolean isServer()
    {
        return true;
    }

    @Override
	public boolean isAcceptNewConnections()
    {
        return false;
    }

    @Override
	public void removeConnection(INode node)
    {
        for(IConnectionChangeListener listener : m_connectionChangeListeners) {
            listener.connectionRemoved(node);
        }
    }

    @Override
	public INode getServerNode()
    {
        return m_node;
    }

    @Override
	public void setLoginValidator(ILoginValidator loginValidator)
    {
    }
    
    @Override
	public ILoginValidator getLoginValidator()
    {
        return null;
    }

    @Override
	public InetSocketAddress getRemoteServerSocketAddress()
    {
        return m_node.getSocketAddress();
    }
    
   

}
