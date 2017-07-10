package games.strategy.engine.message;

import games.strategy.engine.message.unifiedmessenger.Invoke;
import games.strategy.net.GUID;

public class HubInvoke extends Invoke {
  private static final long serialVersionUID = -176987635348969L;

  public HubInvoke() {
    super();
  }

  public HubInvoke(final GUID methodCallId, final boolean needReturnValues, final RemoteMethodCall call) {
    super(methodCallId, needReturnValues, call);
  }
}
