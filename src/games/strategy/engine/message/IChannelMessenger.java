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

import games.strategy.net.INode;

/**
 * A simple way to multicast method calls over several machines and possibly several objects on each machine.<br>
 * 
 * A channel can be created such that all channel subscribers must implement the same<br>
 * interface.  Channel subscribors can be on multiple machines.
 * 
 * 
 * On VM A
 * <pre>
 * RemoteName FOO = new RemoteName("Foo", IFoo.class);
 * 
 * IFoo aFoo = new Foo(); 
 * someChannelMessenger.registerChannelSubscribor(aFoo, FOO);
 * 
 * </pre>
 * 
 * On VM B
 * <pre>
 * 
 * IFoo anotherFoo = new Foo();
 * anotherChannelMessenger.registerChannelSubscribor(anotherFoo, FOO);
 * 
 * 
 * IFoo multicastFoo = (IFoo) anotherChannelMessenger.getChannelBroadcastor(FOO);
 * multicastFoo.fee();

 * </pre>
 * 
 * The <code>multicast.fee()</code> line results in two method calls, one on VM B to anotherFoo, and
 * another call on VM A to aFoo.<br>
 * 
 * The magic is done using reflection and dynamic proxies (java.lang.reflect.Proxy)<br>
 *
 * To avoid naming conflicts, it is advised to use the fully qualified java name and 
 * and a constant as channel names.  For example channel names should be in the form<br>
 * <br> 
 * "foo.fee.Fi.SomeConstant"<br>
 * <br>
 * where SomeConstant is defined in the class foo.fee.Fi<br> 
 * <br>
 * 
 * 
 * 
 * <p><b>Channels and threading</b>
 * <p>
 * There will only be one thread calling methods in a channel at one time.  Methods will be 
 * called on subscribors in the order that they are called on broadcasters.  This means that if you 
 * block the current thread during a client invocation, no further methods can be called on that channel.
 * 
 * <p>
 * @author Sean Bridges
 */
public interface IChannelMessenger
{
    /**
     * Get a reference such that methods called on it will be multicast
     * to all subscribors of the channel 
     */
    public IChannelSubscribor getChannelBroadcastor(RemoteName channelName);
    
    /**
     * register a subscribor to a channel 
     */
    public void registerChannelSubscriber(Object implementor, RemoteName channelName);
    
    /**
     * unregister a subscribor to a channel 
     */
    public void unregisterChannelSubscriber(Object implementor, RemoteName channelName);
    
    
    public INode getLocalNode();
    
    /**
     * wait for the underlying transport layer to finish transmitting all data queued
     */
    public void flush();
    
    /**
     * Is the underlying messenger a server?
     */
    public boolean isServer();
    
}
