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

package games.strategy.net.nio;

import games.strategy.net.INode;
import games.strategy.net.MessageHeader;

import java.nio.channels.SocketChannel;

/**
 * Call backs for an NIO Socket
 */
public interface NIOSocketListener
{

    /**
     * This connection will leave quarantine.
     * 
     *  Messages on this channel will not be read until after this method returns, allowing for setup of the
     *  channel.
     */
    public void socketUnqaurantined(SocketChannel channel, QuarantineConversation conversation);
    
    
    /**
     * An error occured on the channel and it was shut down. 
     */
    
    public void socketError(SocketChannel channel, Exception error);
    
    
    /**
     * 
     */
    public void messageReceived(MessageHeader message, SocketChannel channel);
    
    
    
    /**
     * Get the remote node id for this channel, or null if the remote node id is not yet known.
     * The node may be unknown if the channel is still quarantined
     */
    public INode getRemoteNode(SocketChannel channel);
    
    /**
     * 
     * Get the node id for the local machine, or null if the remote node is not yet known.
     * The node must be known by the time we have an unquarantined channel.
     * 
     */
    public INode getLocalNode();
    
}
