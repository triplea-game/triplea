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
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessageListener;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UnifiedMessengerHub implements IMessageListener, IConnectionChangeListener
{
    
    private final static Logger s_logger = Logger.getLogger(UnifiedMessengerHub.class.getName());
    private final UnifiedMessenger m_localUnified;
    
    //the messenger we are based on
    private final IMessenger m_messenger;
  
    //maps end points to a list of nodes with implementors
    private final Map<String, Collection<INode>> m_endPoints = new HashMap<String, Collection<INode>>();
    
    //changes to the list of endpoints, or reads to it, should be made
    //only while holding this lock
    private final Object m_endPointMutex = new Object();
  
    //the invocations that are currently in progress
    private final Map<GUID, InvocationInProgress> m_invocations = new ConcurrentHashMap<GUID, InvocationInProgress>();

    
    public UnifiedMessengerHub(final IMessenger messenger, UnifiedMessenger localUnified)
    {
        m_messenger = messenger;
        m_localUnified = localUnified;
        m_messenger.addMessageListener(this);
        ((IServerMessenger)m_messenger).addConnectionChangeListener(this);
    }

    private void send(Serializable msg, INode to)
    {
        if(m_messenger.getLocalNode().equals(to))
        {
            m_localUnified.messageReceived(msg, m_messenger.getLocalNode());
        }
        else
        {
            m_messenger.send(msg, to);
        }
        
    }



    public void messageReceived(Serializable msg, INode from)
    {
        if(msg instanceof HasEndPointImplementor)
        {
            synchronized(m_endPointMutex)
            {
                HasEndPointImplementor hasEndPoint = (HasEndPointImplementor) msg;
                Collection<INode> nodes =m_endPoints.get(hasEndPoint.endPointName);
                if(nodes == null)
                {
                    nodes = new ArrayList<INode>();
                    m_endPoints.put(hasEndPoint.endPointName, nodes);
                }
                if(nodes.contains(from))
                    throw new IllegalStateException("Already contained, new" + from + " existing, " + nodes + " name " + hasEndPoint.endPointName);
                nodes.add(from);
            }
        } else if(msg instanceof NoLongerHasEndPointImplementor)
        {
            synchronized(m_endPointMutex)
            {
                NoLongerHasEndPointImplementor hasEndPoint = (NoLongerHasEndPointImplementor) msg;
                Collection<INode> nodes =m_endPoints.get(hasEndPoint.endPointName);
                if(nodes != null)
                {
                    if(!nodes.remove(from))
                        throw new IllegalStateException("Not removed!");
                    if(nodes.isEmpty())
                        m_endPoints.remove(hasEndPoint.endPointName);
                }
            }
        }
        else if(msg instanceof HubInvoke)
        {
            HubInvoke invoke = (HubInvoke) msg;
            
            final Collection<INode> endPointCols = new ArrayList<INode>();
            synchronized(m_endPointMutex)
            {
                if(m_endPoints.containsKey(invoke.call.getRemoteName()))
                    endPointCols.addAll(m_endPoints.get(invoke.call.getRemoteName()));
            
            }
            if(endPointCols.isEmpty())
            {
                if(invoke.needReturnValues)
                {
                    RemoteMethodCallResults results = new RemoteMethodCallResults(new RemoteNotFoundException("Not found"));
                    send(new SpokeInvocationResults(results, invoke.methodCallID ),  from);
                }
                else
                {
                    //no end points, this is ok, we
                    //we are a channel with no implementors
                }
            }
            else
            {
                invoke(invoke, endPointCols, from);
            }
            
            
        }
        else if(msg instanceof HubInvocationResults)
        {
            HubInvocationResults results = (HubInvocationResults) msg;
            results(results,from);
        }

        
        
    }




    private void results(HubInvocationResults results, INode from)
    {

        GUID methodID = results.methodCallID;
        
        InvocationInProgress invocationInProgress = m_invocations.get(methodID);
        boolean done = invocationInProgress.process(results, from);
        
        if(done)
        {
            m_invocations.remove(methodID);
        
            HubInvoke hubInvoke = invocationInProgress.getMethodCall();
            if(s_logger.isLoggable(Level.FINER))
            {
                s_logger.log(Level.FINER, "Method returned:" + hubInvoke.call.getMethodName() + " for remote name:" + hubInvoke.call.getRemoteName()
                        + " with id:" + hubInvoke.methodCallID);
            }
            
            if(invocationInProgress.shouldSendResults())
                sendResultsToCaller(methodID, invocationInProgress);
        }
    }




    private void sendResultsToCaller(GUID methodID, InvocationInProgress invocationInProgress)
    {
        
        RemoteMethodCallResults result = invocationInProgress.getResults();
        INode caller = invocationInProgress.getCaller(); 
        
        SpokeInvocationResults spokeResults = new SpokeInvocationResults(result, methodID);
        send(spokeResults, caller );
    }




    private void invoke(HubInvoke hubInvoke, final Collection<INode> remote, INode from)
    {
        
        if(hubInvoke.needReturnValues)
        {
            if(remote.size() != 1)
            {
                throw new IllegalStateException("Too many nodes:" + remote + " for remote name " + hubInvoke.call);
            }
            
            InvocationInProgress invocationInProgress = new InvocationInProgress(remote.iterator().next(), hubInvoke, from);
            m_invocations.put(hubInvoke.methodCallID, invocationInProgress);
            
            
            if(s_logger.isLoggable(Level.FINER))
            {
                s_logger.log(Level.FINER, "Waiting for method:" + hubInvoke.call.getMethodName() + " for remote name:" + hubInvoke.call.getRemoteName()
                    + " with id:" + hubInvoke.methodCallID);
            }
        }

        //invoke remotely
        SpokeInvoke invoke = new SpokeInvoke(hubInvoke.methodCallID, hubInvoke.needReturnValues, hubInvoke.call, from);
        for(INode node : remote)
        {
            send(invoke, node);
        }
        
    }
    
    
    
    
    /**
     * Wait for the messenger to know about the given endpoint.
     * 
     * @param endPointName
     * @param timeout
     */
    public void waitForNodesToImplement(String endPointName, long timeoutMS)
    {
        //dont use Long.MAX_VALUE since that will overflow
        if (timeoutMS <= 0)
            timeoutMS = Integer.MAX_VALUE;

        long endTime = timeoutMS + System.currentTimeMillis();

        while (System.currentTimeMillis() < endTime && !hasImplementors(endPointName))
        {
            try
            {
                Thread.sleep(50);
            } catch (InterruptedException e)
            {
                //whats a devloper to do
            }
        }
    }

    boolean hasImplementors(String endPointName)
    {
        synchronized(m_endPointMutex)
        {
            return m_endPoints.containsKey(endPointName) && !m_endPoints.get(endPointName).isEmpty();
        }
    }




    public void connectionAdded(INode to)
    {}




    public void connectionRemoved(INode to)
    {
        //we lost a connection to a node
        //any pending results should return
        
        synchronized(m_endPointMutex)
        {
            for(Collection<INode> nodes : m_endPoints.values())
            {
                nodes.remove(to);
            }
        }
        Iterator<InvocationInProgress> waitingIterator = m_invocations.values().iterator();
        while(waitingIterator.hasNext())
        {
            InvocationInProgress invocation = waitingIterator.next();
            if(invocation.isWaitingOn(to))
            {
                RemoteMethodCallResults results = new  RemoteMethodCallResults(new ConnectionLostException("Connection to " + to.getName() + " lost"));
                HubInvocationResults hubResults = new HubInvocationResults(results, invocation.getMethodCallID());
                results(hubResults, to);
            }
        }
    }




 
    
}



