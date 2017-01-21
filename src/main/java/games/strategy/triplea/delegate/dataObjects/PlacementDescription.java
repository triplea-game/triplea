package games.strategy.triplea.delegate.dataObjects;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

public class PlacementDescription extends AbstractMoveDescription {
  private static final long serialVersionUID = -3141153168992624631L;
  private final Territory m_territory;

  public PlacementDescription(final Collection<Unit> units, final Territory territory) {
    super(units);
    m_territory = territory;
  }

  public Territory getTerritory() {
    return m_territory;
  }

  @Override
  public String toString() {
    return "Placement message territory:" + m_territory + " units:" + getUnits();
  }
}
