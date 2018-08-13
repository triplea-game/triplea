package games.strategy.engine.data;

import java.io.Serializable;

/**
 * Factory for creating instances of {@link Unit}.
 */
public interface IUnitFactory extends Serializable {
  Unit createUnit(UnitType type, PlayerID owner, GameData data);
}
