package games.strategy.engine.message;

import games.strategy.net.GUID;
import games.strategy.net.INode;

/**
 * We are waiting for the results of a remote invocation.
 */
class InvocationInProgress {
  private final INode m_waitingOn;
  private final HubInvoke m_methodCall;
  private final INode m_caller;
  private RemoteMethodCallResults m_results;

  InvocationInProgress(final INode waitingOn, final HubInvoke methodCalls, final INode methodCallsFrom) {
    m_waitingOn = waitingOn;
    m_methodCall = methodCalls;
    m_caller = methodCallsFrom;
  }

  boolean isWaitingOn(final INode node) {
    return m_waitingOn.equals(node);
  }

  /**
   * @return true if there are no more results to process.
   */
  boolean process(final HubInvocationResults hubresults, final INode from) {
    if (hubresults.results == null) {
      throw new IllegalStateException("No results");
    }
    m_results = hubresults.results;
    if (!from.equals(m_waitingOn)) {
      throw new IllegalStateException("Wrong node, expecting " + m_waitingOn + " got " + from);
    }
    return true;
  }

  HubInvoke getMethodCall() {
    return m_methodCall;
  }

  INode getCaller() {
    return m_caller;
  }

  RemoteMethodCallResults getResults() {
    return m_results;
  }

  GUID getMethodCallID() {
    return m_methodCall.methodCallID;
  }

  boolean shouldSendResults() {
    return m_methodCall.needReturnValues;
  }
}
