package games.strategy.engine.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import games.strategy.net.GUID;

/**
 * A keyed collection of {@link Unit}s.
 */
public class UnitsList implements Serializable, Iterable<Unit> {
  private static final long serialVersionUID = -3134052492257867416L;

  // TODO - fix this, all units are never gcd
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final Map<GUID, Unit> m_allUnits = new HashMap<>();

  UnitsList() {}

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

  @Override
  public Iterator<Unit> iterator() {
    return getUnits().iterator();
  }
}
