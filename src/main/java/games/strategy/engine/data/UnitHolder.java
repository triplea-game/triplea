package games.strategy.engine.data;

public interface UnitHolder {
  String TERRITORY = "T";
  String PLAYER = "P";

  UnitCollection getUnits();

  void notifyChanged();

  String getType();
}
