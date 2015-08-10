package games.strategy.engine.data;


public interface UnitHolder {
  public static final String TERRITORY = "T";
  public static final String PLAYER = "P";

  public UnitCollection getUnits();

  public void notifyChanged();

  public String getType();
}
