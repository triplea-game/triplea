package games.strategy.engine.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import games.strategy.net.GUID;

public class UnitsList implements Serializable, Iterable<Unit>, Cloneable {
  private static final long serialVersionUID = -3134052492257867416L;
  // maps GUID -> Unit
  // TODO - fix this, all units are never gcd
  // note, weak hash maps are not serializable
  private final Map<GUID, Unit> m_allUnits;

  public Unit get(final GUID id) {
    return m_allUnits.get(id);
  }

  public void put(final Unit unit) {
    m_allUnits.put(unit.getId(), unit);
  }

  /*
   * Gets all units currently in the game
   */
  public Collection<Unit> getUnits() {
    return Collections.unmodifiableCollection(m_allUnits.values());
  }

  UnitsList() {
    this(new HashMap<>());
  }

  UnitsList(final Map<GUID, Unit> allUnits) {
    m_allUnits = allUnits;
  }

  @Override
  public Iterator<Unit> iterator() {
    return getUnits().iterator();
  }

  @Override
  public UnitsList clone() {
    return new UnitsList(new HashMap<>(m_allUnits));
  }
}
