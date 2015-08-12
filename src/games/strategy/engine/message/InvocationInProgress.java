package games.strategy.engine.message;

import games.strategy.net.GUID;
import games.strategy.net.INode;

/**
 * We are waiting for the results of a remote invocation.
 */
public class InvocationInProgress {
  private final INode m_waitingOn;
  private final HubInvoke m_methodCall;
  private final INode m_caller;
  private RemoteMethodCallResults m_results;

  public InvocationInProgress(final INode waitingOn, final HubInvoke methodCalls, final INode methodCallsFrom) {
    m_waitingOn = waitingOn;
    m_methodCall = methodCalls;
    m_caller = methodCallsFrom;
  }

  public boolean isWaitingOn(final INode node) {
    return m_waitingOn.equals(node);
  }

  /**
   * @return true if there are no more results to process
   */
  public boolean process(final HubInvocationResults hubresults, final INode from) {
    if (hubresults.results == null) {
      throw new IllegalStateException("No results");
    }
    m_results = hubresults.results;
    if (!from.equals(m_waitingOn)) {
      throw new IllegalStateException("Wrong node, expecting " + m_waitingOn + " got " + from);
    }
    return true;
  }

  public HubInvoke getMethodCall() {
    return m_methodCall;
  }

  public INode getCaller() {
    return m_caller;
  }

  public RemoteMethodCallResults getResults() {
    return m_results;
  }

  public GUID getMethodCallID() {
    return m_methodCall.methodCallID;
  }

  public boolean shouldSendResults() {
    return m_methodCall.needReturnValues;
  }
}
