package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import lombok.Getter;
import org.triplea.java.collections.IntegerMap;

/** Calculates the value of the dice roll and strength. */
public abstract class StrengthOrRollCalculator {

  // AvailableSupportTracker changes internal state as support is used, so need to have
  // a unique copy for the strength and roll supports
  protected final AvailableSupportCalculator friendlySupportTracker;
  protected final AvailableSupportCalculator enemySupportTracker;
  // Keep track of the units that are providing support and whom they are providing it to
  @Getter private final Map<Unit, IntegerMap<Unit>> supportGiven = new HashMap<>();

  StrengthOrRollCalculator(
      final AvailableSupportCalculator friendlySupport,
      final AvailableSupportCalculator enemySupport) {
    this.friendlySupportTracker = new AvailableSupportCalculator(friendlySupport);
    this.enemySupportTracker = new AvailableSupportCalculator(enemySupport);
  }

  abstract int getValue(Unit unit);

  protected int addSupport(
      final Unit unit,
      final AvailableSupportCalculator supportTracker,
      final Predicate<UnitSupportAttachment> ruleFilter) {
    final IntegerMap<Unit> supportGivenToUnit = supportTracker.giveSupportToUnit(unit, ruleFilter);

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
