package games.strategy.engine.data;

/**
 * Factory for creating instances of {@link Unit}.
 */
public interface UnitFactory {
  Unit createUnit(UnitType type, PlayerID owner, GameData data);
}
