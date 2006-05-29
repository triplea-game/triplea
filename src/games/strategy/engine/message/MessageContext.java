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
 */

package games.strategy.engine.message;

import games.strategy.net.INode;


/**
 * Information useful on invocation of remote networked events. 
 * 
 * @author sgb
 */
public class MessageContext
{

    //the current caller of the remote or channel
    private static final ThreadLocal<INode> m_sender = new ThreadLocal<INode>();
    
    //package access
    //should only be called by EndPoint
    static void setSenderNodeForThread(INode node)
    {
        m_sender.set(node);
    }
    
    
    /**
     * 
     * Within the invocation on a remote method on an IRemote or an IChannelSubscriber, 
     * this method will return the node that originated the message.<p>
     * 
     * Will return null if the current thread is not currenlty executing a remote method
     * of an IRemote or IChannelSubscrobor.<p>
     * 
     * This is set by the server, and cannot be overwritten by the client, and can be used
     * to verify where messages come from.<p>
     * 
     * @return the node that originated the message being received
     */
    public static INode getSender()
    {
        return m_sender.get();
    }
}
