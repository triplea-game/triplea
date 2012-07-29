/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.message;

import games.strategy.net.GUID;
import games.strategy.net.IMessageListener;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.thread.ThreadPool;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A messenger general enough that both Channel and Remote messenger can be
 * based on it.
 * 
 * @author Sean Bridges
 */
public class UnifiedMessenger
{
	private final static Logger s_logger = Logger.getLogger(UnifiedMessenger.class.getName());
	// a thread pool to run the invoke on
	private static ThreadPool m_threadPool = new ThreadPool(15, "UnifiedMessengerPool");
	// the messenger we are based on
	private final IMessenger m_messenger;
	// lock on this for modifications to create or remove local end points
	private final Object m_endPointMutex = new Object();
	// maps String -> EndPoint
	// these are the end points that
	// have local implementors
	private final Map<String, EndPoint> m_localEndPoints = new HashMap<String, EndPoint>();
	private final Object m_pendingLock = new Object();
	// threads wait on these latches for the hub to return invocations
	// the latch should be removed from the map when you countdown the last result
	// access should be synchronized on m_pendingLock
	private final Map<GUID, CountDownLatch> m_pendingInvocations = new HashMap<GUID, CountDownLatch>();
	// after the remote has invoked, the results are placed here
	// access should be synchronized on m_pendingLock
	private final Map<GUID, RemoteMethodCallResults> m_results = new HashMap<GUID, RemoteMethodCallResults>();
	// only non null for the server
	private UnifiedMessengerHub m_hub;
	private final IMessengerErrorListener m_messengerErrorListener = new IMessengerErrorListener()
	{
		public void messengerInvalid(final IMessenger messenger, final Exception reason)
		{
			UnifiedMessenger.this.messengerInvalid();
		}
	};
	
	/**
	 * @param messenger
	 */
	public UnifiedMessenger(final IMessenger messenger)
	{
		m_messenger = messenger;
		m_messenger.addMessageListener(m_messageListener);
		m_messenger.addErrorListener(m_messengerErrorListener);
		if (m_messenger.isServer())
		{
			m_hub = new UnifiedMessengerHub(m_messenger, this);
		}
	}
	
	UnifiedMessengerHub getHub()
	{
		return m_hub;
	}
	
	private void messengerInvalid()
	{
		synchronized (m_pendingLock)
		{
			for (final GUID id : m_pendingInvocations.keySet())
			{
				final CountDownLatch latch = m_pendingInvocations.remove(id);
				latch.countDown();
				m_results.put(id, new RemoteMethodCallResults(new ConnectionLostException("Connection Lost")));
			}
		}
	}
	
	/**
	 * Invoke and wait for all implementors on all vms to finish executing.
	 * 
	 */
	public RemoteMethodCallResults invokeAndWait(final String endPointName, final RemoteMethodCall remoteCall)
	{
		EndPoint local;
		synchronized (m_endPointMutex)
		{
			local = m_localEndPoints.get(endPointName);
		}
		if (local == null)
			return invokeAndWaitRemote(remoteCall);
		// we have the implementor here, just invoke it
		else
		{
			final long number = local.takeANumber();
			final List<RemoteMethodCallResults> results = local.invokeLocal(remoteCall, number, getLocalNode());
			if (results.size() == 0)
				throw new RemoteNotFoundException("Not found:" + endPointName);
			if (results.size() > 1)
				throw new IllegalStateException("Too many implementors, got back:" + results);
			return results.get(0);
		}
	}
	
	private RemoteMethodCallResults invokeAndWaitRemote(final RemoteMethodCall remoteCall)
	{
		// prepatory to anything else...
		// generate a unique id
		final GUID methodCallID = new GUID();
		final CountDownLatch latch = new CountDownLatch(1);
		synchronized (m_pendingLock)
		{
			m_pendingInvocations.put(methodCallID, latch);
		}
		// invoke remotely
		final Invoke invoke = new HubInvoke(methodCallID, true, remoteCall);
		send(invoke, m_messenger.getServerNode());
		if (s_logger.isLoggable(Level.FINER))
		{
			s_logger.log(Level.FINER, "Waiting for method:" + remoteCall.getMethodName() + " for remote name:" + remoteCall.getRemoteName() + " with id:" + methodCallID);
		}
		try
		{
			latch.await();
		} catch (final InterruptedException e)
		{
			s_logger.log(Level.WARNING, e.getMessage());
		}
		if (s_logger.isLoggable(Level.FINER))
		{
			s_logger.log(Level.FINER, "Method returned:" + remoteCall.getMethodName() + " for remote name:" + remoteCall.getRemoteName() + " with id:" + methodCallID);
		}
		RemoteMethodCallResults results;
		// the countdownlatch map will be cleared when the results come in
		synchronized (m_pendingLock)
		{
			results = m_results.remove(methodCallID);
			if (results == null)
				throw new IllegalStateException("No results");
		}
		return results;
	}
	
	/**
	 * invoke without waiting for remote nodes to respond
	 */
	public void invoke(final String endPointName, final RemoteMethodCall call)
	{
		// send the remote invocation
		final Invoke invoke = new HubInvoke(null, false, call);
		send(invoke, m_messenger.getServerNode());
		// invoke locally
		EndPoint endPoint;
		synchronized (m_endPointMutex)
		{
			endPoint = m_localEndPoints.get(endPointName);
		}
		if (endPoint != null)
		{
			final long number = endPoint.takeANumber();
			final List<RemoteMethodCallResults> results = endPoint.invokeLocal(call, number, getLocalNode());
			for (final RemoteMethodCallResults r : results)
			{
				if (r.getException() != null)
				{
					// don't swallow errors
					s_logger.log(Level.WARNING, r.getException().getMessage(), r.getException());
				}
			}
		}
	}
	
	public void addImplementor(final RemoteName endPointDescriptor, final Object implementor, final boolean singleThreaded)
	{
		if (!endPointDescriptor.getClazz().isAssignableFrom(implementor.getClass()))
			throw new IllegalArgumentException(implementor + " does not implement " + endPointDescriptor.getClazz());
		final EndPoint endPoint = getLocalEndPointOrCreate(endPointDescriptor, singleThreaded);
		endPoint.addImplementor(implementor);
	}
	
	public INode getLocalNode()
	{
		return m_messenger.getLocalNode();
	}
	
	/**
	 * Get the 1 and only implementor for the endpoint. throws an exception if there are not exctly 1 implementors
	 */
	public Object getImplementor(final String name)
	{
		synchronized (m_endPointMutex)
		{
			final EndPoint endPoint = m_localEndPoints.get(name);
			return endPoint.getFirstImplementor();
		}
	}
	
	void removeImplementor(final String name, final Object implementor)
	{
		EndPoint endPoint;
		synchronized (m_endPointMutex)
		{
			endPoint = m_localEndPoints.get(name);
			if (endPoint == null)
				throw new IllegalStateException("No end point for:" + name);
			if (implementor == null)
				throw new IllegalArgumentException("null implementor");
			final boolean noneLeft = endPoint.removeImplementor(implementor);
			if (noneLeft)
			{
				m_localEndPoints.remove(name);
				send(new NoLongerHasEndPointImplementor(name), m_messenger.getServerNode());
			}
		}
	}
	
	private EndPoint getLocalEndPointOrCreate(final RemoteName endPointDescriptor, final boolean singleThreaded)
	{
		EndPoint endPoint;
		synchronized (m_endPointMutex)
		{
			if (m_localEndPoints.containsKey(endPointDescriptor.getName()))
				return m_localEndPoints.get(endPointDescriptor.getName());
			endPoint = new EndPoint(endPointDescriptor.getName(), endPointDescriptor.getClazz(), singleThreaded);
			m_localEndPoints.put(endPointDescriptor.getName(), endPoint);
		}
		final HasEndPointImplementor msg = new HasEndPointImplementor(endPointDescriptor.getName());
		send(msg, m_messenger.getServerNode());
		return endPoint;
	}
	
	private void send(final Serializable msg, final INode to)
	{
		if (m_messenger.getLocalNode().equals(to))
		{
			m_hub.messageReceived(msg, getLocalNode());
		}
		else
		{
			m_messenger.send(msg, to);
		}
	}
	
	public boolean isServer()
	{
		return m_messenger.isServer();
	}
	
	/**
	 * Wait for the messenger to know about the given endpoint.
	 * 
	 * @param endPointName
	 * @param timeout
	 */
	public void waitForLocalImplement(final String endPointName, long timeoutMS)
	{
		// dont use Long.MAX_VALUE since that will overflow
		if (timeoutMS <= 0)
			timeoutMS = Integer.MAX_VALUE;
		final long endTime = timeoutMS + System.currentTimeMillis();
		while (System.currentTimeMillis() < endTime && !hasLocalEndPoint(endPointName))
		{
			try
			{
				Thread.sleep(50);
			} catch (final InterruptedException e)
			{
				// whats a devloper to do
			}
		}
	}
	
	private boolean hasLocalEndPoint(final String endPointName)
	{
		synchronized (m_endPointMutex)
		{
			return m_localEndPoints.containsKey(endPointName);
		}
	}
	
	int getLocalEndPointCount(final RemoteName descriptor)
	{
		synchronized (m_endPointMutex)
		{
			if (!m_localEndPoints.containsKey(descriptor.getName()))
				return 0;
			return m_localEndPoints.get(descriptor.getName()).getLocalImplementorCount();
		}
	}
	
	private final IMessageListener m_messageListener = new IMessageListener()
	{
		public void messageReceived(final Serializable msg, final INode from)
		{
			UnifiedMessenger.this.messageReceived(msg, from);
		}
	};
	
	private void assertIsServer(final INode from)
	{
		if (!from.equals(m_messenger.getServerNode()))
			throw new IllegalStateException("Not from server!  Instead from:" + from);
	}
	
	void messageReceived(final Serializable msg, final INode from)
	{
		if (msg instanceof SpokeInvoke)
		{
			// if this isn't the server, something is wrong
			// maybe an attempt to spoof a message
			assertIsServer(from);
			final SpokeInvoke invoke = (SpokeInvoke) msg;
			EndPoint local;
			synchronized (m_endPointMutex)
			{
				local = m_localEndPoints.get(invoke.call.getRemoteName());
			}
			// something a bit strange here, it may be the case
			// that the endpoint was deleted locally
			// regardless, the other side is expecting our reply
			if (local == null)
			{
				if (invoke.needReturnValues)
				{
					send(new HubInvocationResults(new RemoteMethodCallResults(new RemoteNotFoundException("No implementors for " + invoke.call)), invoke.methodCallID), from);
				}
				return;
			}
			// very important
			// we are guaranteed that here messages will be
			// read in the same order that they are sent from the client
			// however, once we delegate to the thread pool, there is no
			// guarantee that the thread pool task will run before
			// we get the next message notification
			// get the number for the invocation here
			final long methodRunNumber = local.takeANumber();
			// we dont want to block the message thread, only one thread is
			// reading messages
			// per connection, so run with out thread pool
			final EndPoint localFinal = local;
			final Runnable task = new Runnable()
			{
				public void run()
				{
					final List<RemoteMethodCallResults> results = localFinal.invokeLocal(invoke.call, methodRunNumber, invoke.getInvoker());
					if (invoke.needReturnValues)
					{
						RemoteMethodCallResults result = null;
						if (results.size() == 1)
						{
							result = results.get(0);
						}
						else
							result = new RemoteMethodCallResults(new IllegalStateException("Invalid result count" + results.size()) + " for end point:" + localFinal);
						send(new HubInvocationResults(result, invoke.methodCallID), from);
					}
				}
			};
			m_threadPool.runTask(task);
		}
		// a remote machine is returning results
		else if (msg instanceof SpokeInvocationResults)
		{
			// if this isn't the server, something is wrong
			// maybe an attempt to spoof a message
			assertIsServer(from);
			final SpokeInvocationResults results = (SpokeInvocationResults) msg;
			final GUID methodID = results.methodCallID;
			// both of these should already be populated
			// this list should be a synchronized list so we can do the add
			// all
			synchronized (m_pendingLock)
			{
				m_results.put(methodID, results.results);
				m_pendingInvocations.remove(methodID).countDown();
			}
		}
	}
	
	public void dumpState(final PrintStream stream)
	{
		synchronized (m_endPointMutex)
		{
			stream.println("Local Endpoints:" + m_localEndPoints);
		}
		synchronized (m_endPointMutex)
		{
			stream.println("Remote nodes with implementors:" + m_results);
			stream.println("Remote nodes with implementors:" + m_pendingInvocations);
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
	// the next number we are going to give
	private final AtomicLong m_nextGivenNumber = new AtomicLong();
	// the next number we can run
	private long m_currentRunnableNumber = 0;
	private final Object m_numberMutext = new Object();
	private final Object m_implementorsMutext = new Object();
	private final String m_name;
	private final Class<?> m_remoteClass;
	private final List<Object> m_implementors = new ArrayList<Object>();
	private final boolean m_singleThreaded;
	
	public EndPoint(final String name, final Class<?> remoteClass, final boolean singleThreaded)
	{
		m_name = name;
		m_remoteClass = remoteClass;
		m_singleThreaded = singleThreaded;
	}
	
	public Object getFirstImplementor()
	{
		synchronized (m_implementorsMutext)
		{
			if (m_implementors.size() != 1)
			{
				throw new IllegalStateException("Invalid implementor count, " + m_implementors);
			}
			return m_implementors.get(0);
		}
	}
	
	public long takeANumber()
	{
		return m_nextGivenNumber.getAndIncrement();
	}
	
	private void waitTillCanBeRun(final long aNumber)
	{
		synchronized (m_numberMutext)
		{
			while (aNumber > m_currentRunnableNumber)
			{
				try
				{
					m_numberMutext.wait();
				} catch (final InterruptedException e)
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
	public boolean addImplementor(final Object implementor)
	{
		if (!m_remoteClass.isAssignableFrom(implementor.getClass()))
			throw new IllegalArgumentException(m_remoteClass + " is not assignable from " + implementor.getClass());
		synchronized (m_implementorsMutext)
		{
			final boolean rVal = m_implementors.isEmpty();
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
	boolean removeImplementor(final Object implementor)
	{
		synchronized (m_implementorsMutext)
		{
			if (!m_implementors.remove(implementor))
			{
				throw new IllegalStateException("Not removed, impl:" + implementor + " have " + m_implementors);
			}
			return m_implementors.isEmpty();
		}
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public Class<?> getRemoteClass()
	{
		return m_remoteClass;
	}
	
	/*
	 * @param number - like the number you get in a bank line, if we are single
	 * threaded, then the method will not run until the number comes up. Acquire
	 * with getNumber() @return a List of RemoteMethodCallResults
	 */
	public List<RemoteMethodCallResults> invokeLocal(final RemoteMethodCall call, final long number, final INode messageOriginator)
	{
		try
		{
			if (m_singleThreaded)
			{
				waitTillCanBeRun(number);
			}
			return invokeMultiple(call, messageOriginator);
		} finally
		{
			releaseNumber();
		}
	}
	
	/**
	 * @param call
	 * @param rVal
	 */
	private List<RemoteMethodCallResults> invokeMultiple(final RemoteMethodCall call, final INode messageOriginator)
	{
		// copy the implementors
		List<Object> implementorsCopy;
		synchronized (m_implementorsMutext)
		{
			implementorsCopy = new ArrayList<Object>(m_implementors);
		}
		final List<RemoteMethodCallResults> results = new ArrayList<RemoteMethodCallResults>(implementorsCopy.size());
		for (final Object implementor : implementorsCopy)
		{
			results.add(invokeSingle(call, implementor, messageOriginator));
		}
		return results;
	}
	
	/**
	 * @param call
	 * @param implementor
	 * @return
	 */
	private RemoteMethodCallResults invokeSingle(final RemoteMethodCall call, final Object implementor, final INode messageOriginator)
	{
		call.resolve(m_remoteClass);
		Method method;
		try
		{
			method = implementor.getClass().getMethod(call.getMethodName(), call.getArgTypes());
			method.setAccessible(true);
		} catch (final SecurityException e)
		{
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		} catch (final NoSuchMethodException e)
		{
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		}
		MessageContext.setSenderNodeForThread(messageOriginator);
		try
		{
			final Object methodRVal = method.invoke(implementor, call.getArgs());
			return new RemoteMethodCallResults(methodRVal);
		} catch (final InvocationTargetException e)
		{
			return new RemoteMethodCallResults(e.getTargetException());
		} catch (final IllegalAccessException e)
		{
			// this shouldnt happen
			System.err.println("error in call:" + call);
			e.printStackTrace();
			return new RemoteMethodCallResults(e);
		} catch (final IllegalArgumentException e)
		{
			// this shouldnt happen
			System.err.println("error in call:" + call);
			e.printStackTrace();
			return new RemoteMethodCallResults(e);
		} finally
		{
			MessageContext.setSenderNodeForThread(null);
		}
	}
	
	public boolean equivalent(final EndPoint other)
	{
		if (other.m_singleThreaded != this.m_singleThreaded)
			return false;
		if (!other.m_name.equals(this.m_name))
			return false;
		if (!(other.m_remoteClass.equals(m_remoteClass)))
			return false;
		return true;
	}
	
	@Override
	public String toString()
	{
		return "Name:" + m_name + " singleThreaded:" + m_singleThreaded + " implementors:" + m_implementors;
	}
}


// an end point has been created, we should follow
class EndPointCreated implements Serializable
{
	private static final long serialVersionUID = -5780669206340723091L;
	public final String[] classes;
	public final String name;
	public final boolean singleThreaded;
	
	public EndPointCreated(final String[] classes, final String name, final boolean singleThreaded)
	{
		this.classes = classes;
		this.name = name;
		this.singleThreaded = singleThreaded;
	}
}


// and end point has been destroyed, we too should jump off that bridge
class EndPointDestroyed implements Serializable
{
	private static final long serialVersionUID = 8932889316564814214L;
	public final String name;
	
	public EndPointDestroyed(final String name)
	{
		this.name = name;
	}
	
	@Override
	public String toString()
	{
		return "EndPointDestroyed:" + name;
	}
}


// someone now has an implementor for an endpoint
class HasEndPointImplementor implements Serializable
{
	private static final long serialVersionUID = 7607319129099694815L;
	public final String endPointName;
	
	public HasEndPointImplementor(final String endPointName)
	{
		this.endPointName = endPointName;
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getName() + ":" + endPointName;
	}
}


// someone no longer has implementors for an endpoint
class NoLongerHasEndPointImplementor implements Serializable
{
	private static final long serialVersionUID = -4855990132007435355L;
	public final String endPointName;
	
	public NoLongerHasEndPointImplementor(final String endPointName)
	{
		this.endPointName = endPointName;
	}
}


// someone wants us to invoke something locally
abstract class Invoke implements Externalizable
{
	private static final long serialVersionUID = -5453883962199970896L;
	public GUID methodCallID;
	public boolean needReturnValues;
	public RemoteMethodCall call;
	
	public Invoke()
	{
	}
	
	@Override
	public String toString()
	{
		return "invoke on:" + call.getRemoteName() + " method name:" + call.getMethodName() + " method call id:" + methodCallID;
	}
	
	public Invoke(final GUID methodCallID, final boolean needReturnValues, final RemoteMethodCall call)
	{
		if (needReturnValues && methodCallID == null)
			throw new IllegalArgumentException("Cant have no id and need return values");
		if (!needReturnValues && methodCallID != null)
			throw new IllegalArgumentException("Cant have id and not need return values");
		this.methodCallID = methodCallID;
		this.needReturnValues = needReturnValues;
		this.call = call;
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException
	{
		needReturnValues = in.read() == 1;
		if (needReturnValues)
			methodCallID = (GUID) in.readObject();
		call = new RemoteMethodCall();
		call.readExternal(in);
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException
	{
		out.write(needReturnValues ? 1 : 0);
		if (needReturnValues)
			out.writeObject(methodCallID);
		call.writeExternal(out);
	}
}


// the results of a remote invocation
abstract class InvocationResults implements Externalizable
{
	private static final long serialVersionUID = -382704036681832123L;
	public RemoteMethodCallResults results;
	public GUID methodCallID;
	
	public InvocationResults()
	{
	}
	
	public InvocationResults(final RemoteMethodCallResults results, final GUID methodCallID)
	{
		if (results == null)
			throw new IllegalArgumentException("Null results");
		if (methodCallID == null)
			throw new IllegalArgumentException("Null id");
		this.results = results;
		this.methodCallID = methodCallID;
	}
	
	@Override
	public String toString()
	{
		return "Invocation results for method id:" + methodCallID + " results:" + results;
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException
	{
		results.writeExternal(out);
		methodCallID.writeExternal(out);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException
	{
		results = new RemoteMethodCallResults();
		results.readExternal(in);
		methodCallID = new GUID();
		methodCallID.readExternal(in);
	}
}
