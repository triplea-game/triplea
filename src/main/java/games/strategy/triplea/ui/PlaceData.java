package games.strategy.triplea.ui;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

public class PlaceData {
  private final Collection<Unit> m_units;
  private final Territory m_at;

  public PlaceData(final Collection<Unit> units, final Territory at) {
    m_units = units;
    m_at = at;
  }

  public Territory getAt() {
    return m_at;
  }

  public Collection<Unit> getUnits() {
    return m_units;
  }
}
