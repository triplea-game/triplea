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
import java.util.*;

/**
 * A messenger that doesnt do anything.
 */

public class DummyMessenger implements IServerMessenger
{
    /**
     * Send a message to the given node. Returns immediately.
     */
    public void send(Serializable msg, INode to)
    {
    }

    /**
     * Send a message to all nodes.
     */
    public void broadcast(Serializable msg)
    {
    }

    /**
     * Listen for messages of a certain type.
     */
    public void addMessageListener(IMessageListener listener)
    {
    }

    /**
     * Stop listening to messages.
     */
    public void removeMessageListener(IMessageListener listener)
    {
    }

    /**
     * Listen for messages of a certain type.
     */
    public void addErrorListener(IMessengerErrorListener listener)
    {
    }

    /**
     * Stop listening to messages.
     */
    public void removeErrorListener(IMessengerErrorListener listener)
    {
    }

    private final INode m_node = new Node("dummy", null, 0);

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
    public void addConnectionChangeListener(IConnectionChangeListener listener)
    {
    }

    /**
     * Remove a listener for change in connection status.
     */
    public void removeConnectionChangeListener(IConnectionChangeListener listener)
    {
    }

    /**
     * Returns when all messages have been written over the network. shutdown
     * causes this method to return. Does not gaurantee that the messages have
     * reached their destination.
     */
    public void flush()
    {
    }

    public void setAcceptNewConnections(boolean accept)
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

    public void removeConnection(INode node)
    {
        // TODO Auto-generated method stub
        
    }

    public INode getServerNode()
    {
        return m_node;
    }

}
