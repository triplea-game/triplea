package games.strategy.engine.message.unifiedmessenger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.message.ConnectionLostException;
import games.strategy.engine.message.HubInvocationResults;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteMethodCallResults;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.RemoteNotFoundException;
import games.strategy.engine.message.SpokeInvocationResults;
import games.strategy.engine.message.SpokeInvoke;
import games.strategy.engine.message.UnifiedMessengerHub;
import games.strategy.net.GUID;
import games.strategy.net.IMessageListener;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.util.ThreadUtil;

/**
 * A messenger general enough that both Channel and Remote messenger can be
 * based on it.
 */
public class UnifiedMessenger {
  private static final Logger s_logger = Logger.getLogger(UnifiedMessenger.class.getName());

  private static final ExecutorService threadPool = Executors.newFixedThreadPool(15);
  // the messenger we are based on
  private final IMessenger m_messenger;
  // lock on this for modifications to create or remove local end points
  private final Object m_endPointMutex = new Object();
  // maps String -> EndPoint
  // these are the end points that
  // have local implementors
  private final Map<String, EndPoint> m_localEndPoints = new HashMap<>();
  private final Object m_pendingLock = new Object();
  // threads wait on these latches for the hub to return invocations
  // the latch should be removed from the map when you countdown the last result
  // access should be synchronized on m_pendingLock
  // TODO: how do these get shutdown when we exit a game or close triplea?
  private final Map<GUID, CountDownLatch> m_pendingInvocations = new HashMap<>();
  // after the remote has invoked, the results are placed here
  // access should be synchronized on m_pendingLock
  private final Map<GUID, RemoteMethodCallResults> m_results = new HashMap<>();
  // only non null for the server
  private UnifiedMessengerHub m_hub;

  /**
   * Creates a new instance of UnifiedMessanger.
   */
  public UnifiedMessenger(final IMessenger messenger) {
    m_messenger = messenger;
    final IMessageListener m_messageListener = (msg, from) -> UnifiedMessenger.this.messageReceived(msg, from);
    m_messenger.addMessageListener(m_messageListener);
    final IMessengerErrorListener m_messengerErrorListener =
        (messenger1, reason) -> UnifiedMessenger.this.messengerInvalid();
    m_messenger.addErrorListener(m_messengerErrorListener);
    if (m_messenger.isServer()) {
      m_hub = new UnifiedMessengerHub(m_messenger, this);
    }
  }

  UnifiedMessengerHub getHub() {
    return m_hub;
  }

  private void messengerInvalid() {
    synchronized (m_pendingLock) {
      for (final GUID id : m_pendingInvocations.keySet()) {
        final CountDownLatch latch = m_pendingInvocations.remove(id);
        latch.countDown();
        m_results.put(id, new RemoteMethodCallResults(new ConnectionLostException("Connection Lost")));
      }
    }
  }

  /**
   * Invoke and wait for all implementors on all vms to finish executing.
   */
  public RemoteMethodCallResults invokeAndWait(final String endPointName, final RemoteMethodCall remoteCall) {
    EndPoint local;
    synchronized (m_endPointMutex) {
      local = m_localEndPoints.get(endPointName);
    }
    if (local == null) {
      return invokeAndWaitRemote(remoteCall);
      // we have the implementor here, just invoke it
    } else {
      final long number = local.takeANumber();
      final List<RemoteMethodCallResults> results = local.invokeLocal(remoteCall, number, getLocalNode());
      if (results.size() == 0) {
        throw new RemoteNotFoundException("Not found:" + endPointName);
      }
      if (results.size() > 1) {
        throw new IllegalStateException("Too many implementors, got back:" + results);
      }
      return results.get(0);
    }
  }

  private RemoteMethodCallResults invokeAndWaitRemote(final RemoteMethodCall remoteCall) {
    final GUID methodCallID = new GUID();
    final CountDownLatch latch = new CountDownLatch(1);
    synchronized (m_pendingLock) {
      m_pendingInvocations.put(methodCallID, latch);
    }
    // invoke remotely
    final Invoke invoke = new HubInvoke(methodCallID, true, remoteCall);
    send(invoke, m_messenger.getServerNode());

    try {
      latch.await();
    } catch (final InterruptedException e) {
      s_logger.log(Level.WARNING, e.getMessage());
    }

    synchronized (m_pendingLock) {
      final RemoteMethodCallResults results = m_results.remove(methodCallID);
      if (results == null) {
        throw new IllegalStateException(
            "No results from remote call. Method returned:" + remoteCall.getMethodName() + " for remote name:"
                + remoteCall.getRemoteName() + " with id:" + methodCallID);
      }
      return results;
    }
  }

  /**
   * invoke without waiting for remote nodes to respond.
   */
  public void invoke(final String endPointName, final RemoteMethodCall call) {
    // send the remote invocation
    final Invoke invoke = new HubInvoke(null, false, call);
    send(invoke, m_messenger.getServerNode());
    // invoke locally
    EndPoint endPoint;
    synchronized (m_endPointMutex) {
      endPoint = m_localEndPoints.get(endPointName);
    }
    if (endPoint != null) {
      final long number = endPoint.takeANumber();
      final List<RemoteMethodCallResults> results = endPoint.invokeLocal(call, number, getLocalNode());
      for (final RemoteMethodCallResults r : results) {
        if (r.getException() != null) {
          // don't swallow errors
          s_logger.log(Level.WARNING, r.getException().getMessage(), r.getException());
        }
      }
    }
  }

  public void addImplementor(final RemoteName endPointDescriptor, final Object implementor,
      final boolean singleThreaded) {
    if (!endPointDescriptor.getClazz().isAssignableFrom(implementor.getClass())) {
      throw new IllegalArgumentException(implementor + " does not implement " + endPointDescriptor.getClazz());
    }
    final EndPoint endPoint = getLocalEndPointOrCreate(endPointDescriptor, singleThreaded);
    endPoint.addImplementor(implementor);
  }

  public INode getLocalNode() {
    return m_messenger.getLocalNode();
  }

  /**
   * Get the 1 and only implementor for the endpoint. throws an exception if there are not exctly 1 implementors
   */
  public Object getImplementor(final String name) {
    synchronized (m_endPointMutex) {
      final EndPoint endPoint = m_localEndPoints.get(name);
      return endPoint.getFirstImplementor();
    }
  }

  public void removeImplementor(final String name, final Object implementor) {
    EndPoint endPoint;
    synchronized (m_endPointMutex) {
      endPoint = m_localEndPoints.get(name);
      if (endPoint == null) {
        throw new IllegalStateException("No end point for:" + name);
      }
      if (implementor == null) {
        throw new IllegalArgumentException("null implementor");
      }
      final boolean noneLeft = endPoint.removeImplementor(implementor);
      if (noneLeft) {
        m_localEndPoints.remove(name);
        send(new NoLongerHasEndPointImplementor(name), m_messenger.getServerNode());
      }
    }
  }

  private EndPoint getLocalEndPointOrCreate(final RemoteName endPointDescriptor, final boolean singleThreaded) {
    EndPoint endPoint;
    synchronized (m_endPointMutex) {
      if (m_localEndPoints.containsKey(endPointDescriptor.getName())) {
        return m_localEndPoints.get(endPointDescriptor.getName());
      }
      endPoint = new EndPoint(endPointDescriptor.getName(), endPointDescriptor.getClazz(), singleThreaded);
      m_localEndPoints.put(endPointDescriptor.getName(), endPoint);
    }
    final HasEndPointImplementor msg = new HasEndPointImplementor(endPointDescriptor.getName());
    send(msg, m_messenger.getServerNode());
    return endPoint;
  }

  private void send(final Serializable msg, final INode to) {
    if (m_messenger.getLocalNode().equals(to)) {
      m_hub.messageReceived(msg, getLocalNode());
    } else {
      m_messenger.send(msg, to);
    }
  }

  public boolean isServer() {
    return m_messenger.isServer();
  }

  /**
   * Wait for the messenger to know about the given endpoint.
   */
  public void waitForLocalImplement(final String endPointName, long timeoutMS) {
    // dont use Long.MAX_VALUE since that will overflow
    if (timeoutMS <= 0) {
      timeoutMS = Integer.MAX_VALUE;
    }
    final long endTime = timeoutMS + System.currentTimeMillis();
    while (System.currentTimeMillis() < endTime && !hasLocalEndPoint(endPointName)) {
      ThreadUtil.sleep(50);
    }
  }

  private boolean hasLocalEndPoint(final String endPointName) {
    synchronized (m_endPointMutex) {
      return m_localEndPoints.containsKey(endPointName);
    }
  }

  public int getLocalEndPointCount(final RemoteName descriptor) {
    synchronized (m_endPointMutex) {
      if (!m_localEndPoints.containsKey(descriptor.getName())) {
        return 0;
      }
      return m_localEndPoints.get(descriptor.getName()).getLocalImplementorCount();
    }
  }

  private void assertIsServer(final INode from) {
    if (!from.equals(m_messenger.getServerNode())) {
      throw new IllegalStateException("Not from server!  Instead from:" + from);
    }
  }

  public void messageReceived(final Serializable msg, final INode from) {
    if (msg instanceof SpokeInvoke) {
      // if this isn't the server, something is wrong
      // maybe an attempt to spoof a message
      assertIsServer(from);
      final SpokeInvoke invoke = (SpokeInvoke) msg;
      EndPoint local;
      synchronized (m_endPointMutex) {
        local = m_localEndPoints.get(invoke.call.getRemoteName());
      }
      // something a bit strange here, it may be the case
      // that the endpoint was deleted locally
      // regardless, the other side is expecting our reply
      if (local == null) {
        if (invoke.needReturnValues) {
          send(new HubInvocationResults(
              new RemoteMethodCallResults(new RemoteNotFoundException("No implementors for " + invoke.call)),
              invoke.methodCallID), from);
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
      final Runnable task = () -> {
        final List<RemoteMethodCallResults> results =
            localFinal.invokeLocal(invoke.call, methodRunNumber, invoke.getInvoker());
        if (invoke.needReturnValues) {
          RemoteMethodCallResults result = null;
          if (results.size() == 1) {
            result = results.get(0);
          } else {
            result = new RemoteMethodCallResults(
                new IllegalStateException("Invalid result count" + results.size()) + " for end point:" + localFinal);
          }
          send(new HubInvocationResults(result, invoke.methodCallID), from);
        }
      };
      threadPool.execute(task);
    } else if (msg instanceof SpokeInvocationResults) { // a remote machine is returning results
      // if this isn't the server, something is wrong
      // maybe an attempt to spoof a message
      assertIsServer(from);
      final SpokeInvocationResults results = (SpokeInvocationResults) msg;
      final GUID methodID = results.methodCallID;
      // both of these should already be populated
      // this list should be a synchronized list so we can do the add
      // all
      synchronized (m_pendingLock) {
        m_results.put(methodID, results.results);
        m_pendingInvocations.remove(methodID).countDown();
      }
    }
  }

  @Override
  public String toString() {
    return "Server:" + m_messenger.isServer() + " EndPoints:" + m_localEndPoints;
  }
}


