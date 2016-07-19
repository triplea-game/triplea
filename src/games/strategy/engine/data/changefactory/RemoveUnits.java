package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitHolder;

import java.util.ArrayList;
import java.util.Collection;

class RemoveUnits extends Change {
  private static final long serialVersionUID = -6410444472951010568L;
  private final String m_name;
  private final Collection<Unit> m_units;
  private final String m_type;

  RemoveUnits(final UnitCollection collection, final Collection<Unit> units) {
    this(collection.getHolder().getName(), collection.getHolder().getType(), units);
  }

  RemoveUnits(final String name, final String type, final Collection<Unit> units) {
    m_units = new ArrayList<>(units);
    m_name = name;
    m_type = type;
  }

  @Override
  public Change invert() {
    return new AddUnits(m_name, m_type, m_units);
  }

  @Override
  protected void perform(final GameData data) {
    final UnitHolder holder = data.getUnitHolder(m_name, m_type);
    if (!holder.getUnits().containsAll(m_units)) {
      throw new IllegalStateException("Not all units present in:" + m_name + ".  Trying to remove:" + m_units
          + " present:" + holder.getUnits().getUnits());
    }
    holder.getUnits().removeAllUnits(m_units);
  }

  @Override
  public String toString() {
    return "Remove unit change. Remove from:" + m_name + " units:" + m_units;
  }
}
