package games.strategy.engine.data.changefactory;

import java.util.ArrayList;
import java.util.Collection;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitHolder;

/**
 * Add units
 */
class AddUnits extends Change {
  private static final long serialVersionUID = 2694342784633196289L;
  private final String m_name;
  private final Collection<Unit> m_units;
  private final String m_type;

  AddUnits(final UnitCollection collection, final Collection<Unit> units) {
    m_units = new ArrayList<>(units);
    m_name = collection.getHolder().getName();
    m_type = collection.getHolder().getType();
  }

  AddUnits(final String name, final String type, final Collection<Unit> units) {
    m_units = new ArrayList<>(units);
    m_type = type;
    m_name = name;
  }

  @Override
  public Change invert() {
    return new RemoveUnits(m_name, m_type, m_units);
  }

  @Override
  protected void perform(final GameData data) {
    final UnitHolder holder = data.getUnitHolder(m_name, m_type);
    holder.getUnits().addAllUnits(m_units);
  }

  @Override
  public String toString() {
    return "Add unit change.  Add to:" + m_name + " units:" + m_units;
  }
}
