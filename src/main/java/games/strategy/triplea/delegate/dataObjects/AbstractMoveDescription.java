package games.strategy.triplea.delegate.dataObjects;

import java.util.Collection;

import games.strategy.engine.data.Unit;

public abstract class AbstractMoveDescription implements java.io.Serializable {
  private static final long serialVersionUID = -6615899716448836002L;
  private final Collection<Unit> m_units;

  public AbstractMoveDescription(final Collection<Unit> units) {
    m_units = units;
  }

  public Collection<Unit> getUnits() {
    return m_units;
  }

  @Override
  public abstract String toString();
}
