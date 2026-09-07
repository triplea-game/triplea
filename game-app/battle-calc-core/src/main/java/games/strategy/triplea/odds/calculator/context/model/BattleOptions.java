package games.strategy.triplea.odds.calculator.context.model;

import java.util.List;

/**
 * Caller-supplied knobs for one calc run: the retreat rule and each side's order-of-loss
 * preference, which the adapter bakes into {@link BattleScenario}'s casualty orders.
 *
 * <p>{@code retreatAfterRound} and {@code retreatAfterXUnitsLeft} are round-end withdrawal
 * thresholds ({@code -1} disables each); {@code amphibious} marks the attack as amphibious without
 * the caller having to flag its units; {@code keepOneAttackingLandUnit} is carried for interface
 * parity but not yet honoured downstream (the simulator hardcodes it off).
 */
public record BattleOptions(
    boolean retreatWhenOnlyAirLeft,
    List<UnitTypeId> attackerOol,
    List<UnitTypeId> defenderOol,
    int retreatAfterRound,
    int retreatAfterXUnitsLeft,
    boolean amphibious,
    boolean keepOneAttackingLandUnit) {

  /** Defaults the retreat thresholds to disabled, and amphibious and keep-one-land to off. */
  public BattleOptions(
      final boolean retreatWhenOnlyAirLeft,
      final List<UnitTypeId> attackerOol,
      final List<UnitTypeId> defenderOol) {
    this(retreatWhenOnlyAirLeft, attackerOol, defenderOol, -1, -1, false, false);
  }
}
