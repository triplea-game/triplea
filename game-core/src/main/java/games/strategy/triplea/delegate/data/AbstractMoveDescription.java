package games.strategy.triplea.delegate.data;

import java.io.Serializable;
import java.util.Collection;

import games.strategy.engine.data.Unit;

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
