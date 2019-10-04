package games.strategy.engine.message;

import games.strategy.net.INode;
import java.util.UUID;

/** We are waiting for the results of a remote invocation. */
class InvocationInProgress {
  private final INode waitingOn;
  private final HubInvoke methodCall;
  private final INode caller;
  private RemoteMethodCallResults results;

  InvocationInProgress(
      final INode waitingOn, final HubInvoke methodCalls, final INode methodCallsFrom) {
    this.waitingOn = waitingOn;
    methodCall = methodCalls;
    caller = methodCallsFrom;
  }

  boolean isWaitingOn(final INode node) {
    return waitingOn.equals(node);
  }

  /** Returns true if there are no more results to process. */
  boolean process(final HubInvocationResults hubResults, final INode from) {
    results = hubResults.results;
    if (!from.equals(waitingOn)) {
      throw new IllegalStateException("Wrong node, expecting " + waitingOn + " got " + from);
    }
    return true;
  }

  INode getCaller() {
    return caller;
  }

  RemoteMethodCallResults getResults() {
    return results;
  }

  UUID getMethodCallId() {
    return methodCall.methodCallId;
  }

  boolean shouldSendResults() {
    return methodCall.needReturnValues;
  }
}
