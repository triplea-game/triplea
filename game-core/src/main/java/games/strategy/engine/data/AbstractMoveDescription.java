package games.strategy.engine.data;

import java.io.Serializable;
import java.util.Collection;

/** Superclass for any action that describes the movement or placement of units. */
public abstract class AbstractMoveDescription implements Serializable {
  private static final long serialVersionUID = -6615899716448836002L;
  private final Collection<Unit> units;

  public AbstractMoveDescription(final Collection<Unit> units) {
    this.units = units;
  }

  public Collection<Unit> getUnits() {
    return units;
  }

  @Override
  public abstract String toString();
}
