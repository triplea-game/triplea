package games.strategy.engine.message.unifiedmessenger;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import games.strategy.engine.message.RemoteMethodCallResults;
import games.strategy.net.GUID;

/**
 * The results of a remote invocation.
 */
public abstract class InvocationResults implements Externalizable {
  private static final long serialVersionUID = -382704036681832123L;
  public RemoteMethodCallResults results;
  public GUID methodCallId;

  public InvocationResults() {}

  public InvocationResults(final RemoteMethodCallResults results, final GUID methodCallId) {
    if (results == null) {
      throw new IllegalArgumentException("Null results");
    }
    if (methodCallId == null) {
      throw new IllegalArgumentException("Null id");
    }
    this.results = results;
    this.methodCallId = methodCallId;
  }

  @Override
  public String toString() {
    return "Invocation results for method id:" + methodCallId + " results:" + results;
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    results.writeExternal(out);
    methodCallId.writeExternal(out);
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    results = new RemoteMethodCallResults();
    results.readExternal(in);
    methodCallId = new GUID();
    methodCallId.readExternal(in);
  }
}
