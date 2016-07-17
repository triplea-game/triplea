package games.strategy.engine.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import games.strategy.net.GUID;

/**
 * The results of a remote Invocation
 */
public abstract class InvocationResults implements Externalizable {
  private static final long serialVersionUID = -382704036681832123L;
  public RemoteMethodCallResults results;
  public GUID methodCallID;

  public InvocationResults() {}

  public InvocationResults(final RemoteMethodCallResults results, final GUID methodCallID) {
    if (results == null) {
      throw new IllegalArgumentException("Null results");
    }
    if (methodCallID == null) {
      throw new IllegalArgumentException("Null id");
    }
    this.results = results;
    this.methodCallID = methodCallID;
  }

  @Override
  public String toString() {
    return "Invocation results for method id:" + methodCallID + " results:" + results;
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    results.writeExternal(out);
    methodCallID.writeExternal(out);
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    results = new RemoteMethodCallResults();
    results.readExternal(in);
    methodCallID = new GUID();
    methodCallID.readExternal(in);
  }
}
