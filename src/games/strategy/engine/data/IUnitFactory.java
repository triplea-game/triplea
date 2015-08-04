package games.strategy.engine.data;

import java.io.Serializable;

public interface IUnitFactory extends Serializable {
  Unit createUnit(UnitType type, PlayerID owner, GameData data);
}
