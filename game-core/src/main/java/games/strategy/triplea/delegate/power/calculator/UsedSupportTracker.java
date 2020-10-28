package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/** Gives and records the support given to units */
@Value
class UsedSupportTracker {
  // Keep track of the units that are providing support and whom they are providing it to
  @Getter Map<Unit, IntegerMap<Unit>> supportGiven = new HashMap<>();

  /**
   * Gives support to the unit and removes it from the availableSupport
   *
   * <p>Each time this is called, the amount of available support will decrease equal to the amount
   * returned.
   *
   * @param unit Unit to have support given to it
   * @param availableSupport Keeps state of what support is still available
   * @return the amount of support given
   */
  int giveSupport(final Unit unit, final AvailableSupports availableSupport) {
    final IntegerMap<Unit> supportGivenToUnit = availableSupport.giveSupportToUnit(unit);

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
