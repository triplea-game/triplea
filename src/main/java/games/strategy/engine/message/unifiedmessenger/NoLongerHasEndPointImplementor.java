package games.strategy.engine.message.unifiedmessenger;

import java.io.Serializable;

// someone no longer has implementors for an endpoint
public class NoLongerHasEndPointImplementor implements Serializable {
  private static final long serialVersionUID = -4855990132007435355L;
  public final String endPointName;

  public NoLongerHasEndPointImplementor(final String endPointName) {
    this.endPointName = endPointName;
  }
}
