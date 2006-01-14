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
    
    private static String getUnifiedName(String channelName)
    {
        //synchronized to ensure the unified name is fully created before it is used
        //synchronize on "C:"
        synchronized("C:")
        {
            return "C:" + channelName;
        }
    }
    
    private void assertChannelExists(String channelName)
    {
        if(!m_unifiedMessenger.isAwareOfEndPoint(getUnifiedName(channelName)))
            throw new IllegalStateException("No channel called " + channelName);
    }
    
    /* 
     * @see games.strategy.net.IChannelMessenger#getChannelBroadcastor(java.lang.String)
     */
    public IChannelSubscribor getChannelBroadcastor(String channelName)
    {
        //return an IChannelSubscribor  that knows how to call the methods correctly
        assertChannelExists(channelName);
     
        String unifiedName = getUnifiedName(channelName);
        
        InvocationHandler ih = new UnifiedInvocationHandler(m_unifiedMessenger,unifiedName, true, false);
        
        IChannelSubscribor rVal = (IChannelSubscribor) Proxy.newProxyInstance(
                  Thread.currentThread().getContextClassLoader(), 
                  m_unifiedMessenger.getTypes(unifiedName), ih );
        
        return rVal;
    }
   

    /* 
     * @see games.strategy.net.IChannelMessenger#registerChannelSubscriber(java.lang.Object, java.lang.String)
     */
    public void registerChannelSubscriber(Object implementor, String channelName)
    {
        m_unifiedMessenger.addImplementor(getUnifiedName(channelName),implementor );
    }


    /* 
     * @see games.strategy.net.IChannelMessenger#unregisterChannelSubscriber(java.lang.Object, java.lang.String)
     */
    public void unregisterChannelSubscriber(Object implementor, String channelName)
    {
        m_unifiedMessenger.removeImplementor(getUnifiedName(channelName), implementor);
    }    
    
    /* 
     * @see games.strategy.net.IChannelMessenger#createChannel(java.lang.Class, java.lang.String)
     */
    public void createChannel(Class<? extends IChannelSubscribor> channelInterface, String channelName)
    {
        if(!channelInterface.isInterface())
            throw new IllegalArgumentException(channelInterface.getName() +  " must be an interface");
        
        m_unifiedMessenger.createEndPoint(getUnifiedName(channelName), new Class[] {channelInterface, IChannelSubscribor.class}, true);
    }

    /* 
     * @see games.strategy.net.IChannelMessenger#destroyChannel(java.lang.String)
     */
    public void destroyChannel(String channelName)
    {
        m_unifiedMessenger.destroyEndPoint(getUnifiedName(channelName));
    }

    /* 
     * @see games.strategy.net.IChannelMessenger#getLocalSubscriborCount(java.lang.String)
     */
    public int getLocalSubscriborCount(String channelName)
    {
        return m_unifiedMessenger.getLocalImplementorCount(getUnifiedName(channelName));
    }
    
    public boolean hasSubscribors(String channelName)
    {
        return m_unifiedMessenger.isAwareOfImplementors(getUnifiedName(channelName));
    }
    
    /* 
     * @see games.strategy.net.IChannelMessenger#hasChannel(java.lang.String)
     */
    public boolean hasChannel(String channelName)
    {
       return m_unifiedMessenger.isAwareOfEndPoint(getUnifiedName(channelName));
    }

    /* (non-Javadoc)
     * @see games.strategy.net.IChannelMessenger#getLocalNode()
     */
    public INode getLocalNode()
    {
        return m_unifiedMessenger.getLocalNode();
    }

    public void flush()
    {
        m_unifiedMessenger.flush();
    }
    
    public boolean isServer()
    {
        return m_unifiedMessenger.isServer();
    }

    /* 
     * @see games.strategy.net.IChannelMessenger#waitForChannelToExist(java.lang.String, long)
     */
    public void waitForChannelToExist(String channelName, long timeoutMS)
    {
       m_unifiedMessenger.waitForEndPoint(getUnifiedName(channelName), timeoutMS); 
    }


}


