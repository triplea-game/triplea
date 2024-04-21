package games.strategy.engine.data;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/** An object that contains a collection of {@link Unit}s. */
public interface UnitHolder {
  String TERRITORY = "T";
  String PLAYER = "P";

  UnitCollection getUnitCollection();

  void notifyChanged();

  String getType();

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
