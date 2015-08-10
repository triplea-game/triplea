package games.strategy.engine.message;

import games.strategy.net.GUID;

public class HubInvocationResults extends InvocationResults {
  public HubInvocationResults() {
    super();
  }

  public HubInvocationResults(final RemoteMethodCallResults results, final GUID methodCallID) {
    super(results, methodCallID);
  }
}
