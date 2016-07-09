package games.strategy.engine.message;

import games.strategy.engine.message.unifiedmessenger.InvocationResults;
import games.strategy.net.GUID;

public class SpokeInvocationResults extends InvocationResults {
  public SpokeInvocationResults() {
    super();
  }

  public SpokeInvocationResults(final RemoteMethodCallResults results, final GUID methodCallID) {
    super(results, methodCallID);
  }
}
