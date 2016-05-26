package games.strategy.engine.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import games.strategy.net.GUID;

public class UnitsList implements java.io.Serializable, Iterable<Unit> {
  private static final long serialVersionUID = -3134052492257867416L;
  // maps GUID -> Unit
  // TODO - fix this, all units are never gcd
  // note, weak hash maps are not serializable
  private Map<GUID, Unit> m_allUnits;

  Unit get(final GUID id) {
    return m_allUnits.get(id);
  }

  public void put(final Unit unit) {
    m_allUnits.put(unit.getID(), unit);
  }

  /*
   * Gets all units currently in the game
   */
  public Collection<Unit> getUnits() {
    return Collections.unmodifiableCollection(m_allUnits.values());
  }

  public void refresh() {
    m_allUnits = new HashMap<>();
  }

  UnitsList() {
    refresh();
  }

  @Override
  public Iterator<Unit> iterator() {
    return getUnits().iterator();
  }
}
