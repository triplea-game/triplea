package games.strategy.engine.message.unifiedmessenger;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.message.RemoteMethodCallResults;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

/** The results of a remote invocation. */
public abstract class InvocationResults implements Externalizable {
  private static final long serialVersionUID = -382704036681832123L;
  public RemoteMethodCallResults results;
  public UUID methodCallId;

  public InvocationResults() {}

  public InvocationResults(final RemoteMethodCallResults results, final UUID methodCallId) {
    checkNotNull(results);
    checkNotNull(methodCallId);

    this.results = results;
    this.methodCallId = methodCallId;
  }

  @Override
  public String toString() {
    return "Invocation results for method id: " + methodCallId + " results: " + results;
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    results.writeExternal(out);
    out.writeObject(methodCallId.toString());
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    results = new RemoteMethodCallResults();
    results.readExternal(in);
    methodCallId = UUID.fromString((String) in.readObject());
  }
}
