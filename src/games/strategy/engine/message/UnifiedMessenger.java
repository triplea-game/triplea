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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import games.strategy.net.*;
import games.strategy.thread.ThreadPool;
import games.strategy.util.Match;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

/**
 * A messenger general enough that both Channel and Remote messenger can be
 * based on it.
 * 
 * @author Sean Bridges
 */
public class UnifiedMessenger
{
    
    private final static Logger s_logger = Logger.getLogger(UnifiedMessenger.class.getName());

    //a thread pool to run the invoke on
    private static ThreadPool m_threadPool = new ThreadPool(15, "UnifiedMessengerPool");

    //the messenger we are based on
    private final IMessenger m_messenger;

    //maps String -> EndPoint
    //these are the end points we know about
    //some of them may not have implementors
    private final Map<String,EndPoint> m_localEndPoints = new ConcurrentHashMap<String,EndPoint>();

    //maps INode - collection of endpoints as string that the INode has
    // implementors for
    private final Map<INode, Collection<String>> m_remoteNodesWithImplementors = new HashMap<INode, Collection<String>>();

    //maps GUID-> synchronized list of RemoteMethodResults
    private final Map<GUID, List<RemoteMethodCallResults>> m_methodCallResults = new ConcurrentHashMap<GUID, List<RemoteMethodCallResults>>();

    //maps GUID -> CountDownLatch of clients we are awaiting responses from
    private final Map<GUID, CountDownLatch> m_methodCallWaitCount = new ConcurrentHashMap<GUID, CountDownLatch>();

    //maps GUID -> CountDownLatch of clients we are awaiting responses from
    private final Map<GUID, List<INode>> m_methodCallWaitNodes = new ConcurrentHashMap<GUID, List<INode>>();


    //synchronize on this to wait for the init data to arrive
    private final CountDownLatch m_initCountDownLatch = new CountDownLatch(1);

    //lock on this for modifications to create or remove local end points
    private final Object m_endPointmutex = new Object();

    //used to synchronize access to m_remoteNodesWithImplementors 
    private final Object m_nodesWithImplementorsMutex = new Object();

    
    private final IMessengerErrorListener m_messengerErrorListener = new IMessengerErrorListener()
    {
    
        public void messengerInvalid(IMessenger messenger, Exception reason, List unsent)
        {
            UnifiedMessenger.this.messengerInvalid();
        }
    
        public void connectionLost(INode node, Exception reason, List unsent)
        {}
    
    };
    
    private final IConnectionChangeListener m_connectionChangeListener = new IConnectionChangeListener()
    {
        public void connectionRemoved(INode to)
        {
            UnifiedMessenger.this.connectionRemoved(to);
        }
    
        public void connectionAdded(INode to)
        {}
    
    };
    /**
     * @param messenger
     */
    public UnifiedMessenger(final IMessenger messenger)
    {
        m_messenger = messenger;
        m_messenger.addMessageListener(m_messageListener);
        m_messenger.addConnectionChangeListener(m_connectionChangeListener);
        m_messenger.addErrorListener(m_messengerErrorListener);

        if (m_messenger.isServer())
        {
            //we are responsible for initializing others
            //add a listener to deal with it
            m_messenger.addMessageListener(m_serverMessageListener);
        } else
        {

            m_messenger.broadcast(new UnifiedInitRequest());
            try
            {
                m_initCountDownLatch.await();
            } catch (InterruptedException e)
            {
            }
        }
    }

    protected void messengerInvalid()
    {
        synchronized(m_nodesWithImplementorsMutex)
        {
            m_remoteNodesWithImplementors.clear();
        }
        synchronized(m_endPointmutex)
        {
            m_localEndPoints.clear();
        }
        
        //we need to wake up threads that have been invoked
        for(GUID methodCallId : m_methodCallWaitNodes.keySet() )
        {
            List<INode> nodes = m_methodCallWaitNodes.get(methodCallId);
            for(int i = 0; i < nodes.size(); i++)
            {
                m_methodCallResults.get(methodCallId).add(new RemoteMethodCallResults(new ConnectionLostException("Connection Lost")) );
                m_methodCallWaitCount.get(methodCallId).countDown();
            }
            nodes.clear();
        }
    }

    private void connectionRemoved(INode lostNode)
    {
        synchronized(m_nodesWithImplementorsMutex)
        {
            m_remoteNodesWithImplementors.remove(lostNode);
        }
        
        synchronized(m_endPointmutex)
        {
            for(String endPointName : m_localEndPoints.keySet())
            {
                EndPoint endPoint = m_localEndPoints.get(endPointName);
                if(lostNode.equals(endPoint.getPossessiveNode()))
                {
                    m_localEndPoints.remove(endPointName);
                }
            }
        }
        
        //we need to wake up threads that are waiting
        for(GUID methodCallId : m_methodCallWaitNodes.keySet() )
        {
            List<INode> nodes = m_methodCallWaitNodes.get(methodCallId);
            if(nodes.contains(lostNode))
            {
                nodes.remove(lostNode);
                //add our results
                m_methodCallResults.get(methodCallId).add(new RemoteMethodCallResults(new ConnectionLostException("Connection Lost")) );
                m_methodCallWaitCount.get(methodCallId).countDown();
            }
        }
        
    }

    /**
     * Create an end point. Creating an equivalent end point multiple times is
     * ok, subsequent calls will be ignored if the end point is the same, but an
     * error will be generated if an attemot to change the endpoint definition
     * through a create call.
     * 
     * @param name -
     *            the name of the endpoint
     * @param classes
     * @param singleThreaded -
     *            if more than one thread can be calling methods on end points
     *            at a time.
     */
    public void createEndPoint(String name, Class[] classes, boolean singleThreaded, boolean possesive)
    {
        INode possesiveNode;
        if(possesive)
            possesiveNode = getLocalNode();
        else
            possesiveNode = null;
        
        createEndPointInternal(name, classes, singleThreaded, possesiveNode);
        
        m_messenger.broadcast(new EndPointCreated(RemoteMethodCall.classesToString(classes, null), name, singleThreaded, possesiveNode));
    }

    private void createEndPointInternal(String name, Class[] classes, boolean singleThreaded, INode possesiveNode)
    {
        EndPoint endPoint = new EndPoint(name, classes, singleThreaded, possesiveNode);
        synchronized (m_endPointmutex)
        {
            //if we already have it, make sure its the same classes
            if (m_localEndPoints.containsKey(name))
            {
                //make sure they are equivalent
                EndPoint existing = m_localEndPoints.get(name);
                if (!existing.equivalent(endPoint))
                    throw new IllegalStateException("End point already exists with different types or threading");
            } else
            {
                //we dont have it
                //add it

                m_localEndPoints.put(name, endPoint);
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
        synchronized (m_endPointmutex)
        {
            m_localEndPoints.remove(name);
            Iterator<Collection<String>> remotes = m_remoteNodesWithImplementors.values().iterator();
            while (remotes.hasNext())
            {
                Collection coll = remotes.next();
                coll.remove(name);
            }
        }
    }

    private INode[] getRemoteNodesWithImplementors(final String endPointName)
    {
        Match<Collection<String>> containsEndPoint = new Match<Collection<String>>()
        {
            public boolean match(Collection<String> o)
            {
                return o.contains(endPointName);
            }
        };

        Set<INode> matching;

        synchronized (m_nodesWithImplementorsMutex)
        {
            matching = Match.getKeysWhereValueMatch(m_remoteNodesWithImplementors, containsEndPoint);
        }

        return matching.toArray(new INode[matching.size()]);
    }

    public Class[] getTypes(String endPointName)
    {
        return getLocalEndPointOrThrow(endPointName).getClasses();
    }

    public int getLocalImplementorCount(String endPointName)
    {
        return getLocalEndPointOrThrow(endPointName).getLocalImplementorCount();
    }

    /**
     * Invoke and wait for all implementors on all vms to finish executing.
     *  
     */
    public RemoteMethodCallResults[] invokeAndWait(String endPointName, RemoteMethodCall remoteCall)
    {
        EndPoint endPoint = m_localEndPoints.get(endPointName);

        //prepatory to anything else...
        //generate a unique id
        final GUID methodCallID = new GUID();

        //who do we call
        final INode[] remote = getRemoteNodesWithImplementors(endPointName);
        CountDownLatch latch = new CountDownLatch(remote.length);

        if (remote.length > 0)
        {
            //these are the nodes we are waiting for
            m_methodCallWaitNodes.put(methodCallID, Collections.synchronizedList(new ArrayList<INode>(Arrays.asList(remote))));
            
            //we need to indicate how many returns we are waiting for
            m_methodCallWaitCount.put(methodCallID, latch);
            //the list should be synchronized since we may add to it in
            // multiple threads
            m_methodCallResults.put(methodCallID, Collections.synchronizedList(new ArrayList<RemoteMethodCallResults>()));
        }

        //invoke remotely
        Invoke invoke = new Invoke(methodCallID, true, remoteCall);
        for (int i = 0; i < remote.length; i++)
        {
            m_messenger.send(invoke, remote[i]);
        }

        //invoke locally, we want to do this after remote invocation, since the
        // remote calls and
        //the local calls may execute concurrently

        List<RemoteMethodCallResults> results;
        if(endPoint != null)
            results = endPoint.invokeLocal(remoteCall, endPoint.takeANumber(), m_messenger.getLocalNode());
        else
            results = Collections.emptyList();

        //wait for the remote calls to finish
        if (remote.length > 0)
        {
            s_logger.log(Level.FINER, "Waiting for method:" + remoteCall.getMethodName() + " for remote name:" + remoteCall.getRemoteName()
                    + " with id:" + methodCallID);
            try
            {
                latch.await();
            } catch (InterruptedException e)
            {
                s_logger.log(Level.WARNING, e.getMessage());
            }

            s_logger.log(Level.FINER, "Method returned:" + remoteCall.getMethodName() + " for remote name:" + remoteCall.getRemoteName()
                    + " with id:" + methodCallID);

            results.addAll(m_methodCallResults.get(methodCallID));
            m_methodCallResults.remove(methodCallID);
            m_methodCallWaitCount.remove(methodCallID);
            m_methodCallWaitNodes.remove(methodCallID);

        }

        return results.toArray(new RemoteMethodCallResults[results.size()]);
    }

    /**
     * invoke without waiting for remote nodes to respond
     */
    public void invoke(final String endPointName, final RemoteMethodCall call)
    {
        EndPoint endPoint = getLocalEndPointOrThrow(endPointName);

        //invoke remotely first, allows concurrent execution
        //with local invocation
        INode[] destinations = getRemoteNodesWithImplementors(endPointName);

        Invoke invoke = new Invoke(null, false, call);
        for (int i = 0; i < destinations.length; i++)
        {
            m_messenger.send(invoke, destinations[i]);
        }

        //invoke locally

        endPoint.invokeLocal(call, endPoint.takeANumber(), m_messenger.getLocalNode());
    }

    /**
     * Do we know about the given end point? <br>
     * We may or may not have implementors for this endpoint locally.
     */
    public boolean isAwareOfEndPoint(String endPointName)
    {
        return m_localEndPoints.containsKey(endPointName);
    }

    /**
     * Do we know of any implementors for this endpoint. <br>
     * The implementors may or may not be local.
     */
    public boolean isAwareOfImplementors(String endPointName)
    {
        synchronized (m_endPointmutex)
        {
            //if we dont know about the endpoint, we wont know about
            // implementors
            if (!isAwareOfEndPoint(endPointName))
                return false;

            //do we have anything local?
            if (m_localEndPoints.get(endPointName).hasImplementors())
                return true;
        }
  
        return getRemoteNodesWithImplementors(endPointName).length != 0;
        
    }

    private EndPoint getLocalEndPointOrThrow(String name)
    {
        EndPoint rVal = m_localEndPoints.get(name);
        if (rVal == null)
        {
            throw new IllegalStateException("Nothing known about endpoint:" + name);
        }
        return rVal;
    }

    public void addImplementor(String name, Object implementor)
    {
        EndPoint endPoint = getLocalEndPointOrThrow(name);

        boolean broadcast = endPoint.addImplementor(implementor);
        if (broadcast)
            m_messenger.broadcast(new HasEndPointImplementor(name));
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
        EndPoint endPoint = getLocalEndPointOrThrow(name);

        boolean broadcast = endPoint.removeImplementor(implementor);

        if (broadcast)
            m_messenger.broadcast(new NoLongerHasEndPointImplementor(name));

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

            if (msg instanceof UnifiedInitRequest)
            {
                UnifiedInitMessage init;
                List<String> localNodesWithImplementors = new ArrayList<String>();
                EndPointCreated[] created;
                synchronized (m_endPointmutex)
                {
                    created = new EndPointCreated[m_localEndPoints.keySet().size()];
                    Iterator<String> names = m_localEndPoints.keySet().iterator();
                    int i = 0;
                    while (names.hasNext())
                    {
                        String name = names.next();
                        EndPoint endPoint = m_localEndPoints.get(name);

                        if (endPoint.hasImplementors())
                        {
                            localNodesWithImplementors.add(endPoint.getName());
                        }

                        created[i] = new EndPointCreated(RemoteMethodCall.classesToString(endPoint.getClasses(), null), name, endPoint.isSingleThreaded(), endPoint.getPossessiveNode());
                        i++;
                    }
                }
                synchronized (m_nodesWithImplementorsMutex)
                {
                    Map<INode, Collection<String>> nodeWithImplementors = new HashMap<INode, Collection<String>>();
                    Iterator<INode> iter = m_remoteNodesWithImplementors.keySet().iterator();
                    while (iter.hasNext())
                    {
                        //we need to copy each collection here
                        //otherwise it may be modified while we are sending
                        INode node = iter.next();
                        Collection<String> values = new ArrayList<String>(m_remoteNodesWithImplementors.get(node));
                        nodeWithImplementors.put(node, values);
                        
                    }
                    
                    nodeWithImplementors.put(m_messenger.getLocalNode(), localNodesWithImplementors);
                    init = new UnifiedInitMessage(created, nodeWithImplementors);
                }

                m_messenger.send(init, from);

            }

        }

    };

    private IMessageListener m_messageListener = new IMessageListener()
    {
        public void messageReceived(Serializable msg, final INode from)
        {
            if (msg instanceof UnifiedInitMessage)
            {
                UnifiedInitMessage initMessage = (UnifiedInitMessage) msg;
                initFromRemoteData(from, initMessage);

            } else if (msg instanceof EndPointCreated)
            {
                EndPointCreated created = (EndPointCreated) msg;
                createEndPointInternal(created.name, RemoteMethodCall.stringsToClasses(created.classes, null), created.singleThreaded, created.possesiveNode);
            } else if (msg instanceof EndPointDestroyed)
            {
                EndPointDestroyed destroyed = (EndPointDestroyed) msg;
                destroyEndPointInternal(destroyed.name);
            } else if (msg instanceof NoLongerHasEndPointImplementor)
            {
                String name = ((NoLongerHasEndPointImplementor) msg).endPointName;
                synchronized (m_nodesWithImplementorsMutex)
                {
                    Collection current = m_remoteNodesWithImplementors.get(from);
                    if (current != null)
                        current.remove(name);
                }
            } else if (msg instanceof HasEndPointImplementor)
            {
                String name = ((HasEndPointImplementor) msg).endPointName;
                synchronized (m_nodesWithImplementorsMutex)
                {
                    Collection<String> current = m_remoteNodesWithImplementors.get(from);
                    if (current == null)
                    {
                        current = new HashSet<String>();
                        m_remoteNodesWithImplementors.put(from, current);
                    }
                    current.add(name);
                }
            } else if (msg instanceof Invoke)
            {
                final Invoke invoke = (Invoke) msg;
                EndPoint local = m_localEndPoints.get(invoke.call.getRemoteName());

                //something a bit strange here, it may be the case
                //that the endpoint was deleted locally
                //regardless, the other side is expecting our reply
                if (local == null)
                {
                    m_messenger.send(new InvocationResults(new ArrayList<RemoteMethodCallResults>(), invoke.methodCallID), from);
                    return;
                }

                //very important
                //we are guaranteed that here messages will be
                //read in the same order that they are sent from the client
                //however, once we delegate to the thread pool, there is no
                //guarantee that the thread pool task will run before
                //we get the next message notification
                //get the number for the invocation here
                final long methodRunNumber = local.takeANumber();
                //we dont want to block the message thread, only one thread is
                // reading messages
                //per connection, so run with out thread pool
                final EndPoint localFinal = local;
                Runnable task = new Runnable()
                {
                    public void run()
                    {
                        List<RemoteMethodCallResults> results = localFinal.invokeLocal(invoke.call, methodRunNumber, from);
                        if (invoke.needReturnValues)
                        {
                            m_messenger.send(new InvocationResults(results, invoke.methodCallID), from);
                        }
                    }
                };

                m_threadPool.runTask(task);

            }
            //a remote machine is returning results
            else if (msg instanceof InvocationResults)
            {
                InvocationResults results = (InvocationResults) msg;

                GUID methodID = results.methodCallID;

                //both of these should already be populated
                //this list should be a synchronized list so we can do the add all
                m_methodCallResults.get(methodID).addAll(results.results);
                m_methodCallWaitNodes.get(methodID).remove(from);
                m_methodCallWaitCount.get(methodID).countDown();

            }
        }

        /**
         * @param from
         * @param initMessage
         */
        private void initFromRemoteData(final INode from, UnifiedInitMessage initMessage)
        {
            //create end points
            for (int i = 0; i < initMessage.endPoints.length; i++)
            {
                EndPointCreated endPointCreated = initMessage.endPoints[i];
                createEndPointInternal(endPointCreated.name, RemoteMethodCall.stringsToClasses(endPointCreated.classes, null),
                        endPointCreated.singleThreaded, endPointCreated.possesiveNode);
            }

            //initialize who has endpoints
            Iterator<INode> nodes = initMessage.nodesWithEndPoints.keySet().iterator();
            synchronized (m_nodesWithImplementorsMutex)
            {
                while (nodes.hasNext())
                {
                    INode node = nodes.next();
                    Collection<String> initEndPoints = initMessage.nodesWithEndPoints.get(node);
                    Collection<String> currentEndPoints = m_remoteNodesWithImplementors.get(node);
                    if (currentEndPoints == null)
                    {
                        currentEndPoints = new HashSet<String>();
                        m_remoteNodesWithImplementors.put(node, currentEndPoints);
                    }

                    currentEndPoints.addAll(initEndPoints);
                }
            }

            m_initCountDownLatch.countDown();
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
        //dont use Long.MAX_VALUE since that will overflow
        if (timeoutMS <= 0)
            timeoutMS = Integer.MAX_VALUE;

        long endTime = timeoutMS + System.currentTimeMillis();

        while (System.currentTimeMillis() < endTime && !isAwareOfEndPoint(endPointName))
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
     * 
     * @param endPointName
     * @param timeoutMS
     */
    public void waitForImplementors(String endPointName, long timeoutMS)
    {
        //dont use Long.MAX_VALUE since that will overflow
        if (timeoutMS <= 0)
            timeoutMS = Integer.MAX_VALUE;

        long endTime = timeoutMS + System.currentTimeMillis();

        while (System.currentTimeMillis() < endTime && !isAwareOfImplementors(endPointName))
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
    
    public void dumpState(PrintStream stream)
    {
        synchronized(m_endPointmutex)
        {
            stream.println("Local Endpoints:" + m_localEndPoints);
        }
        
        synchronized(m_nodesWithImplementorsMutex)
        {
            stream.println("Remote nodes with implementors:" +  m_remoteNodesWithImplementors);
        }
        
    }

    public void waitForAllJobs()
    {
        m_threadPool.waitForAll();
        
    }
    

}

/**
 * This is where the methods finally get called.
 * 
 * An endpoint contains the implementors for a given name that are local to this
 * node.
 * 
 * You can invoke the method and get the results for all the implementors.
 * 
 * @author Sean Bridges
 */

class EndPoint
{
    //the next number we are going to give
    private final AtomicLong m_nextGivenNumber = new AtomicLong();
    //the next number we can run
    private long m_currentRunnableNumber = 0;

    private final Object m_numberMutext = new Object();
    private final Object m_implementorsMutext = new Object();

    private final String m_name;
    private final Class[] m_classes;
    private final List<Object> m_implementors = new ArrayList<Object>();
    private final boolean m_singleThreaded;
    //we are bound to this node, when the node is removed
    //from the network, we are removed as well
    //if null, we are not bound to a single node
    private final INode m_possesiveNode;

    public EndPoint(final String name, final Class[] classes, boolean singleThreaded, INode possessiveNode)
    {
        if (classes.length <= 0)
            throw new IllegalArgumentException("No classes defined");

        m_possesiveNode = possessiveNode;
        m_name = name;
        m_classes = classes;
        m_singleThreaded = singleThreaded;
    }

    public INode getPossessiveNode()
    {
        return m_possesiveNode;
    }
    
    public long takeANumber()
    {
        return m_nextGivenNumber.getAndIncrement();
    }

    private void waitTillCanBeRun(long aNumber)
    {
        synchronized (m_numberMutext)
        {
            while (aNumber > m_currentRunnableNumber)
            {
                try
                {
                    m_numberMutext.wait();
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void releaseNumber()
    {
        synchronized (m_numberMutext)
        {
            m_currentRunnableNumber++;
            m_numberMutext.notifyAll();
        }
    }

    /**
     * 
     * @return is this the first implementor
     */
    @SuppressWarnings("unchecked")
    public boolean addImplementor(Object implementor)
    {
        //check that implementor implements the correct interfaces
        for (int i = 0; i < m_classes.length; i++)
        {
            if (!m_classes[i].isAssignableFrom(implementor.getClass()))
                throw new IllegalArgumentException(m_classes[i] + " is not assignable from " + implementor.getClass());
        }

        synchronized (m_implementorsMutext)
        {
            boolean rVal = m_implementors.isEmpty();
            m_implementors.add(implementor);
            return rVal;

        }
    }

    public boolean isSingleThreaded()
    {
        return m_singleThreaded;
    }

    public boolean hasImplementors()
    {
        synchronized (m_implementorsMutext)
        {
            return !m_implementors.isEmpty();
        }
    }

    public int getLocalImplementorCount()
    {
        synchronized (m_implementorsMutext)
        {
            return m_implementors.size();
        }
    }

    /**
     * 
     * @return - we have no more implementors
     */
    public boolean removeImplementor(Object implementor)
    {
        synchronized (m_implementorsMutext)
        {
            m_implementors.remove(implementor);
            return m_implementors.isEmpty();
        }
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
     * @param number - like the number you get in a bank line, if we are single
     * threaded, then the method will not run until the number comes up. Acquire
     * with getNumber() @return a List of RemoteMethodCallResults
     */
    public List<RemoteMethodCallResults> invokeLocal(final RemoteMethodCall call, long number, INode messageOriginator)
    {

        try
        {
            if (m_singleThreaded)
            {
                waitTillCanBeRun(number);

                return invokeMultiple(call, messageOriginator);

            } else
                return invokeMultiple(call,messageOriginator);
        } finally
        {
            releaseNumber();
        }

    }

    /**
     * @param call
     * @param rVal
     */
    private List<RemoteMethodCallResults> invokeMultiple(RemoteMethodCall call, INode messageOriginator)
    {
        //copy the implementors
        List<Object> implementorsCopy;
        synchronized (m_implementorsMutext)
        {
            implementorsCopy = new ArrayList<Object>(m_implementors);
        }

        List<RemoteMethodCallResults> results = new ArrayList<RemoteMethodCallResults>(implementorsCopy.size());
        Iterator<Object> iter = implementorsCopy.iterator();
        while (iter.hasNext())
        {
            Object implementor = iter.next();
            results.add(invokeSingle(call, implementor,messageOriginator));
        }
        return results;
    }

    /**
     * @param call
     * @param implementor
     * @return
     */
    private RemoteMethodCallResults invokeSingle(RemoteMethodCall call, Object implementor, INode messageOriginator)
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

        MessageContext.setSenderNodeForThread(messageOriginator);
        try
        {
            Object methodRVal = method.invoke(implementor, call.getArgs());
            return new RemoteMethodCallResults(methodRVal);

        } catch (InvocationTargetException e)
        {
            return new RemoteMethodCallResults(e.getTargetException());
        } catch (IllegalAccessException e)
        {
            //this shouldnt happen
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
        finally
        {
            MessageContext.setSenderNodeForThread(null);
        }
    }

    public boolean equivalent(EndPoint other)
    {
        if (other.m_singleThreaded != this.m_singleThreaded)
            return false;
        if (!other.m_name.equals(this.m_name))
            return false;
        if (!(other.m_classes.length == this.m_classes.length))
            return false;
        for (int i = 0; i < m_classes.length; i++)
        {
            if (!other.m_classes[i].equals(this.m_classes[i]))
                return false;
        }
        return true;
    }
    
    public String toString()
    {
        return "Name:" +  m_name + " singleThreaded:" + m_singleThreaded + " implementors:" + m_implementors;
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
    //if not null, then this end point is tied to the given node
    //if the given node is removed from the network, the end point should be destroyed
    public final INode possesiveNode;

    public EndPointCreated(String[] classes, String name, boolean singleThreaded, INode posessiveNode)
    {
        this.classes = classes;
        this.name = name;
        this.singleThreaded = singleThreaded;
        this.possesiveNode = posessiveNode;
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

    public String toString()
    {
        return "EndPointDestroyed:" + name;
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
    public Map<INode, Collection<String>> nodesWithEndPoints;

    public UnifiedInitMessage(EndPointCreated[] endPoints, Map<INode, Collection<String>> nodesWithEndPoints)
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

    public Invoke()
    {

    }

    public String toString()
    {
        return "invoke on:" + call.getRemoteName() + " method name:" + call.getMethodName() + " method call id:" + methodCallID;
    }

    public Invoke(GUID methodCallID, boolean needReturnValues, RemoteMethodCall call)
    {
        this.methodCallID = methodCallID;
        this.needReturnValues = needReturnValues;
        this.call = call;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        methodCallID = (GUID) in.readObject();
        needReturnValues = in.readBoolean();
        call = (RemoteMethodCall) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(methodCallID);
        out.writeBoolean(needReturnValues);
        out.writeObject(call);
    }

}

//the results of a remote invocation

class InvocationResults implements Externalizable
{
    public Collection<RemoteMethodCallResults> results;
    public GUID methodCallID;

    public InvocationResults()
    {
    	
    }
    
    public InvocationResults(Collection<RemoteMethodCallResults> results, GUID methodCallID)
    {
        this.results = results;
        this.methodCallID = methodCallID;
    }

    public String toString()
    {
        return "Invocation results for method id:" + methodCallID + " number of results:" + results.size();
    }

	public void writeExternal(ObjectOutput out) throws IOException 
	{
		 out.writeObject(results);
		 out.writeObject(methodCallID);
		
	}

	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException 
	{
		results = (Collection<RemoteMethodCallResults>) in.readObject();
		methodCallID = (GUID) in.readObject();
		
	}

}

