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

/**
 * A simple way to multicast method calls over several machines and several objects.<br>
 * 
 * A channel can be created such that all channel subscribers must implement the same<br>
 * interface.  Channel subscribors can be on multiple machines.
 * 
 * On VM A
 * <pre>
 * 
 * someChannelMessenger.createChannel(IFoo.class, "FOO");
 * 
 * IFoo aFoo = new Foo(); 
 * someChannelMessenger.registerChannelSubscribor(aFoo, "FOO");
 * 
 * </pre>
 * 
 * On VM B
 * <pre>
 * 
 * IFoo anotherFoo = new Foo();
 * anotherChannelMessenger.registerChannelSubscribor(anotherFoo, "FOO");
 * 
 * 
 * IFoo multicastFoo = (IFoo) anotherChannelMessenger.getChannelBroadcastor("FOO");
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
 * @author Sean Bridges
 */
public interface IChannelMessenger
{
    /**
     * Get a reference such that methods called on it will be multicast
     * to all subscribors of the channel 
     */
    public IChannelSubscribor getChannelBroadcastor(String channelName);
    
    /**
     * register a subscribor to a channel 
     */
    public void registerChannelSubscriber(Object implementor, String channelName);
    
    /**
     * unregister a subscribor to a channel 
     */
    public void unregisterChannelSubscriber(Object implementor, String channelName);
    
    
    /**
     * Create a channel.
     * Calling this method will create the channel for all IChannelMessengers
     * that this IChannelMessenger is connected to.
     */
    public void createChannel(Class channelInterface, String channelName);

    /**
     * Destroy a channel.
     * Calling this method will destroy the channel for all IChannelMessengers
     * that this IChannelMessenger is connected to. 
     * 
     */
    public void destroyChannel(String channelName);
    
    /**
     * The number of subscribors registered on this instance on  the given channel 
     *  
     */
    public int getLocalSubscriborCount(String channelName);
    
    /**
     * Does the channel exist. 
     * Note that due to threading and networking delays, a channel created
     * on a seperate IChannelMessenger may take a while to be propogated. 
     */
    public boolean hasChannel(String channelName);
    
    public INode getLocalNode();
    
    /**
     * wait for the underlying transport layer to finish transmitting all data queued
     */
    public void flush();
    
    public boolean isServer();
    
    /**
     * 
     * Wait for the channel messenger to be aware of a channel.  Channels created
     * on other vms will not instantly be visible to all messengers.
     * 
     * @param channelName - the channel to wait for
     * @param timeoutMS - if -1 means wait forever
     */
    public void waitForChannelToExist(String channelName, long timeoutMS);
}
