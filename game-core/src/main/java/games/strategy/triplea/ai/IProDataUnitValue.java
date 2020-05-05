package games.strategy.triplea.ai;

import games.strategy.engine.data.UnitType;
import org.triplea.java.collections.IntegerMap;

public interface IProDataUnitValue {
  int getUnitValue(UnitType type);
  IntegerMap<UnitType> getUnitValueMap();
}
