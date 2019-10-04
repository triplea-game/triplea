package games.strategy.engine.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** A keyed collection of {@link Unit}s. */
public class UnitsList implements Serializable, Iterable<Unit> {
  private static final long serialVersionUID = -3134052492257867416L;

  // TODO - fix this, all units are never gcd
  private final Map<UUID, Unit> allUnits = new HashMap<>();

  UnitsList() {}

  public Unit get(final UUID id) {
    return allUnits.get(id);
  }

  public void put(final Unit unit) {
    allUnits.put(unit.getId(), unit);
  }

  /** Gets all units currently in the game. */
  public Collection<Unit> getUnits() {
    return Collections.unmodifiableCollection(allUnits.values());
  }

  @Override
  public Iterator<Unit> iterator() {
    return getUnits().iterator();
  }
}
