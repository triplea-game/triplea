package games.strategy.engine.message.unifiedmessenger;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.message.HubInvocationResults;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteMethodCallResults;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.RemoteNotFoundException;
import games.strategy.engine.message.SpokeInvocationResults;
import games.strategy.engine.message.SpokeInvoke;
import games.strategy.engine.message.UnifiedMessengerHub;
import games.strategy.net.IClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;
import org.triplea.java.concurrency.AsyncRunner;

/** A messenger general enough that both Channel and Remote messenger can be based on it. */
@Slf4j
public class UnifiedMessenger {
  private static final ExecutorService threadPool = Executors.newFixedThreadPool(15);
  // the messenger we are based on
  private final IMessenger messenger;
  // lock on this for modifications to create or remove local end points
  private final Object endPointMutex = new Object();
  // maps String -> EndPoint
  // these are the end points that have local implementors
  private final Map<String, EndPoint> localEndPoints = new HashMap<>();
  private final Object pendingLock = new Object();
  // threads wait on these latches for the hub to return invocations
  // the latch should be removed from the map when you countdown the last result
  // access should be synchronized on pendingLock
  // TODO: how do these get shutdown when we exit a game or close triplea?
  private final Map<UUID, CountDownLatch> pendingInvocations = new HashMap<>();
  // after the remote has invoked, the results are placed here
  // access should be synchronized on pendingLock
  private final Map<UUID, RemoteMethodCallResults> results = new HashMap<>();
  // only non null for the server
  private UnifiedMessengerHub hub;

  public UnifiedMessenger(final IMessenger messenger) {
    this.messenger = messenger;
    this.messenger.addMessageListener(this::messageReceived);
    if (messenger instanceof IClientMessenger) {
      ((IClientMessenger) this.messenger).addErrorListener(this::messengerInvalid);
    }
    if (this.messenger.isServer()) {
      hub = new UnifiedMessengerHub(this.messenger, this);
    }
  }

  @VisibleForTesting
  public UnifiedMessengerHub getHub() {
    return hub;
  }

  private void messengerInvalid(final Throwable cause) {
    synchronized (pendingLock) {
      final Set<UUID> pendingInvocationsKeySet = Set.copyOf(pendingInvocations.keySet());
      for (final UUID id : pendingInvocationsKeySet) {
        final CountDownLatch latch = pendingInvocations.remove(id);
        latch.countDown();
        results.put(id, new RemoteMethodCallResults(cause));
      }
    }
  }

  /** Invoke and wait for all implementors on all vms to finish executing. */
  public RemoteMethodCallResults invokeAndWait(
      final String endPointName, final RemoteMethodCall remoteCall) throws RemoteNotFoundException {
    final EndPoint local;
    synchronized (endPointMutex) {
      local = localEndPoints.get(endPointName);
    }
    if (local == null) {
      return invokeAndWaitRemote(remoteCall);
      // we have the implementor here, just invoke it
    }

    final long number = local.takeANumber();
    final List<RemoteMethodCallResults> results =
        local.invokeLocal(remoteCall, number, getLocalNode());
    if (results.isEmpty()) {
      throw new RemoteNotFoundException(
          "Not found:"
              + endPointName
              + ", method name: "
              + remoteCall.getMethodName()
              + ", remote name: "
              + remoteCall.getRemoteName());
    }
    if (results.size() > 1) {
      throw new IllegalStateException("Too many implementors, got back: " + results);
    }
    return results.get(0);
  }

  private RemoteMethodCallResults invokeAndWaitRemote(final RemoteMethodCall remoteCall) {
    final UUID methodCallId = UUID.randomUUID();
    final CountDownLatch latch = new CountDownLatch(1);
    synchronized (pendingLock) {
      pendingInvocations.put(methodCallId, latch);
    }
    // invoke remotely
    final Invoke invoke = new HubInvoke(methodCallId, true, remoteCall);
    send(invoke, messenger.getServerNode());

    Interruptibles.await(latch);

    synchronized (pendingLock) {
      final RemoteMethodCallResults methodCallResults = results.remove(methodCallId);
      if (methodCallResults == null) {
        throw new IllegalStateException(
            "No results from remote call. Method returned:"
                + remoteCall.getMethodName()
                + " for remote name:"
                + remoteCall.getRemoteName()
                + " with id:"
                + methodCallId);
      }
      return methodCallResults;
    }
  }

  /** invoke without waiting for remote nodes to respond. */
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
      final List<RemoteMethodCallResults> results =
          endPoint.invokeLocal(call, number, getLocalNode());
      for (final RemoteMethodCallResults r : results) {
        if (r.getException() != null) {
          log.warn(
              "Remote method call exception: " + r.getException().getMessage(), r.getException());
        }
      }
    }
  }

  public void addImplementor(
      final RemoteName endPointDescriptor, final Object implementor, final boolean singleThreaded) {
    if (!endPointDescriptor.getClazz().isAssignableFrom(implementor.getClass())) {
      throw new IllegalArgumentException(
          implementor + " does not implement " + endPointDescriptor.getClazz());
    }
    final EndPoint endPoint = getLocalEndPointOrCreate(endPointDescriptor, singleThreaded);
    endPoint.addImplementor(implementor);
  }

  public INode getLocalNode() {
    return messenger.getLocalNode();
  }

  /**
   * Get the 1 and only implementor for the end point. Throws an exception if there are not exactly
   * 1 implementors.
   */
  public Object getImplementor(final String name) {
    synchronized (endPointMutex) {
      final EndPoint endPoint = localEndPoints.get(name);
      checkNotNull(
          endPoint,
          "local endpoints: "
              + localEndPoints
              + " did not contain: "
              + name
              + ", messenger addr: "
              + super.toString());
      return endPoint.getOnlyImplementor();
    }
  }

  public boolean hasSingleImplementor(final String name) {
    synchronized (endPointMutex) {
      return Optional.ofNullable(localEndPoints.get(name))
          .map(EndPoint::hasSingleImplementor)
          .orElse(false);
    }
  }

  /** Removes the specified implementor for the end point with the specified name. */
  public void removeImplementor(final String name, final Object implementor) {
    checkNotNull(implementor);

    synchronized (endPointMutex) {
      final EndPoint endPoint = localEndPoints.get(name);
      if (endPoint == null) {
        throw new IllegalStateException("No end point for: " + name);
      }
      final boolean noneLeft = endPoint.removeImplementor(implementor);
      if (noneLeft) {
        localEndPoints.remove(name);
        send(new NoLongerHasEndPointImplementor(name), messenger.getServerNode());
      }
    }
  }

  private EndPoint getLocalEndPointOrCreate(
      final RemoteName endPointDescriptor, final boolean singleThreaded) {
    final EndPoint endPoint;
    synchronized (endPointMutex) {
      if (localEndPoints.containsKey(endPointDescriptor.getName())) {
        return localEndPoints.get(endPointDescriptor.getName());
      }
      endPoint =
          new EndPoint(endPointDescriptor.getName(), endPointDescriptor.getClazz(), singleThreaded);
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

  /**
   * Invoked when a message is received from a node (either a remote client or the server itself).
   */
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
      // something a bit strange here, it may be the case that the end point was deleted locally
      // regardless, the other side is expecting our reply
      if (local == null) {
        if (invoke.needReturnValues) {
          send(
              new HubInvocationResults(
                  new RemoteMethodCallResults(
                      new RemoteNotFoundException(
                          "No implementors for "
                              + invoke.call
                              + ", inode: "
                              + from
                              + ", msg: "
                              + msg)),
                  invoke.methodCallId),
              from);
        }
        return;
      }
      processMessage(local, invoke, from);
    } else if (msg instanceof SpokeInvocationResults) { // a remote machine is returning results
      // if this isn't the server, something is wrong
      // maybe an attempt to spoof a message
      assertIsServer(from);
      final SpokeInvocationResults spokeInvocationResults = (SpokeInvocationResults) msg;
      final UUID methodId = spokeInvocationResults.methodCallId;
      // both of these should already be populated
      // this list should be a synchronized list so we can do the add all
      synchronized (pendingLock) {
        results.put(methodId, spokeInvocationResults.results);
        final CountDownLatch latch = pendingInvocations.remove(methodId);
        checkNotNull(
            latch,
            String.format(
                "method id: %s, was not present in pending invocations: %s, "
                    + "unified messenger addr: %s",
                methodId, pendingInvocations, super.toString()));
        latch.countDown();
      }
    }
  }

  private void processMessage(final EndPoint local, final SpokeInvoke invoke, final INode from) {
    // very important
    // we are guaranteed that here messages will be read in the same order that they are sent from
    // the client
    // however, once we delegate to the thread pool, there is no guarantee that the thread pool
    // task will run before
    // we get the next message notification
    // get the number for the invocation here
    final long methodRunNumber = local.takeANumber();
    // we don't want to block the message thread, only one thread is
    // reading messages per connection, so run with out thread pool
    AsyncRunner.runAsync(
            () -> {
              final List<RemoteMethodCallResults> results =
                  local.invokeLocal(invoke.call, methodRunNumber, invoke.getInvoker());
              if (invoke.needReturnValues) {
                final RemoteMethodCallResults result;
                if (results.size() == 1) {
                  result = results.get(0);
                } else {
                  result =
                      new RemoteMethodCallResults(
                          new IllegalStateException(
                              String.format(
                                  "Invalid result count '%d' for end point '%s'",
                                  results.size(), local)));
                }
                send(new HubInvocationResults(result, invoke.methodCallId), from);
              }
            },
            threadPool)
        .exceptionally(
            throwable -> {
              log.error("Exception during execution of client request", throwable);
              if (invoke.needReturnValues) {
                try {
                  send(
                      new HubInvocationResults(
                          new RemoteMethodCallResults(throwable), invoke.methodCallId),
                      from);
                } catch (final RuntimeException e) {
                  log.error("Exception while sending exception to client", throwable);
                }
              }
            });
  }

  private void assertIsServer(final INode from) {
    Preconditions.checkState(
        from.equals(messenger.getServerNode()), "Not from server!  Instead from: " + from);
  }

  @Override
  public String toString() {
    return "Server: " + messenger.isServer() + " EndPoints: " + localEndPoints;
  }
}
