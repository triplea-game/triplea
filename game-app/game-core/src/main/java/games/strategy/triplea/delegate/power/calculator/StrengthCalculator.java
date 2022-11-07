package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import java.util.Map;
import org.triplea.java.collections.IntegerMap;

public interface StrengthCalculator {
  StrengthValue getStrength(Unit unit);

  Map<Unit, IntegerMap<Unit>> getSupportGiven();
}
