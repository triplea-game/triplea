package games.strategy.engine.message;

import games.strategy.engine.message.unifiedmessenger.Invoke;
import java.util.UUID;

/**
 * A request to invoke a remote method on the hub node. All remote method invocations originate as
 * an instance of this class.
 */
public class HubInvoke extends Invoke {
  private static final long serialVersionUID = -176987635348969L;

  public HubInvoke() {}

  public HubInvoke(
      final UUID methodCallId, final boolean needReturnValues, final RemoteMethodCall call) {
    super(methodCallId, needReturnValues, call);
  }
}
