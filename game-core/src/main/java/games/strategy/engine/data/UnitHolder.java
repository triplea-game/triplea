package games.strategy.engine.data;

/** An object that contains a collection of {@link Unit}s. */
public interface UnitHolder {
  String TERRITORY = "T";
  String PLAYER = "P";

  UnitCollection getUnits();

  void notifyChanged();

  String getType();
}
