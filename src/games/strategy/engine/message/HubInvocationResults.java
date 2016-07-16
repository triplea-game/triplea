package games.strategy.engine.message;

import games.strategy.net.GUID;

public class HubInvocationResults extends InvocationResults {
  private static final long serialVersionUID = -1769876896858969L;

  public HubInvocationResults() {
    super();
  }

  public HubInvocationResults(final RemoteMethodCallResults results, final GUID methodCallID) {
    super(results, methodCallID);
  }
}
