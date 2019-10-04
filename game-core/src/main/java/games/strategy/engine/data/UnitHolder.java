package games.strategy.engine.data;

import java.util.Collection;

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
}
