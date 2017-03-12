package games.strategy.engine.message.unifiedmessenger;

import java.io.Serializable;

// an end point has been created, we should follow
class EndPointCreated implements Serializable {
  private static final long serialVersionUID = -5780669206340723091L;
  public final String[] classes;
  public final String name;
  public final boolean singleThreaded;

  public EndPointCreated(final String[] classes, final String name, final boolean singleThreaded) {
    this.classes = classes;
    this.name = name;
    this.singleThreaded = singleThreaded;
  }
}
