package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

/**
 * A collection of units.
 */
public class UnitCollection extends GameDataComponent implements Iterable<Unit> {
  private static final long serialVersionUID = -3534037864426122864L;
  private final List<Unit> m_units = new ArrayList<>();
  private final NamedUnitHolder m_holder;

  /**
   * Creates new UnitCollection
   *
   * @param holder
   *        named unit holder
   * @param data
   *        game data
   */
  public UnitCollection(final NamedUnitHolder holder, final GameData data) {
    super(data);
    m_holder = holder;
  }

  void addUnit(final Unit unit) {
    m_units.add(unit);
    m_holder.notifyChanged();
  }

  void addAllUnits(final UnitCollection collection) {
    m_units.addAll(collection.m_units);
    m_holder.notifyChanged();
  }

  public void addAllUnits(final Collection<Unit> units) {
    m_units.addAll(units);
    m_holder.notifyChanged();
  }

  public void removeAllUnits(final Collection<Unit> units) {
    m_units.removeAll(units);
    m_holder.notifyChanged();
  }

  public int getUnitCount() {
    return m_units.size();
  }

  public int getUnitCount(final UnitType type) {
    int count = 0;
    final Iterator<Unit> iterator = m_units.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().getType().equals(type)) {
        count++;
      }
    }
    return count;
  }

  public int getUnitCount(final UnitType type, final PlayerID owner) {
    int count = 0;
    final Iterator<Unit> iterator = m_units.iterator();
    while (iterator.hasNext()) {
      final Unit current = iterator.next();
      if (current.getType().equals(type) && current.getOwner().equals(owner)) {
        count++;
      }
    }
    return count;
  }

  public int getUnitCount(final PlayerID owner) {
    int count = 0;
    final Iterator<Unit> iterator = m_units.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().getOwner().equals(owner)) {
        count++;
      }
    }
    return count;
  }

  public boolean containsAll(final Collection<Unit> units) {
    // much faster for large sets
    if (m_units.size() > 500 && units.size() > 500) {
      return new HashSet<>(m_units).containsAll(units);
    }
    return m_units.containsAll(units);
  }

  /**
   * @param type
   *        referring unit type
   * @param max_units
   *        maximal number of units
   * @return up to count units of a given type currently in the collection.
   */
  public Collection<Unit> getUnits(final UnitType type, final int max_units) {
    if (max_units == 0) {
      return new ArrayList<>();
    }
    if (max_units < 0) {
      throw new IllegalArgumentException("value must be positiive.  Instead its:" + max_units);
    }
    final Collection<Unit> rVal = new ArrayList<>();
    for (final Unit current : m_units) {
      if (current.getType().equals(type)) {
        rVal.add(current);
        if (rVal.size() == max_units) {
          return rVal;
        }
      }
    }
    return rVal;
  }

  /**
   * @return integer map of UnitType
   */
  public IntegerMap<UnitType> getUnitsByType() {
    final IntegerMap<UnitType> units = new IntegerMap<>();
    for (final UnitType type : getData().getUnitTypeList()) {
      final int count = getUnitCount(type);
      if (count > 0) {
        units.put(type, count);
      }
    }
    return units;
  }

  /**
   * @param id
   *        referring player ID
   * @return map of UnitType (only of units for the specified player)
   */
  public IntegerMap<UnitType> getUnitsByType(final PlayerID id) {
    final IntegerMap<UnitType> count = new IntegerMap<>();
    for (final Unit unit : m_units) {
      if (unit.getOwner().equals(id)) {
        count.add(unit.getType(), 1);
      }
    }
    return count;
  }

  /**
   * @param types
   *        map of unit types
   * @return collection of units of each type up to max
   */
  public Collection<Unit> getUnits(final IntegerMap<UnitType> types) {
    final Collection<Unit> units = new ArrayList<>();
    for (final UnitType type : types.keySet()) {
      units.addAll(getUnits(type, types.getInt(type)));
    }
    return units;
  }

  public int size() {
    return m_units.size();
  }

  public boolean isEmpty() {
    return m_units.isEmpty();
  }

  public Collection<Unit> getUnits() {
    return new ArrayList<>(m_units);
  }

  /**
   * @return a Set of all players who have units in this collection.
   */
  public Set<PlayerID> getPlayersWithUnits() {
    // note nulls are handled by PlayerID.NULL_PLAYERID
    final Set<PlayerID> ids = new HashSet<>();
    for (final Unit unit : m_units) {
      ids.add(unit.getOwner());
    }
    return ids;
  }

  /**
   * @return the count of units each player has in this collection.
   */
  public IntegerMap<PlayerID> getPlayerUnitCounts() {
    final IntegerMap<PlayerID> count = new IntegerMap<>();
    for (final Unit unit : m_units) {
      count.add(unit.getOwner(), 1);
    }
    return count;
  }

  public boolean hasUnitsFromMultiplePlayers() {
    return getPlayersWithUnits().size() > 1;
  }

  public NamedUnitHolder getHolder() {
    return m_holder;
  }

  public boolean allMatch(final Match<Unit> matcher) {
    for (final Unit unit : m_units) {
      if (!matcher.match(unit)) {
        return false;
      }
    }
    return true;
  }

  public boolean someMatch(final Match<Unit> matcher) {
    for (final Unit unit : m_units) {
      if (matcher.match(unit)) {
        return true;
      }
    }
    return false;
  }

  public int countMatches(final Match<Unit> predicate) {
    return Match.countMatches(m_units, predicate);
  }

  public List<Unit> getMatches(final Match<Unit> predicate) {
    final List<Unit> values = new ArrayList<>();
    for (final Unit unit : m_units) {
      if (predicate.match(unit)) {
        values.add(unit);
      }
    }
    return values;
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder();
    buf.append("Unit collecion held by ").append(m_holder.getName());
    buf.append(" units:");
    final IntegerMap<UnitType> units = getUnitsByType();
    for (final UnitType unit : units.keySet()) {
      buf.append(" <").append(unit.getName()).append(",").append(units.getInt(unit)).append("> ");
    }
    return buf.toString();
  }

  @Override
  public Iterator<Unit> iterator() {
    return Collections.unmodifiableList(m_units).iterator();
  }
}
