package games.strategy.engine.message;

import games.strategy.engine.message.unifiedmessenger.Invoke;
import games.strategy.net.INode;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;
import lombok.Getter;

/**
 * A request forwarded by the hub node to invoke a remote method on a spoke node. Instances of this
 * class should only be created by the messaging framework on the hub node. All remote method
 * invocations should originate as an instance of {@link HubInvoke}.
 */
@Getter
public class SpokeInvoke extends Invoke {
  private static final long serialVersionUID = -2007645463748969L;
  private INode invoker;

  public SpokeInvoke() {}

  public SpokeInvoke(
      final UUID methodCallId,
      final boolean needReturnValues,
      final RemoteMethodCall call,
      final INode invoker) {
    super(methodCallId, needReturnValues, call);
    this.invoker = invoker;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    invoker = (INode) in.readObject();
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(invoker);
  }
}
