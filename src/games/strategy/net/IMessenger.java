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

package games.strategy.net;

import java.io.*;
import java.util.*;

/**
 * A simple way to connect multiple socket end points.
 * An IMessenger listens for incoming messages, and sends
 * them to all registered listeners.
 * 
 * Messages are recieved and sent in order, but because each 
 * recieved message is processed in a seperate thread, messages may be processed
 * of order.
 * 
 * If the order of the messages is important, you should use an OrderedMessage.
 * OrderedMessage is a marker interface.  The IMessenger will not start
 * processing an OrderedMessage until the previous OrderedMessage has finished processing.
 * 
 * If you want messages to be routed to a particular destination, or you want to block 
 * the thread sending message and recieve a response, you should consider using
 * IMessageManager instead.  The IMessageManager is built on top of
 * the IMesseneger
 *  
 */
public interface IMessenger
{
    /**
     * Send a message to the given node. Returns immediately.
     */
    public void send(Serializable msg, INode to);
    /**
     * Send a message to all nodes.
     */
    public void broadcast(Serializable msg);
    /**
     * Listen for messages of a certain type.
     */
    public void addMessageListener(IMessageListener listener);
    /**
     * Stop listening to messages.
     */
    public void removeMessageListener(IMessageListener listener);
    /**
     * Listen for messages of a certain type.
     */
    public void addErrorListener(IMessengerErrorListener listener);
    /**
     * Stop listening to messages.
     */
    public void removeErrorListener(IMessengerErrorListener listener);

    /**
     * Get the local node
     */
    public INode getLocalNode();

    /**
     * Get a list of nodes.
     */
    public Set getNodes();
    /**
     * test the connection.
     */
    public boolean isConnected();
    /**
     * Shut the connection down.
     */
    public void shutDown();

    /**
     * Add a listener for change in connection status.
     */
    public void addConnectionChangeListener(IConnectionChangeListener listener);

    /**
     * Remove a listener for change in connection status.
     */
    public void removeConnectionChangeListener(IConnectionChangeListener listener);

    /**
     * Returns when all messages have been written over the network. shutdown causes this method to return. Does not gaurantee that the messages have reached their destination.
     */
    public void flush();

    /**
     * Listen for when this messenger makes a broadcast.
     * 
     * This is used to listen for outgoing broadvasts, since these outgoing broadcasts will not be sent to a local message listener.
     */
    public void addBroadcastListener(IBroadcastListener listener);
    public void removeBroadcastListener(IBroadcastListener listener);
    
    /**
     * Pause the current thread until there are no messages are being processed.
     */
    public void waitForAllMessagsToBeProcessed();
}
