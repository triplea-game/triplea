package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

/**
 * A collection of units.
 */
public class UnitCollection extends GameDataComponent implements Collection<Unit> {
  private static final long serialVersionUID = -3534037864426122864L;
  private final List<Unit> m_units = new ArrayList<>();
  private final NamedUnitHolder m_holder;

  /**
   * Creates new UnitCollection.
   *
   * @param holder named unit holder
   * @param data game data
   */
  public UnitCollection(final NamedUnitHolder holder, final GameData data) {
    super(data);
    m_holder = holder;
  }

  @Override
  public boolean add(final Unit unit) {
    final boolean result = m_units.add(unit);
    m_holder.notifyChanged();
    return result;
  }

  @Override
  public boolean addAll(final Collection<? extends Unit> units) {
    final boolean result = m_units.addAll(units);
    m_holder.notifyChanged();
    return result;
  }

  @Override
  public boolean removeAll(final Collection<?> units) {
    final boolean result = m_units.removeAll(units);
    m_holder.notifyChanged();
    return result;
  }

  public int getUnitCount() {
    return m_units.size();
  }

  int getUnitCount(final UnitType type) {
    return (int) m_units.stream().filter(u -> u.getType().equals(type)).count();
  }

  public int getUnitCount(final UnitType type, final PlayerID owner) {
    return (int) m_units.stream().filter(u -> u.getType().equals(type) && u.getOwner().equals(owner)).count();
  }

  int getUnitCount(final PlayerID owner) {
    return (int) m_units.stream().filter(u -> u.getOwner().equals(owner)).count();
  }

  @Override
  public boolean containsAll(final Collection<?> units) {
    // much faster for large sets
    if ((m_units.size() > 500) && (units.size() > 500)) {
      return new HashSet<>(m_units).containsAll(units);
    }
    return m_units.containsAll(units);
  }

  /**
   * @param type
   *        referring unit type
   * @param maxUnits
   *        maximal number of units
   * @return up to count units of a given type currently in the collection.
   */
  public Collection<Unit> getUnits(final UnitType type, final int maxUnits) {
    if (maxUnits == 0) {
      return new ArrayList<>();
    }
    if (maxUnits < 0) {
      throw new IllegalArgumentException("value must be positiive.  Instead its:" + maxUnits);
    }
    final Collection<Unit> units = new ArrayList<>();
    for (final Unit current : m_units) {
      if (current.getType().equals(type)) {
        units.add(current);
        if (units.size() == maxUnits) {
          return units;
        }
      }
    }
    return units;
  }

  /**
   * @param types
   *        map of unit types
   * @return collection of units of each type up to max.
   */
  public Collection<Unit> getUnits(final IntegerMap<UnitType> types) {
    final Collection<Unit> units = new ArrayList<>();
    for (final UnitType type : types.keySet()) {
      units.addAll(getUnits(type, types.getInt(type)));
    }
    return units;
  }

  public Collection<Unit> getUnits() {
    return new ArrayList<>(m_units);
  }

  /**
   * @return integer map of UnitType.
   */
  public IntegerMap<UnitType> getUnitsByType() {
    final IntegerMap<UnitType> units = new IntegerMap<>();
    getData().getUnitTypeList().forEach(type -> {
      final int count = getUnitCount(type);
      if (count > 0) {
        units.put(type, count);
      }
    });
    return units;
  }

  /**
   * @param id
   *        referring player ID
   * @return map of UnitType (only of units for the specified player).
   */
  public IntegerMap<UnitType> getUnitsByType(final PlayerID id) {
    final IntegerMap<UnitType> count = new IntegerMap<>();
    m_units.stream().filter(unit -> unit.getOwner().equals(id)).forEach(unit -> count.add(unit.getType(), 1));
    return count;
  }

  @Override
  public int size() {
    return m_units.size();
  }

  @Override
  public boolean isEmpty() {
    return m_units.isEmpty();
  }

  /**
   * @return a Set of all players who have units in this collection.
   */
  public Set<PlayerID> getPlayersWithUnits() {
    // note nulls are handled by PlayerID.NULL_PLAYERID
    return m_units.stream().map(Unit::getOwner).collect(Collectors.toSet());
  }

  /**
   * @return The count of units each player has in this collection.
   */
  public IntegerMap<PlayerID> getPlayerUnitCounts() {
    final IntegerMap<PlayerID> count = new IntegerMap<>();
    m_units.stream().forEach(unit -> count.add(unit.getOwner(), 1));
    return count;
  }

  public boolean hasUnitsFromMultiplePlayers() {
    return getPlayersWithUnits().size() > 1;
  }

  public NamedUnitHolder getHolder() {
    return m_holder;
  }

  public boolean allMatch(final Predicate<Unit> matcher) {
    return m_units.stream().allMatch(matcher);
  }

  public boolean anyMatch(final Predicate<Unit> matcher) {
    return m_units.stream().anyMatch(matcher);
  }

  public int countMatches(final Predicate<Unit> predicate) {
    return CollectionUtils.countMatches(m_units, predicate);
  }

  public List<Unit> getMatches(final Predicate<Unit> predicate) {
    return CollectionUtils.getMatches(m_units, predicate);
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

  @Override
  public boolean contains(final Object object) {
    return m_units.contains(object);
  }

  @Override
  public Object[] toArray() {
    return m_units.toArray();
  }

  @Override
  public <T> T[] toArray(final T[] array) {
    return m_units.toArray(array);
  }

  @Override
  public boolean remove(final Object object) {
    final boolean result = m_units.remove(object);
    m_holder.notifyChanged();
    return result;
  }

  @Override
  public boolean retainAll(final Collection<?> collection) {
    return m_units.retainAll(collection);
  }

  @Override
  public void clear() {
    m_units.clear();
    m_holder.notifyChanged();
  }
}
