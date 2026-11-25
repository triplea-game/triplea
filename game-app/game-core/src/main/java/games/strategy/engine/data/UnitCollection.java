package games.strategy.engine.data;

import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** A collection of units. */
public class UnitCollection extends GameDataComponent implements Collection<Unit> {
  private static final long serialVersionUID = -3534037864426122864L;

  private final List<Unit> units = new ArrayList<>();
  @Getter private final NamedUnitHolder holder;

  public UnitCollection(final NamedUnitHolder holder, final GameData data) {
    super(data);
    this.holder = holder;
  }

  @Override
  public boolean add(final Unit unit) {
    units.add(unit);
    holder.notifyChanged();
    return true;
  }

  @Override
  public boolean addAll(final Collection<? extends Unit> units) {
    final boolean result = this.units.addAll(units);
    holder.notifyChanged();
    return result;
  }

  @Override
  public boolean removeAll(final Collection<?> units) {
    final boolean result = this.units.removeAll(units);
    holder.notifyChanged();
    return result;
  }

  public int getUnitCount() {
    return units.size();
  }

  int getUnitCount(final UnitType type) {
    return (int) units.stream().filter(u -> u.getType().equals(type)).count();
  }

  public int getUnitCount(final UnitType type, final GamePlayer owner) {
    return (int) units.stream().filter(u -> u.getType().equals(type) && u.isOwnedBy(owner)).count();
  }

  int getUnitCount(final GamePlayer owner) {
    return (int) units.stream().filter(u -> u.isOwnedBy(owner)).count();
  }

  @Override
  public boolean containsAll(final Collection<?> units) {
    // much faster for large sets
    if (this.units.size() > 500 && units.size() > 500) {
      return new HashSet<>(this.units).containsAll(units);
    }
    return this.units.containsAll(units);
  }

  /**
   * Returns up to count units of a given type currently in the collection.
   *
   * @param type referring unit type
   * @param maxUnits maximal number of units
   */
  public Collection<Unit> getUnits(final UnitType type, final int maxUnits) {
    if (maxUnits == 0) {
      return new ArrayList<>();
    }
    if (maxUnits < 0) {
      throw new IllegalArgumentException("value must be positive.  Instead its: " + maxUnits);
    }
    final Collection<Unit> units = new ArrayList<>();
    for (final Unit current : this.units) {
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
   * Returns collection of units of each type up to max.
   *
   * @param types map of unit types
   */
  public Collection<Unit> getUnits(final IntegerMap<UnitType> types) {
    final Collection<Unit> units = new ArrayList<>();
    for (final UnitType type : types.keySet()) {
      units.addAll(getUnits(type, types.getInt(type)));
    }
    return units;
  }

  public Collection<Unit> getUnits() {
    return Collections.unmodifiableList(units);
  }

  /** Returns integer map of UnitType. */
  public IntegerMap<UnitType> getUnitsByType() {
    final IntegerMap<UnitType> units = new IntegerMap<>();
    getData()
        .getUnitTypeList()
        .forEach(
            type -> {
              final int count = getUnitCount(type);
              if (count > 0) {
                units.put(type, count);
              }
            });
    return units;
  }

  /** Returns map of UnitType (only of units for the specified player). */
  public IntegerMap<UnitType> getUnitsByType(final GamePlayer gamePlayer) {
    final IntegerMap<UnitType> count = new IntegerMap<>();
    units.stream()
        .filter(Matches.unitIsOwnedBy(gamePlayer))
        .forEach(unit -> count.add(unit.getType(), 1));
    return count;
  }

  @Override
  public int size() {
    return units.size();
  }

  @Override
  public boolean isEmpty() {
    return units.isEmpty();
  }

  /** Returns a Set of all players who have units in this collection. */
  public Set<GamePlayer> getPlayersWithUnits() {
    // note nulls are handled by PlayerList.getNullPlayer()
    return units.stream().map(Unit::getOwner).collect(Collectors.toSet());
  }

  /** Returns the count of units each player has in this collection. */
  public IntegerMap<GamePlayer> getPlayerUnitCounts() {
    final IntegerMap<GamePlayer> count = new IntegerMap<>();
    units.forEach(unit -> count.add(unit.getOwner(), 1));
    return count;
  }

  public List<GamePlayer> getPlayersSortedByUnitCount() {
    final IntegerMap<GamePlayer> map = getPlayerUnitCounts();
    final List<GamePlayer> players = new ArrayList<>(map.keySet());
    players.sort(Comparator.comparingInt(map::getInt).reversed());
    return players;
  }

  public boolean hasUnitsFromMultiplePlayers() {
    return getPlayersWithUnits().size() > 1;
  }

  public boolean allMatch(final Predicate<Unit> matcher) {
    return units.stream().allMatch(matcher);
  }

  public boolean anyMatch(final Predicate<Unit> matcher) {
    return units.stream().anyMatch(matcher);
  }

  public int countMatches(final Predicate<Unit> predicate) {
    return CollectionUtils.countMatches(units, predicate);
  }

  public List<Unit> getMatches(final Predicate<Unit> predicate) {
    return CollectionUtils.getMatches(units, predicate);
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder();
    buf.append("Unit collection held by ").append(holder.getName());
    buf.append(" units:");
    final IntegerMap<UnitType> units = getUnitsByType();
    for (final UnitType unit : units.keySet()) {
      buf.append(" <").append(unit.getName()).append(",").append(units.getInt(unit)).append("> ");
    }
    return buf.toString();
  }

  @Override
  public Iterator<Unit> iterator() {
    return Collections.unmodifiableList(units).iterator();
  }

  @Override
  public boolean contains(final Object object) {
    return units.contains(object);
  }

  @Override
  public Object[] toArray() {
    return units.toArray();
  }

  @Override
  public <T> T[] toArray(final T[] array) {
    return units.toArray(array);
  }

  @Override
  public boolean remove(final Object object) {
    final boolean changed = units.remove(object);
    if (changed) {
      holder.notifyChanged();
    }
    return changed;
  }

  @Override
  public boolean removeIf(final Predicate<? super Unit> predicate) {
    final boolean changed = units.removeIf(predicate);
    if (changed) {
      holder.notifyChanged();
    }
    return changed;
  }

  @Override
  public boolean retainAll(final Collection<?> collection) {
    final boolean changed = units.retainAll(collection);
    if (changed) {
      holder.notifyChanged();
    }
    return changed;
  }

  @Override
  public void clear() {
    if (!units.isEmpty()) {
      units.clear();
      holder.notifyChanged();
    }
  }
}
