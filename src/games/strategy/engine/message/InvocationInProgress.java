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


import games.strategy.net.GUID;
import games.strategy.net.INode;


/**
 * We are waiting for the results of a remote invocation.
 *  
 * @author sgb
 */
public class InvocationInProgress
{
    private final INode m_waitingOn;
    private final HubInvoke m_methodCall;
    private final INode m_caller;
    private RemoteMethodCallResults m_results;
    
    public InvocationInProgress( INode waitingOn, final HubInvoke methodCalls, final INode methodCallsFrom)
    {
        m_waitingOn = waitingOn;
        m_methodCall = methodCalls;
        m_caller = methodCallsFrom;
    }
    
    public boolean isWaitingOn(INode node)
    {
        return m_waitingOn.equals(node);
    }
    
    /**
     * 
     * @return true if there are no more results to process
     */
    public boolean process(HubInvocationResults hubresults, INode from)
    {
        if(hubresults.results == null)
            throw new IllegalStateException("No results");
        
        m_results = hubresults.results;
        
        if(!from.equals(m_waitingOn))
            throw new IllegalStateException("Wrong node, expecting " + m_waitingOn + " got " + from);
        
        return true;
        
    }
    
    public HubInvoke getMethodCall()
    {
        return m_methodCall;
    }
    
    public INode getCaller()
    {
        return m_caller;
    }
    
    public RemoteMethodCallResults getResults()
    {
        return m_results;
    }
    
    public GUID getMethodCallID()
    {
        return m_methodCall.methodCallID;
    }
    
    public boolean shouldSendResults()
    {
        return m_methodCall.needReturnValues;
    }
    
    
}