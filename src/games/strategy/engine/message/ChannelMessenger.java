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

import java.lang.reflect.*;

/**
 * Implementation of IChannelMessenger built on top of an IMessenger
 * 
 * @author Sean Bridges
 */
public class ChannelMessenger implements IChannelMessenger
{
    private final UnifiedMessenger m_unifiedMessenger;
    
    public ChannelMessenger(UnifiedMessenger messenger)
    {
        m_unifiedMessenger = messenger;
    }
    
    UnifiedMessenger getUnifiedMessenger()
    {
        return m_unifiedMessenger;
    }
    
    /* 
     * @see games.strategy.net.IChannelMessenger#getChannelBroadcastor(java.lang.String)
     */
    public IChannelSubscribor getChannelBroadcastor(RemoteName channelName)
    {
        InvocationHandler ih = new UnifiedInvocationHandler(m_unifiedMessenger,channelName.getName(), true, channelName.getClazz());
        
        IChannelSubscribor rVal = (IChannelSubscribor) Proxy.newProxyInstance(
                  Thread.currentThread().getContextClassLoader(), 
                  new Class[] {channelName.getClazz()}, ih );
        
        return rVal;
    }
   

    /* 
     * @see games.strategy.net.IChannelMessenger#registerChannelSubscriber(java.lang.Object, java.lang.String)
     */
    public void registerChannelSubscriber(Object implementor, RemoteName channelName)
    {
        if(!IChannelSubscribor.class.isAssignableFrom(channelName.getClazz()))
            throw new IllegalStateException(channelName.getClazz() + " is not a channel subscribor");
        
        m_unifiedMessenger.addImplementor(channelName,implementor, true );
    }


    /* 
     * @see games.strategy.net.IChannelMessenger#unregisterChannelSubscriber(java.lang.Object, java.lang.String)
     */
    public void unregisterChannelSubscriber(Object implementor, RemoteName channelName)
    {
        m_unifiedMessenger.removeImplementor(channelName.getName(), implementor);
    }    
    
    


    /* (non-Javadoc)
     * @see games.strategy.net.IChannelMessenger#getLocalNode()
     */
    public INode getLocalNode()
    {
        return m_unifiedMessenger.getLocalNode();
    }

    
    public boolean isServer()
    {
        return m_unifiedMessenger.isServer();
    }

 

}


