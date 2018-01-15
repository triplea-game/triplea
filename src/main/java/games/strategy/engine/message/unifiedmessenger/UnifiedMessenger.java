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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

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

/**
 * A messenger general enough that both Channel and Remote messenger can be
 * based on it.
 */
public class UnifiedMessenger {
  private static final Logger logger = Logger.getLogger(UnifiedMessenger.class.getName());

  private static final ExecutorService threadPool = Executors.newFixedThreadPool(15);
  // the messenger we are based on
  private final IMessenger messenger;
  // lock on this for modifications to create or remove local end points
  private final Object endPointMutex = new Object();
  // maps String -> EndPoint
  // these are the end points that
  // have local implementors
  private final Map<String, EndPoint> localEndPoints = new HashMap<>();
  private final Object pendingLock = new Object();
  // threads wait on these latches for the hub to return invocations
  // the latch should be removed from the map when you countdown the last result
  // access should be synchronized on pendingLock
  // TODO: how do these get shutdown when we exit a game or close triplea?
  private final Map<GUID, CountDownLatch> pendingInvocations = new HashMap<>();
  // after the remote has invoked, the results are placed here
  // access should be synchronized on pendingLock
  private final Map<GUID, RemoteMethodCallResults> results = new HashMap<>();
  // only non null for the server
  private UnifiedMessengerHub hub;

  /**
   * Creates a new instance of UnifiedMessanger.
   */
  public UnifiedMessenger(final IMessenger messenger) {
    this.messenger = messenger;
    final IMessageListener messageListener = (msg, from) -> UnifiedMessenger.this.messageReceived(msg, from);
    this.messenger.addMessageListener(messageListener);
    final IMessengerErrorListener messengerErrorListener =
        (messenger1, reason) -> UnifiedMessenger.this.messengerInvalid();
    this.messenger.addErrorListener(messengerErrorListener);
    if (this.messenger.isServer()) {
      hub = new UnifiedMessengerHub(this.messenger, this);
    }
  }

  @VisibleForTesting
  UnifiedMessengerHub getHub() {
    return hub;
  }

  private void messengerInvalid() {
    synchronized (pendingLock) {
      for (final GUID id : pendingInvocations.keySet()) {
        final CountDownLatch latch = pendingInvocations.remove(id);
        latch.countDown();
        results.put(id, new RemoteMethodCallResults(new ConnectionLostException("Connection Lost")));
      }
    }
  }

  /**
   * Invoke and wait for all implementors on all vms to finish executing.
   */
  public RemoteMethodCallResults invokeAndWait(final String endPointName, final RemoteMethodCall remoteCall) {
    final EndPoint local;
    synchronized (endPointMutex) {
      local = localEndPoints.get(endPointName);
    }
    if (local == null) {
      return invokeAndWaitRemote(remoteCall);
      // we have the implementor here, just invoke it
    }

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

  private RemoteMethodCallResults invokeAndWaitRemote(final RemoteMethodCall remoteCall) {
    final GUID methodCallId = new GUID();
    final CountDownLatch latch = new CountDownLatch(1);
    synchronized (pendingLock) {
      pendingInvocations.put(methodCallId, latch);
    }
    // invoke remotely
    final Invoke invoke = new HubInvoke(methodCallId, true, remoteCall);
    send(invoke, messenger.getServerNode());

    try {
      latch.await();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    synchronized (pendingLock) {
      final RemoteMethodCallResults methodCallResults = results.remove(methodCallId);
      if (methodCallResults == null) {
        throw new IllegalStateException(
            "No results from remote call. Method returned:" + remoteCall.getMethodName() + " for remote name:"
                + remoteCall.getRemoteName() + " with id:" + methodCallId);
      }
      return methodCallResults;
    }
  }

  /**
   * invoke without waiting for remote nodes to respond.
   */
  public void invoke(final String endPointName, final RemoteMethodCall call) {
    // send the remote invocation
    final Invoke invoke = new HubInvoke(null, false, call);
    send(invoke, messenger.getServerNode());
    // invoke locally
    final EndPoint endPoint;
    synchronized (endPointMutex) {
      endPoint = localEndPoints.get(endPointName);
    }
    if (endPoint != null) {
      final long number = endPoint.takeANumber();
      final List<RemoteMethodCallResults> results = endPoint.invokeLocal(call, number, getLocalNode());
      for (final RemoteMethodCallResults r : results) {
        if (r.getException() != null) {
          // don't swallow errors
          logger.log(Level.WARNING, r.getException().getMessage(), r.getException());
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
    return messenger.getLocalNode();
  }

  /**
   * Get the 1 and only implementor for the endpoint. throws an exception if there are not exctly 1 implementors
   */
  public Object getImplementor(final String name) {
    synchronized (endPointMutex) {
      final EndPoint endPoint = localEndPoints.get(name);
      Preconditions.checkNotNull(endPoint, "local endpoints: "
          + localEndPoints + " did not contain: " + name + ", messenger addr: " + super.toString());
      return endPoint.getFirstImplementor();
    }
  }

  public void removeImplementor(final String name, final Object implementor) {
    final EndPoint endPoint;
    synchronized (endPointMutex) {
      endPoint = localEndPoints.get(name);
      if (endPoint == null) {
        throw new IllegalStateException("No end point for:" + name);
      }
      if (implementor == null) {
        throw new IllegalArgumentException("null implementor");
      }
      final boolean noneLeft = endPoint.removeImplementor(implementor);
      if (noneLeft) {
        localEndPoints.remove(name);
        send(new NoLongerHasEndPointImplementor(name), messenger.getServerNode());
      }
    }
  }

  private EndPoint getLocalEndPointOrCreate(final RemoteName endPointDescriptor, final boolean singleThreaded) {
    final EndPoint endPoint;
    synchronized (endPointMutex) {
      if (localEndPoints.containsKey(endPointDescriptor.getName())) {
        return localEndPoints.get(endPointDescriptor.getName());
      }
      endPoint = new EndPoint(endPointDescriptor.getName(), endPointDescriptor.getClazz(), singleThreaded);
      localEndPoints.put(endPointDescriptor.getName(), endPoint);
    }
    final HasEndPointImplementor msg = new HasEndPointImplementor(endPointDescriptor.getName());
    send(msg, messenger.getServerNode());
    return endPoint;
  }

  private void send(final Serializable msg, final INode to) {
    if (messenger.getLocalNode().equals(to)) {
      hub.messageReceived(msg, getLocalNode());
    } else {
      messenger.send(msg, to);
    }
  }

  public boolean isServer() {
    return messenger.isServer();
  }

  public int getLocalEndPointCount(final RemoteName descriptor) {
    synchronized (endPointMutex) {
      if (!localEndPoints.containsKey(descriptor.getName())) {
        return 0;
      }
      return localEndPoints.get(descriptor.getName()).getLocalImplementorCount();
    }
  }


  public void messageReceived(final Serializable msg, final INode from) {
    if (msg instanceof SpokeInvoke) {
      // if this isn't the server, something is wrong
      // maybe an attempt to spoof a message
      assertIsServer(from);
      final SpokeInvoke invoke = (SpokeInvoke) msg;
      final EndPoint local;
      synchronized (endPointMutex) {
        local = localEndPoints.get(invoke.call.getRemoteName());
      }
      // something a bit strange here, it may be the case
      // that the endpoint was deleted locally
      // regardless, the other side is expecting our reply
      if (local == null) {
        if (invoke.needReturnValues) {
          send(new HubInvocationResults(
              new RemoteMethodCallResults(new RemoteNotFoundException(
                  "No implementors for " + invoke.call + ", inode: " + from + ", msg: " + msg)),
              invoke.methodCallId), from);
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
      // we don't want to block the message thread, only one thread is
      // reading messages per connection, so run with out thread pool
      final EndPoint localFinal = local;
      threadPool.execute(() -> {
        final List<RemoteMethodCallResults> results =
            localFinal.invokeLocal(invoke.call, methodRunNumber, invoke.getInvoker());
        if (invoke.needReturnValues) {
          final RemoteMethodCallResults result;
          if (results.size() == 1) {
            result = results.get(0);
          } else {
            result = new RemoteMethodCallResults(
                new IllegalStateException("Invalid result count" + results.size()) + " for end point:" + localFinal);
          }
          send(new HubInvocationResults(result, invoke.methodCallId), from);
        }
      });
    } else if (msg instanceof SpokeInvocationResults) { // a remote machine is returning results
      // if this isn't the server, something is wrong
      // maybe an attempt to spoof a message
      assertIsServer(from);
      final SpokeInvocationResults spokeInvocationResults = (SpokeInvocationResults) msg;
      final GUID methodId = spokeInvocationResults.methodCallId;
      // both of these should already be populated
      // this list should be a synchronized list so we can do the add
      // all
      synchronized (pendingLock) {
        results.put(methodId, spokeInvocationResults.results);
        final CountDownLatch latch = pendingInvocations.remove(methodId);
        Preconditions.checkNotNull(latch, String.format(
            "method id: %s, was not present in pending invocations: %s, unified messenger addr: %s",
            methodId, pendingInvocations, super.toString()));
        latch.countDown();
      }
    }
  }

  private void assertIsServer(final INode from) {
    Preconditions.checkState(
        from.equals(messenger.getServerNode()), "Not from server!  Instead from:" + from);
  }

  @Override
  public String toString() {
    return "Server:" + messenger.isServer() + " EndPoints:" + localEndPoints;
  }
}


