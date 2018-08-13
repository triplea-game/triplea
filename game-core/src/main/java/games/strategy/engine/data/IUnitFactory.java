package games.strategy.engine.data;

/**
 * Factory for creating instances of {@link Unit}.
 */
public interface IUnitFactory {
  Unit createUnit(UnitType type, PlayerID owner, GameData data);
}
