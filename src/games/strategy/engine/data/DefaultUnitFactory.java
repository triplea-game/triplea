package games.strategy.engine.data;

public class DefaultUnitFactory implements IUnitFactory {
  private static final long serialVersionUID = 201371033476236028L;

  @Override
  public Unit createUnit(final UnitType type, final PlayerID owner, final GameData data) {
    return new Unit(type, owner, data);
  }
}
