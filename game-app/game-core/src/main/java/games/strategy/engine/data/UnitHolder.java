package games.strategy.engine.data;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/** An object that contains a collection of {@link Unit}s. */
public interface UnitHolder {
  @Deprecated String TERRITORY = UnitHolderType.TERRITORY.id();
  @Deprecated String PLAYER = UnitHolderType.PLAYER.id();

  UnitCollection getUnitCollection();

  void notifyChanged();

  UnitHolderType getType();

  default Collection<Unit> getUnits() {
    return getUnitCollection().getUnits();
  }

  default boolean anyUnitsMatch(final Predicate<Unit> matcher) {
    return getUnitCollection().anyMatch(matcher);
  }

  default List<Unit> getMatches(final Predicate<Unit> matcher) {
    return getUnitCollection().getMatches(matcher);
  }
}
