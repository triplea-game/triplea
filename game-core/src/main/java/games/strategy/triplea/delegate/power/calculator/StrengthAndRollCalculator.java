package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/** Calculates the value of the dice roll and strength. */
@Value
class StrengthAndRollCalculator {
  // Keep track of the units that are providing support and whom they are providing it to
  @Getter Map<Unit, IntegerMap<Unit>> supportGiven = new HashMap<>();

  int addSupport(final Unit unit, final AvailableSupports supportTracker) {
    final IntegerMap<Unit> supportGivenToUnit = supportTracker.giveSupportToUnit(unit);

    supportGivenToUnit
        .keySet()
        .forEach(
            (supporter) -> {
              supportGiven
                  .computeIfAbsent(supporter, (newSupport) -> new IntegerMap<>())
                  .add(unit, supportGivenToUnit.getInt(supporter));
            });

    return supportGivenToUnit.totalValues();
  }
}
