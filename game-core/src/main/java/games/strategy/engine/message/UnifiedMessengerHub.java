package games.strategy.engine.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.message.unifiedmessenger.HasEndPointImplementor;
import games.strategy.engine.message.unifiedmessenger.NoLongerHasEndPointImplementor;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessageListener;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.util.Interruptibles;

public class UnifiedMessengerHub implements IMessageListener, IConnectionChangeListener {
  private static final int NODE_IMPLEMENTATION_TIMEOUT = 200;
  private final UnifiedMessenger localUnified;
  // the messenger we are based on
  private final IMessenger messenger;
  // maps end points to a list of nodes with implementors
  private final Map<String, Collection<INode>> endPoints = new HashMap<>();
  // changes to the list of endpoints, or reads to it, should be made
  // only while holding this lock
  private final Object endPointMutex = new Object();
  // the invocations that are currently in progress
  private final Map<GUID, InvocationInProgress> invocations = new ConcurrentHashMap<>();

  public UnifiedMessengerHub(final IMessenger messenger, final UnifiedMessenger localUnified) {
    this.messenger = messenger;
    this.localUnified = localUnified;
    this.messenger.addMessageListener(this);
    ((IServerMessenger) this.messenger).addConnectionChangeListener(this);
  }

  private void send(final Serializable msg, final INode to) {
    if (messenger.getLocalNode().equals(to)) {
      localUnified.messageReceived(msg, messenger.getLocalNode());
    } else {
      messenger.send(msg, to);
    }
  }

  @Override
  public void messageReceived(final Serializable msg, final INode from) {
    if (msg instanceof HasEndPointImplementor) {
      synchronized (endPointMutex) {
        final HasEndPointImplementor hasEndPoint = (HasEndPointImplementor) msg;
        final Collection<INode> nodes = endPoints.computeIfAbsent(hasEndPoint.endPointName, k -> new ArrayList<>());
        if (nodes.contains(from)) {
          throw new IllegalStateException(
              "Already contained, new" + from + " existing, " + nodes + " name " + hasEndPoint.endPointName);
        }
        nodes.add(from);
      }
    } else if (msg instanceof NoLongerHasEndPointImplementor) {
      synchronized (endPointMutex) {
        final NoLongerHasEndPointImplementor hasEndPoint = (NoLongerHasEndPointImplementor) msg;
        final Collection<INode> nodes = endPoints.get(hasEndPoint.endPointName);
        if (nodes != null) {
          if (!nodes.remove(from)) {
            throw new IllegalStateException("Not removed!");
          }
          if (nodes.isEmpty()) {
            endPoints.remove(hasEndPoint.endPointName);
          }
        }
      }
    } else if (msg instanceof HubInvoke) {
      final HubInvoke invoke = (HubInvoke) msg;
      final Collection<INode> endPointCols = new ArrayList<>();
      synchronized (endPointMutex) {
        if (endPoints.containsKey(invoke.call.getRemoteName())) {
          endPointCols.addAll(endPoints.get(invoke.call.getRemoteName()));
        }
      }
      // the node will already have routed messages to local invokers
      endPointCols.remove(from);
      if (endPointCols.isEmpty()) {
        if (invoke.needReturnValues) {
          final RemoteMethodCallResults results =
              new RemoteMethodCallResults(new RemoteNotFoundException("Not found:" + invoke.call.getRemoteName()));
          send(new SpokeInvocationResults(results, invoke.methodCallId), from);
        }
        // no end points, this is ok, we
        // we are a channel with no implementors
      } else {
        invoke(invoke, endPointCols, from);
      }
    } else if (msg instanceof HubInvocationResults) {
      final HubInvocationResults results = (HubInvocationResults) msg;
      results(results, from);
    }
  }

  private void results(final HubInvocationResults results, final INode from) {
    final GUID methodId = results.methodCallId;
    final InvocationInProgress invocationInProgress = invocations.get(methodId);
    final boolean done = invocationInProgress.process(results, from);
    if (done) {
      invocations.remove(methodId);
      if (invocationInProgress.shouldSendResults()) {
        sendResultsToCaller(methodId, invocationInProgress);
      }
    }
  }

  private void sendResultsToCaller(final GUID methodId, final InvocationInProgress invocationInProgress) {
    final RemoteMethodCallResults result = invocationInProgress.getResults();
    final INode caller = invocationInProgress.getCaller();
    final SpokeInvocationResults spokeResults = new SpokeInvocationResults(result, methodId);
    send(spokeResults, caller);
  }

  private void invoke(final HubInvoke hubInvoke, final Collection<INode> remote, final INode from) {
    if (hubInvoke.needReturnValues) {
      if (remote.size() != 1) {
        throw new IllegalStateException("Too many nodes:" + remote + " for remote name " + hubInvoke.call);
      }
      final InvocationInProgress invocationInProgress =
          new InvocationInProgress(remote.iterator().next(), hubInvoke, from);
      invocations.put(hubInvoke.methodCallId, invocationInProgress);
    }
    // invoke remotely
    final SpokeInvoke invoke =
        new SpokeInvoke(hubInvoke.methodCallId, hubInvoke.needReturnValues, hubInvoke.call, from);
    for (final INode node : remote) {
      send(invoke, node);
    }
  }

  /**
   * Wait for the messenger to know about the given endpoint.
   *
   * @deprecated testing code smell, should not be dependent upon wall clock timing, try to remove this method.
   */
  @VisibleForTesting
  @Deprecated
  public void waitForNodesToImplement(final String endPointName) {
    final long endTime = NODE_IMPLEMENTATION_TIMEOUT + System.currentTimeMillis();
    while ((System.currentTimeMillis() < endTime) && !hasImplementors(endPointName)) {
      if (!Interruptibles.sleep(50)) {
        return;
      }
    }
  }

  public boolean hasImplementors(final String endPointName) {
    synchronized (endPointMutex) {
      return endPoints.containsKey(endPointName) && !endPoints.get(endPointName).isEmpty();
    }
  }

  @Override
  public void connectionAdded(final INode to) {}

  @Override
  public void connectionRemoved(final INode to) {
    // we lost a connection to a node
    // any pending results should return
    synchronized (endPointMutex) {
      for (final Collection<INode> nodes : endPoints.values()) {
        nodes.remove(to);
      }
    }
    for (final InvocationInProgress invocation : invocations.values()) {
      if (invocation.isWaitingOn(to)) {
        final RemoteMethodCallResults results =
            new RemoteMethodCallResults(new ConnectionLostException("Connection to " + to.getName() + " lost"));
        final HubInvocationResults hubResults = new HubInvocationResults(results, invocation.getMethodCallId());
        results(hubResults, to);
      }
    }
  }
}
