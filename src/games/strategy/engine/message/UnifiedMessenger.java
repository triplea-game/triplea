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
import games.strategy.util.Match;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;


/**
 * A messenger general enough that both Channel and Remote messenger can be based on it.
 * 
 * @author Sean Bridges
 */
public class UnifiedMessenger
{
    //the messenger we are based on
    private final IMessenger m_messenger;
    
    //a thread pool to run the invoke on
    private games.strategy.thread.ThreadPool m_threadPool = new games.strategy.thread.ThreadPool(15, "UnifiedMessengerPool");
    
    //maps String -> EndPoint
    //these are the end points we know about
    //some of them may not have implementors
    private final Map m_localEndPoints = Collections.synchronizedMap(new HashMap());
            
    //maps INode - collection of endpoints as string that the INode has implementors for
    private final Map m_remoteNodesWithImplementors = Collections.synchronizedMap(new HashMap());
    
    //maps GUID-> collection of RemoteMethodResults
    private final Map m_methodCallResults = Collections.synchronizedMap(new HashMap());
    
    //maps GUID -> number of clients we are awaiting responses from
    private final Map m_methodCallWaitCount = Collections.synchronizedMap(new HashMap());
    
    //synchronize on this to wait for the init lock to arrive
    private final Object m_initLock = new Object();

    //lock on this for modifications to end points
    private final Object m_endPointMutex = new Object(); 
    
    /**
     * @param messenger
     */
    public UnifiedMessenger(final IMessenger messenger)
    {
        m_messenger = messenger;
        m_messenger.addMessageListener(m_messageListener);
        
        if(m_messenger.isServer())
        {
            //we are responsible for initializing others
            //add a listener to deal with it
            m_messenger.addMessageListener(m_serverMessageListener);
        }
        else
        {
            //we want to wait till we are initialized before the constructor returns
            synchronized(m_initLock)
            {
                m_messenger.broadcast(new UnifiedInitRequest());
                try
                {
                    m_initLock.wait();
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Create an end point.  Creating an equivalent end point multiple times is ok, subsequent calls 
     * will be ignored if the end point is the same, but an error will be generated if an attemot to
     * change the endpoint definition through a create call.
     * 
     * @param name - the name of the endpoint
     * @param classes
     * @param singleThreaded - if more than one thread can be calling methods on end points at a time.
     */
    public void createEndPoint(String name, Class[] classes, boolean singleThreaded)
    {
        createEndPointInternal(name, classes, singleThreaded);
        m_messenger.broadcast(new EndPointCreated( RemoteMethodCall.classesToString(classes), name, singleThreaded));
    }
    
    private void createEndPointInternal(String name, Class[] classes, boolean singleThreaded)
    {
        synchronized(m_endPointMutex)
        {
	        EndPoint endPoint = new EndPoint(name, classes, singleThreaded);
	        
	        //if we already have it, make sure its the same classes
	        if(m_localEndPoints.containsKey(name))
	        {
	            EndPoint existing = (EndPoint) m_localEndPoints.get(name);
	            if(existing.equivalent(endPoint))
	            {
	                return; 
	            }
	            else
	            {
	                throw new IllegalStateException("End point already exists with different types or threading");
	            }
	        }
	        
	        //we dont have it
	        //add it
	        synchronized(m_endPointMutex)
	        {
	            m_localEndPoints.put(name, new EndPoint(name,classes, singleThreaded));
	        }
        }
    }
    
    public void destroyEndPoint(String name)
    {
        m_messenger.broadcast(new EndPointDestroyed(name));
        destroyEndPointInternal(name);
    }
    
    private void destroyEndPointInternal(String name)
    {
        synchronized(m_endPointMutex)
        {
            m_localEndPoints.remove(name);
	        Iterator remotes = m_remoteNodesWithImplementors.values().iterator();
	        while(remotes.hasNext())
	        {
	            Collection coll = (Collection) remotes.next();
	            coll.remove(name);
	        }
        }
    }

    private INode[] getNodesWithImplementors(final String endPointName)
    {
        Match containsEndPoint = new Match()
        {
            public boolean match(Object o)
            {
                return ((Collection) o).contains(endPointName);
            }
        };
        
        Set matching;
        
        synchronized(m_endPointMutex)
        {
            matching = Match.getKeysWhereValueMatch(m_remoteNodesWithImplementors, containsEndPoint);
        }
        
        return (INode[]) matching.toArray(new INode[matching.size()]);
    }

    public Class[] getTypes(String endPointName)
    {
        assertEndPointExists(endPointName);
        return ((EndPoint)m_localEndPoints.get(endPointName)).getClasses();
    }
    
    public int getLocalImplementorCount(String endPointName)
    {
        assertEndPointExists(endPointName);
        return ((EndPoint)m_localEndPoints.get(endPointName)).getLocalImplementorCount();
    }
    
    /**
     *  Invoke and wait for all implementors on all vms to finish executing.
     *
     */
    public RemoteMethodCallResults[] invokeAndWait(String endPointName, RemoteMethodCall remoteCall)
    {
        assertEndPointExists(endPointName);
        
        //prepatory to anything else...
        //generate a unique id
        GUID methodCallID = new GUID();
        
        //who do we call
        INode[] remote = getNodesWithImplementors(endPointName);
               
        if(remote.length > 0)
        {
            //we need to indicate how many returns we are waiting for
            //this value should be removed from the map when set to 0
	        m_methodCallWaitCount.put(methodCallID, new Integer(remote.length));
        }

        //invoke remotely
        Invoke invoke = new Invoke(methodCallID, true, remoteCall, endPointName);
        for(int i = 0; i <remote.length; i++)
        {
            m_messenger.send(invoke, remote[i]);
        }
        
        //invoke locally, we want to do this after remote invocation, since the remote calls and
        //the local calls may execute concurrently
        EndPoint endPoint = (EndPoint) m_localEndPoints.get(endPointName);
        List results = endPoint.invokeLocal(remoteCall);
                
        //wait for the remote calls to finish
        if(remote.length > 0)
        {            
            synchronized(methodCallID)
            {
                //wait until the results are cleared
                while(!m_methodCallResults.containsKey(methodCallID))
                {
                    try
                    {
                        methodCallID.wait();
                    } catch(InterruptedException e)
                    {
                        e.printStackTrace(System.out);
                    }
                }
                results.addAll( (List) m_methodCallResults.get(methodCallID));
                m_methodCallResults.remove(methodCallID);
            }
        }
       
        return (RemoteMethodCallResults[]) results.toArray(new RemoteMethodCallResults[results.size()]);
    }
        
    /**
     * invoke without waiting for remote nodes to respond 
     */
    public void invoke(final String endPointName, final RemoteMethodCall call)
    {
        assertEndPointExists(endPointName);
        
        //invoke remotely first, allows concurrent execution 
        //with local invocation
        INode[] destinations = getNodesWithImplementors(endPointName);
        Invoke invoke = new Invoke(null, false, call, endPointName);
        for(int i = 0; i < destinations.length; i++)
        {
            m_messenger.send(invoke, destinations[i]);
        }
     
        //invoke locally
        EndPoint endPoint = (EndPoint) m_localEndPoints.get(endPointName);
        endPoint.invokeLocal(call);                
    }
    
    
    public boolean isAwareOfEndPoint(String endPointName)
    {
        synchronized(m_endPointMutex)
        {
            return m_localEndPoints.containsKey(endPointName);
        }
    }
    
    public boolean isAwareOfImplementors(String endPointName)
    {
        if(!isAwareOfEndPoint(endPointName))
            return false;
        
        if( ((EndPoint) m_localEndPoints.get(endPointName)).hasImplementors())
            return true;
        
        return getNodesWithImplementors(endPointName).length != 0; 
    }
    
    private void assertEndPointExists(String name)
    {
        if(! isAwareOfEndPoint(name))
        {
            throw new IllegalStateException("Nothing known about endpoint:" + name);
        }
    }
    
    public void addImplementor(String name, Object implementor)
    {
        assertEndPointExists(name);
        
        EndPoint endPoint = (EndPoint) m_localEndPoints.get(name);
        if(!endPoint.hasImplementors())
        {
            m_messenger.broadcast(new HasEndPointImplementor(name));
        }
        endPoint.addImplementor(implementor);
    }
    
    public INode getLocalNode()
    {
        return m_messenger.getLocalNode();
    }
    
    public void flush()
    {
        m_messenger.flush();
    }

    
    
    public void removeImplementor(String name, Object implementor)
    {
        assertEndPointExists(name);
        
        EndPoint endPoint = (EndPoint) m_localEndPoints.get(name);
        endPoint.removeImplementor(implementor);
        
        if(!endPoint.hasImplementors())
        {
            m_messenger.broadcast(new NoLongerHasEndPointImplementor(name));
        }
    }
    
    public boolean isServer()
    {
        return m_messenger.isServer();
    }

    //only the server response to init messages
    //so create a special listener to listen for those messages
    private IMessageListener m_serverMessageListener = new IMessageListener()
    {
        public void messageReceived(Serializable msg, INode from)
        {            
            
            if(msg instanceof UnifiedInitRequest)
            {
                List localNodesWithImplementors = new ArrayList();
                EndPointCreated[] created = new EndPointCreated[m_localEndPoints.keySet().size()];
                Iterator names = m_localEndPoints.keySet().iterator();
                int i = 0;
                while(names.hasNext())
                {
                    String name = (String) names.next();
                    EndPoint endPoint = (EndPoint) m_localEndPoints.get(name);
                    
                    if(endPoint.hasImplementors())
                    {
                        localNodesWithImplementors.add(endPoint.getName());
                    }
                    
                    created[i] = new EndPointCreated(
                                       RemoteMethodCall.classesToString(endPoint.getClasses()), 
                                       name,
                                       endPoint.isSingleThreaded());
                    i++;
                }
                
                Map nodesWithImplementors = new HashMap(m_remoteNodesWithImplementors);
                nodesWithImplementors.put(m_messenger.getLocalNode(),localNodesWithImplementors );
                
                UnifiedInitMessage init = new UnifiedInitMessage(created, nodesWithImplementors);
                m_messenger.send(init, from);
            }
        }
    };
    
    private IMessageListener m_messageListener = new IMessageListener()
    {
        public void messageReceived(Serializable msg,final  INode from)
        {
            if(msg instanceof UnifiedInitMessage)
            {
                UnifiedInitMessage initMessage = (UnifiedInitMessage) msg;
                initFromRemoteData(from, initMessage);
                
            } else if(msg instanceof EndPointCreated)
            {
                EndPointCreated created = (EndPointCreated) msg;
                createEndPointInternal(created.name, RemoteMethodCall.stringsToClasses(created.classes), created.singleThreaded);
            } else if (msg instanceof EndPointDestroyed)
            {
                EndPointDestroyed destroyed = (EndPointDestroyed) msg;
                destroyEndPointInternal(destroyed.name);
            } else if (msg instanceof NoLongerHasEndPointImplementor)
            {
                synchronized(m_endPointMutex)
                {	
	                String name = ((NoLongerHasEndPointImplementor) msg).endPointName;
	                Collection current = (Collection) m_remoteNodesWithImplementors.get(from);
	                if(current == null)
	                {
	                    current = new HashSet();
	                    m_remoteNodesWithImplementors.put(from, current);
	                }
	                current.remove(name);
                }
            }else if (msg instanceof HasEndPointImplementor)
            {
                synchronized(m_endPointMutex)
                {
	                String name = ((HasEndPointImplementor) msg).endPointName;
	                Collection current = (Collection) m_remoteNodesWithImplementors.get(from);
	                if(current == null)
	                {
	                    current = new HashSet();
	                    m_remoteNodesWithImplementors.put(from, current);
	                }
	                current.add(name);
                }
            }
            else if (msg instanceof Invoke)
            {
                final Invoke invoke = (Invoke) msg;
                final EndPoint local = (EndPoint) m_localEndPoints.get(invoke.endPointName);

                //something a bit strange here, it may be the case 
                //that the endpoint was deleted locally
                //regardless, the other side is expecting our reply
                if(local == null)
                {
                    m_messenger.send(new InvocationResults(new ArrayList(), invoke.methodCallID), from);
                    return;
                }
                //we dont want to block the message thread, only one thread is reading messages
                //per connection, so run with out thread pool
                Runnable task = new Runnable() {
                    public void run()
                    {
                       List results = local.invokeLocal(invoke.call);
                       if(invoke.needReturnValues)
                       {
                           m_messenger.send(new InvocationResults(results, invoke.methodCallID), from);
                       }
                    }
                };
                
                m_threadPool.runTask(task);
            }
            //a remote machine is returning results
            else if(msg instanceof InvocationResults)
            {
                InvocationResults results = (InvocationResults) msg;
                
                //we need to find the GUID on the local vm
                //we synch on that object
                Iterator keys = m_methodCallWaitCount.keySet().iterator();
                GUID localGUID = null;
                while(keys.hasNext())
                {
                    localGUID = (GUID) keys.next();
                    if(localGUID.equals(results.methodCallID))
                    {
                        break;
                    }
                }
                synchronized(localGUID)
                {
                    if(!m_methodCallResults.containsKey(localGUID))
                    {
                        m_methodCallResults.put(localGUID, new ArrayList());
                    }
                    ((ArrayList) m_methodCallResults.get(localGUID)).addAll(results.results);
                    
                    int waitCount = ((Integer) m_methodCallWaitCount.get(localGUID)).intValue();
                    waitCount--;
                    if(waitCount == 0)
                    {
                        //remove to allow gc
                        m_methodCallWaitCount.remove(localGUID);
                        //wake up the waiter
                        localGUID.notifyAll();
                    }
                }
                
            }
                    
                
        }

        /**
         * @param from
         * @param initMessage
         */
        private void initFromRemoteData(final INode from, UnifiedInitMessage initMessage)
        {
            synchronized(m_endPointMutex)
            {
                //create end points
                for(int i = 0; i < initMessage.endPoints.length; i++)
                {
                    EndPointCreated endPointCreated = initMessage.endPoints[i];
                    createEndPointInternal(endPointCreated.name, RemoteMethodCall.stringsToClasses(endPointCreated.classes), endPointCreated.singleThreaded);
                }
                
                //initialize who has endpoints
                Iterator nodes = initMessage.nodesWithEndPoints.keySet().iterator();
                while(nodes.hasNext())
                {
                    INode node = (INode) nodes.next();
                    Collection initEndPoints = (Collection) initMessage.nodesWithEndPoints.get(node);
                    Collection currentEndPoints = (Collection) m_remoteNodesWithImplementors.get(from);
                    if(currentEndPoints == null)
                    {
                        currentEndPoints = new HashSet();
                        m_remoteNodesWithImplementors.put(from,currentEndPoints);
                    }
                    
                    currentEndPoints.addAll(initEndPoints);
                }
            }
            //release mutex lock before acquiring init lock
            synchronized(m_initLock)
            {
                m_initLock.notifyAll();
            }
        }
    };
    
    
    /**
     * Wait for the messenger to know about the given endpoint.
     * 
     * @param endPointName
     * @param timeout
     */
    public void waitForEndPoint(String endPointName, long timeoutMS)
    {
        if(timeoutMS <= 0)
            timeoutMS = Long.MAX_VALUE;
           
        long endTime = timeoutMS + System.currentTimeMillis();
        
        
        while(System.currentTimeMillis() < endTime && !isAwareOfEndPoint(endPointName))
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
    
    /**
     * Wait for the messenger to know about an implementor for a given endpoint
     * @param endPointName
     * @param timeoutMS
     */
    public void waitForImplementors(String endPointName, long timeoutMS)
    {
        if(timeoutMS <= 0)
            timeoutMS = Long.MAX_VALUE;
            
        long endTime = timeoutMS + System.currentTimeMillis();        
        
        while(System.currentTimeMillis() < endTime &&  !isAwareOfImplementors(endPointName))
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
    
}


/**
 * This is where the methods finally get called.
 * 
 * An endpoint contains the implementors for a given name that are local to this node.
 * 
 * You can invoke the method and get the results for all the implementors. 
 *
 * @author Sean Bridges
 */
class EndPoint
{
    private final String m_name;
    private final Class[] m_classes;
    private final Collection m_implementors = Collections.synchronizedList(new ArrayList());
    private final boolean m_singleThreaded;
    
    public EndPoint(final String name, final Class[] classes, boolean singleThreaded)
    {
        if(classes.length <= 0)
            throw new IllegalArgumentException("No classes defined");
        
        m_name = name;
        m_classes = classes;
        m_singleThreaded = singleThreaded;
    }
    
    public void addImplementor(Object implementor)
    {
        //check that implementor implements the correct interfaces
        for(int i = 0; i < m_classes.length; i++)
        {
            if(!m_classes[i].isAssignableFrom(implementor.getClass()))
                throw new IllegalArgumentException(m_classes[i] + " is not assignable from " + implementor.getClass());
        }
        
        m_implementors.add(implementor);
    }
    
    public boolean isSingleThreaded()
    {
        return m_singleThreaded;
    }
    
    public boolean hasImplementors()
    {
        return !m_implementors.isEmpty();
    }
    
    public int getLocalImplementorCount()
    {
        return m_implementors.size();
    }
    
    public void removeImplementor(Object implementor)
    {
        m_implementors.remove(implementor);
    }
    
    public String getName()
    {
        return m_name;
    }
    
    public Class[] getClasses()
    {
        return m_classes;
    }
    
    /*
     * @return a List of RemoteMethodCallResults
     */
    public List invokeLocal(RemoteMethodCall call)
    {
        if(m_implementors.isEmpty())
            return new ArrayList();

        if(m_singleThreaded)
        {
            synchronized(this)
            {
                return invokeMultiple(call);               
            }
        }
        else
            return invokeMultiple(call);	
               
        
    }
    
    /**
     * @param call
     * @param rVal
     */
    private List invokeMultiple(RemoteMethodCall call)
    {
        List results = new ArrayList(m_implementors.size());
        Iterator iter = new ArrayList(m_implementors).iterator();
        while(iter.hasNext())
        {
            Object implementor = iter.next();
            results.add(invokeSingle(call, implementor));
        }
        return results;
    }

    /**
     * @param call
     * @param implementor
     * @return
     */
    private RemoteMethodCallResults invokeSingle(RemoteMethodCall call, Object implementor)
    {
        Method method;
        try
        {
            method = implementor.getClass().getMethod(call.getMethodName(), call.getArgTypes());
            method.setAccessible(true);
        } catch (SecurityException e)
        {                
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        } catch (NoSuchMethodException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
        
        try
        {
            Object methodRVal = method.invoke(implementor, call.getArgs());
            return new RemoteMethodCallResults(methodRVal);
            
        } catch(InvocationTargetException e)
        {
            return new RemoteMethodCallResults(e.getTargetException());
        } catch(IllegalAccessException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
    }

    public boolean equivalent(EndPoint other)
    {
        if(other.m_singleThreaded != this.m_singleThreaded)
            return false;
        if(!other.m_name.equals(this.m_name))
            return false;
        if(!(other.m_classes.length == this.m_classes.length))
            return false;
        for(int i = 0; i < m_classes.length; i++)
        {
            if(!other.m_classes[i].equals(this.m_classes[i]))
                return false;
        }
        return true;
    }
    
}


//Misc messaging classes

//a request for initialization
//only the server should reply
class UnifiedInitRequest implements Serializable
{
    
}

//an end point has been created, we should follow
class EndPointCreated implements Serializable
{
    public final String[] classes;
    public final String name;
    public final boolean singleThreaded;
    
    
    public EndPointCreated(String[] classes, String name, boolean singleThreaded)
    {
        this.classes = classes;
        this.name = name;
        this.singleThreaded = singleThreaded;
    }
}

//and end point has been destroyed, we too should jump off that bridge
class EndPointDestroyed implements Serializable
{
    public final String name;
    
    public EndPointDestroyed(String name)
    {
        this.name = name;
    }
}

//someone now has an implementor for an endpoint
class HasEndPointImplementor implements Serializable
{
    public final String endPointName;

    
    
    public HasEndPointImplementor(String endPointName)
    {
        this.endPointName = endPointName;
    }
    
    public String toString()
    {
        return this.getClass().getName() + ":" + endPointName;
    }


}

//someone no longer has implementors for an endpoint
class NoLongerHasEndPointImplementor implements Serializable
{
    public final String endPointName;

    public NoLongerHasEndPointImplementor(String endPointName)
    {
        this.endPointName = endPointName;
    }
}

//enough info to initialize us
class UnifiedInitMessage implements Serializable
{
    public EndPointCreated[] endPoints;
    //maps INode -> Collection of strings representing the end points on that
    //node with implementors
    public Map nodesWithEndPoints;
   
    
    public UnifiedInitMessage(EndPointCreated[] endPoints, Map nodesWithEndPoints)
    {
        this.endPoints = endPoints;
        this.nodesWithEndPoints = nodesWithEndPoints;
    }
}

//someone wants us to invoke something locally
class Invoke implements Externalizable
{
    public GUID methodCallID;
    public boolean needReturnValues;
    public RemoteMethodCall call;
    public String endPointName;
        
    public Invoke()
    {
        
    }
    
    public String toString()
    {
        return "invoke on:" + endPointName + " method name:" + call.getMethodName() + " method call id:" + methodCallID;
    }
    
    public Invoke(GUID methodCallID, boolean needReturnValues, RemoteMethodCall call, String endPointName)
    {
        this.methodCallID = methodCallID;
        this.needReturnValues = needReturnValues;
        this.call = call;
        this.endPointName = endPointName;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        methodCallID = (GUID) in.readObject();
        needReturnValues = in.readBoolean();
        call = (RemoteMethodCall) in.readObject();
        endPointName = (String) in.readObject();
        
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(methodCallID);
        out.writeBoolean(needReturnValues);
        out.writeObject(call);
        out.writeObject(endPointName);
    }
    
}

//the results of a remote invocation
class InvocationResults implements Serializable
{
    public Collection results;
    public GUID methodCallID;
    
    public InvocationResults(Collection results, GUID methodCallID)
    {
        this.results = results;
        this.methodCallID = methodCallID;
    }
    
    public String toString()
    {
        return "Invocation results for method id:" + methodCallID + " number of results:" + results.size();
    }
    
}