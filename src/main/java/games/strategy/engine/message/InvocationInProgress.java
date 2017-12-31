package games.strategy.engine.message;

import games.strategy.net.GUID;
import games.strategy.net.INode;

/**
 * We are waiting for the results of a remote invocation.
 */
class InvocationInProgress {
  private final INode waitingOn;
  private final HubInvoke methodCall;
  private final INode caller;
  private RemoteMethodCallResults results;

  InvocationInProgress(final INode waitingOn, final HubInvoke methodCalls, final INode methodCallsFrom) {
    this.waitingOn = waitingOn;
    methodCall = methodCalls;
    caller = methodCallsFrom;
  }

  boolean isWaitingOn(final INode node) {
    return waitingOn.equals(node);
  }

  /**
   * @return true if there are no more results to process.
   */
  boolean process(final HubInvocationResults hubresults, final INode from) {
    if (hubresults.results == null) {
      throw new IllegalStateException("No results");
    }
    results = hubresults.results;
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

  GUID getMethodCallId() {
    return methodCall.methodCallId;
  }

  boolean shouldSendResults() {
    return methodCall.needReturnValues;
  }
}
