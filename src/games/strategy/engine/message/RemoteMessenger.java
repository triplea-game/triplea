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

import java.lang.reflect.*;

/**
 * An implementation of IRemoteMessenger based on MessageManager and Messenger.
 * 
 * 
 * @author Sean Bridges
 */
public class RemoteMessenger implements IRemoteMessenger
{
  
    UnifiedMessenger m_unifiedMessenger;
    
    public RemoteMessenger(UnifiedMessenger messenger)
    {
        m_unifiedMessenger = messenger;
    }
    
    private static String getUnifiedName(String channelName)
    {
        return "R:" + channelName;
    }
    
    private void assertRemoteExists(String channelName)
    {
        if(!m_unifiedMessenger.isAwareOfEndPoint(getUnifiedName(channelName)))
            throw new IllegalStateException("No remote called " + channelName);
    }

    
    public IRemote getRemote(String remoteNname)
    {
        //return an IChannelSubscribor  that knows how to call the methods correctly
        assertRemoteExists(remoteNname);
     
        String unifiedName = getUnifiedName(remoteNname);
        
        InvocationHandler ih = new UnifiedInvocationHandler(m_unifiedMessenger,unifiedName, false, true);
        
        IRemote rVal = (IRemote) Proxy.newProxyInstance(
                  Thread.currentThread().getContextClassLoader(), 
                  m_unifiedMessenger.getTypes(unifiedName), ih );
        
        return rVal;
    }

    /* 
     * @see games.strategy.net.IRemoteMessenger#registerRemote(java.lang.Class, java.lang.Object, java.lang.String)
     */
    public void registerRemote(Class remoteInterface, Object implementor,
            String name)
    {
         if(!IRemote.class.isAssignableFrom(remoteInterface))
             throw new IllegalArgumentException(remoteInterface.getName() +  "does not implement IRemote");
         if(!remoteInterface.isAssignableFrom(implementor.getClass()))
             throw new IllegalArgumentException(implementor + " does not implement " + remoteInterface.getName());
         if(!remoteInterface.isInterface())
             throw new IllegalArgumentException(remoteInterface.getName() +  " must be an interface");
        
         if(m_unifiedMessenger.isAwareOfEndPoint(getUnifiedName(name)))
             throw new IllegalStateException("Remote already bound:" + name);
         
        m_unifiedMessenger.createEndPoint(getUnifiedName(name), new Class[] {remoteInterface, IRemote.class}, false); 
        m_unifiedMessenger.addImplementor(getUnifiedName(name), implementor);
    }

    /* 
     * @see games.strategy.net.IRemoteMessenger#unregisterRemote(java.lang.String)
     */
    public void unregisterRemote(String name)
    {
        m_unifiedMessenger.destroyEndPoint(getUnifiedName(name));
    }
    
    /* 
     * @see games.strategy.net.IRemoteMessenger#hasRemote(java.lang.String)
     */
    public boolean hasRemote(String name)
    {
        return m_unifiedMessenger.isAwareOfEndPoint(getUnifiedName(name));
    }
    
    public void flush()
    {
        m_unifiedMessenger.flush();
    }
    
    public boolean isServer()
    {
        return m_unifiedMessenger.isServer();
    }

    
    public void waitForRemote(String name, long timeoutMS)
    {
        m_unifiedMessenger.waitForImplementors(getUnifiedName(name), timeoutMS);
    }
    
}



