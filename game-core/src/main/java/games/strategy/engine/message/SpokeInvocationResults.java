package games.strategy.engine.message;

import games.strategy.engine.message.unifiedmessenger.InvocationResults;
import games.strategy.net.GUID;

/** The results of a remote method call invoked via {@link SpokeInvoke}. */
public class SpokeInvocationResults extends InvocationResults {
  private static final long serialVersionUID = 8998965687635348969L;

  public SpokeInvocationResults() {}

  public SpokeInvocationResults(final RemoteMethodCallResults results, final GUID methodCallId) {
    super(results, methodCallId);
  }
}
