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
 * Invocation handler for the UnifiedMessenger
 *
 *
 * @author Sean Bridges
 */
/**
 * Handles the invocation for a channel
 */
class UnifiedInvocationHandler implements InvocationHandler
{
    private final UnifiedMessenger m_messenger;
    private final String m_endPointName;
    private final boolean m_ignoreResults;
    private final boolean m_assertExactlyOneResults;

      
    public UnifiedInvocationHandler(final UnifiedMessenger messenger, final String endPointName, final boolean ignoreResults, boolean assertExactlyOneResult)
    {
        m_messenger = messenger;
        m_endPointName = endPointName;
        m_ignoreResults = ignoreResults;
        m_assertExactlyOneResults = assertExactlyOneResult;
    }
    
    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        RemoteMethodCall remoteMethodMsg = new RemoteMethodCall(m_endPointName, method.getName(), args, method.getParameterTypes());
        if(m_ignoreResults)
        {
            m_messenger.invoke(m_endPointName, remoteMethodMsg);
            return null;
        }
        else
        {
            RemoteMethodCallResults[] response = m_messenger.invokeAndWait(m_endPointName, remoteMethodMsg);

            if(m_assertExactlyOneResults && response.length == 0)
                throw new RemoteNotFoundException("Remote not found:" + m_endPointName);
            
            if(m_assertExactlyOneResults && response.length != 1)
                throw new IllegalStateException("Expecting only one result, but got:" + response.length + " end point:" + m_endPointName);
                
            if(response[0].getException() != null)
                throw response[0].getException();
            return response[0].getRVal();
        }
    }   
}