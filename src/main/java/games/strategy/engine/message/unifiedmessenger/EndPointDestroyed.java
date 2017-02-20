package games.strategy.engine.message.unifiedmessenger;

import java.io.Serializable;

// and end point has been destroyed, we too should jump off that bridge
class EndPointDestroyed implements Serializable {
  private static final long serialVersionUID = 8932889316564814214L;
  public final String name;

  public EndPointDestroyed(final String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "EndPointDestroyed:" + name;
  }
}
