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
  
    private final UnifiedMessenger m_unifiedMessenger;
    
    public RemoteMessenger(UnifiedMessenger messenger)
    {
        m_unifiedMessenger = messenger;
    }
    
    

    
    public IRemote getRemote(RemoteName remoteName)
    {
        
        InvocationHandler ih = new UnifiedInvocationHandler(m_unifiedMessenger,remoteName.getName(), false, remoteName.getClazz());
        
        IRemote rVal = (IRemote) Proxy.newProxyInstance(
                  Thread.currentThread().getContextClassLoader(), 
                  new Class[] {remoteName.getClazz()}, ih );
        
        return rVal;
    }

    
    public void registerRemote(Object implementor,
            RemoteName name)
    {
        m_unifiedMessenger.addImplementor(name, implementor, false); 
    }

    
    public void unregisterRemote(RemoteName name)
    {
        unregisterRemote(name.getName());
    }
    
    public void flush()
    {
        m_unifiedMessenger.flush();
    }
    
    public boolean isServer()
    {
        return m_unifiedMessenger.isServer();
    }




    public void unregisterRemote(String name)
    {
        m_unifiedMessenger.removeImplementor(name, m_unifiedMessenger.getImplementor(name));
        
    }

    public boolean hasLocalImplementor(RemoteName descriptor)
    {
        return m_unifiedMessenger.getLocalEndPointCount(descriptor) == 1;
    }
    
}



