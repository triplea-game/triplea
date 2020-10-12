package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import lombok.Getter;
import org.triplea.java.collections.IntegerMap;

/** Calculates the value of the dice roll and strength. */
abstract class StrengthOrRollCalculator {

  protected final AvailableSupportTracker friendlySupportTracker;
  protected final AvailableSupportTracker enemySupportTracker;
  // Keep track of the units that are providing support and whom they are providing it to
  @Getter private final Map<Unit, IntegerMap<Unit>> supportGiven = new HashMap<>();

  StrengthOrRollCalculator(
      final AvailableSupportTracker friendlySupport, final AvailableSupportTracker enemySupport) {
    this.friendlySupportTracker = friendlySupport.filter(getRuleFilter());
    this.enemySupportTracker = enemySupport.filter(getRuleFilter());
  }

  abstract int getValue(Unit unit);

  protected abstract Predicate<UnitSupportAttachment> getRuleFilter();

  protected int addSupport(final Unit unit, final AvailableSupportTracker supportTracker) {
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
