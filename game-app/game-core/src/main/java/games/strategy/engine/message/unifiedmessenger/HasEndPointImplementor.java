package games.strategy.engine.message.unifiedmessenger;

import java.io.Serializable;

/** A message indicating someone now has an implementor for an end point. */
public class HasEndPointImplementor implements Serializable {
  private static final long serialVersionUID = 7607319129099694815L;
  public final String endPointName;

  public HasEndPointImplementor(final String endPointName) {
    this.endPointName = endPointName;
  }

  @Override
  public String toString() {
    return this.getClass().getName() + ": " + endPointName;
  }
}
